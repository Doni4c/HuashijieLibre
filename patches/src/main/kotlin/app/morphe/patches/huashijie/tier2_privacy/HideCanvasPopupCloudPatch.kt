package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
import app.morphe.patches.huashijie.core.hookPaintBubblePopupBeforeShow
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel

private const val TOGGLE_KEY = "pure_studio_hide_canvas_popup_cloud"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

@Suppress("unused")
val hideCanvasPopupCloudPatch = bytecodePatch(
    name = "Hide Canvas Popup Cloud Sync",
    description = "Hides the cloud draft button and cloud sync tab inside the PaintActivity canvas settings popup and rebalances remaining controls.",
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
    }
}
