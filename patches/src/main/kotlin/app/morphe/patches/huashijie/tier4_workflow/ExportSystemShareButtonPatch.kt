package app.morphe.patches.huashijie.tier4_workflow

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.tier3_ui_polish.ensureExportAdapterShareHooksInjected

private const val TOGGLE_KEY = "libre_export_system_share_button"

@Suppress("unused")
val exportSystemShareButtonPatch = bytecodePatch(
    name = "Export Sheet Share Button",
    description = "Adds a standalone Android System Share button next to Album and File inside the export sheet for both artwork and timelapse videos.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val epv = MultiVersionResolvers.resolveExportPictureViewClass(classes)
        val cbInterface = MultiVersionResolvers.resolveExportPictureCallbackInterface(classes)
        val cbInvoke = MultiVersionResolvers.resolveExportPictureCallbackInvoke(cbInterface)
        val sizeCbClass = MultiVersionResolvers.resolveExportPictureSizeCallbackClass(classes, cbInvoke)

        registerResolvedTarget(
            "ExportPictureView.callbackField",
            MultiVersionResolvers.resolveExportPictureCallbackField(epv, cbInterface).name,
        )
        registerResolvedTarget(
            "ExportPictureView.formatField",
            MultiVersionResolvers.resolveExportPictureFormatField(epv).name,
        )
        registerResolvedTarget(
            "ExportPictureView.watermarkSwitchField",
            MultiVersionResolvers.resolveExportPictureWatermarkSwitchField(epv).name,
        )
        registerResolvedTarget(
            "ExportPictureView.shareProgressField",
            MultiVersionResolvers.resolveExportPictureShareProgressField(epv).name,
        )
        registerResolvedTarget("ExportPictureView.sizeCallbackClass", sizeCbClass.type.toJavaClassName())
        registerResolvedTarget(
            "ExportPictureView.sizeCallbackField",
            MultiVersionResolvers.resolveExportPictureSizeCallbackField(epv, sizeCbClass).name,
        )
        registerResolvedTarget(
            "ExportPictureView.sizeTypeField",
            MultiVersionResolvers.resolveExportPictureSizeTypeField(epv).name,
        )
        registerResolvedTarget(
            "ExportPictureView.customWidthField",
            MultiVersionResolvers.resolveExportPictureCustomWidthField(epv).name,
        )
        registerResolvedTarget(
            "ExportPictureView.customHeightField",
            MultiVersionResolvers.resolveExportPictureCustomHeightField(epv).name,
        )
        registerResolvedTarget("ExportPictureView.callbackInvoke", cbInvoke.name)
        registerRxFileToolTargets(classes)

        ensureExportAdapterShareHooksInjected()
        ensureExportActivityShareCallbacksHooked()
    }
}
