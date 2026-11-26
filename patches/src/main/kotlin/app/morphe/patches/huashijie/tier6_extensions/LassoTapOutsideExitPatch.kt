package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex

private const val TOGGLE_KEY = "libre_touch_outside_dismiss"
private const val HANDLE_TOUCH_OUTSIDE =
    "Lapp/morphe/extension/huashijie/UiUxController;->handlePaintTouchOutside(Landroid/app/Activity;Landroid/view/MotionEvent;)V"

private val paintDispatchTouchFingerprint = Fingerprint(
    name = "dispatchTouchEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/PaintActivity;"
    },
)

@Suppress("unused")
val lassoTapOutsideExitPatch = bytecodePatch(
    name = "Lasso Tap-Outside Exit",
    description = "Exits active Lasso selection with a single tap outside the selection while preserving Magic Wand, Polygonal Lasso, Move/Transform, and active guide bars.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        registerResolvedTarget(
            "PaintActivity.outCloseViewField",
            MultiVersionResolvers.resolvePaintOutCloseViewField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.xuanQuViewField",
            MultiVersionResolvers.resolvePaintXuanQuViewField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.xuanQuMoveViewField",
            MultiVersionResolvers.resolvePaintXuanQuMoveViewField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.leftSettingViewField",
            MultiVersionResolvers.resolvePaintLeftSettingViewField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.canvasViewField",
            MultiVersionResolvers.resolvePaintCanvasViewField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.userIdField",
            MultiVersionResolvers.resolvePaintUserIdField(paintClass).name,
        )
        registerResolvedTarget(
            "PaintActivity.switchLeftTool",
            MultiVersionResolvers.resolvePaintSwitchLeftToolMethod(paintClass).name,
        )
        registerResolvedTarget(
            "PAINT_PARAMS.layerStateSparseField",
            MultiVersionResolvers.resolvePaintParamsLayerStateSparseField(classes).name,
        )
        registerResolvedTarget(
            "RenderAssist.selectionRectField",
            MultiVersionResolvers.resolveLayerStateSelectionRectField(classes).name,
        )

        val method = paintDispatchTouchFingerprint.method
        val thisReg = method.thisRegisterIndex()
        val evReg = thisReg + 1
        val invokeSmali = if (thisReg in 0..15 && evReg in 0..15) {
            "invoke-static {v$thisReg, v$evReg}, $HANDLE_TOUCH_OUTSIDE"
        } else {
            "invoke-static/range {v$thisReg .. v$evReg}, $HANDLE_TOUCH_OUTSIDE"
        }
        method.addInstructionsPreservingLabels(0, invokeSmali)
    }
}
