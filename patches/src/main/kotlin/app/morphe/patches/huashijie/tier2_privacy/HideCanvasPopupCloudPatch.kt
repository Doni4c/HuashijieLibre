package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.LIBRE_TRANSLATION_DISPATCH
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.hookPaintBubblePopupBeforeShow
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val TOGGLE_KEY = "pure_studio_hide_canvas_popup_cloud"
private const val OP_IS_HIDE_CANVAS_POPUP_CLOUD = 18
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

@Suppress("unused")
val hideCanvasPopupCloudPatch = bytecodePatch(
    name = "Hide Canvas Popup Cloud Sync",
    description = "Hides cloud backup controls in the canvas settings popup and the cloud tab in image pickers.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val bubbleType = MultiVersionResolvers.resolveBubblePopupClass(classes).type
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)

        for (method in listOf(
            MultiVersionResolvers.resolvePaintPopup1Menu(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup3Canvas(paintClass, bubbleType),
        )) {
            hookPaintBubblePopupBeforeShow(
                toFingerprint(method),
                "${WORKSPACE_UI_CONTROLLER}->optimizePaintCloudViews(Landroid/view/View;)V",
            )
        }

        // Hide the Cloud Album tab (CloudImageFragment) inside LocalImageActivity when enabled.
        val localImageClass = MultiVersionResolvers.resolveLocalImageActivityClass(classes)
        val showCloudMethod = MultiVersionResolvers.resolveLocalImageShowCloudMethod(classes, localImageClass)
        registerResolvedTarget(
            "LocalImageActivity.showCloudMethod",
            "${localImageClass.type}->${showCloudMethod.name}",
        )
        val mutableShowCloud = toMutableMethod(showCloudMethod)
        val firstInst = mutableShowCloud.getInstruction(0) as? TwoRegisterInstruction
        val origObjReg = firstInst?.registerB ?: (mutableShowCloud.implementation!!.registerCount - 1)
        val (targetMethod, rOp, shiftedIdx) = resolveLowRegisterAt(mutableShowCloud, 0, origObjReg)
        val shiftedFirst = targetMethod.getInstruction(shiftedIdx) as? TwoRegisterInstruction
        val shiftedObjReg = shiftedFirst?.registerB ?: (targetMethod.implementation!!.registerCount - 1)
        val resumeInsn = targetMethod.getInstruction(shiftedIdx)
        if (shiftedObjReg in 0..15) {
            targetMethod.addInstructionsWithLabels(
                shiftedIdx,
                """
                const/16 v$rOp, $OP_IS_HIDE_CANVAS_POPUP_CLOUD
                invoke-static {v$rOp, v$shiftedObjReg}, $LIBRE_TRANSLATION_DISPATCH
                move-result-object v$rOp
                if-eqz v$rOp, :libre_show_orig_cloud
                const/4 v$rOp, 0x0
                return v$rOp
                """.trimIndent(),
                ExternalLabel("libre_show_orig_cloud", resumeInsn),
            )
        } else {
            val (target2, rArg, idx2) = resolveLowRegisterAt(targetMethod, shiftedIdx, rOp, shiftedObjReg)
            val resumeInsn2 = target2.getInstruction(idx2)
            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$rOp, $OP_IS_HIDE_CANVAS_POPUP_CLOUD
                const/4 v$rArg, 0x0
                invoke-static {v$rOp, v$rArg}, $LIBRE_TRANSLATION_DISPATCH
                move-result-object v$rOp
                if-eqz v$rOp, :libre_show_orig_cloud
                const/4 v$rOp, 0x0
                return v$rOp
                """.trimIndent(),
                ExternalLabel("libre_show_orig_cloud", resumeInsn2),
            )
        }
    }
}
