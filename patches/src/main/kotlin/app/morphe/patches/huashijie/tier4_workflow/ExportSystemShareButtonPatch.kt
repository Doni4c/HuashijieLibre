package app.morphe.patches.huashijie.tier4_workflow

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
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
        ensureExportAdapterShareHooksInjected()
        ensureExportActivityShareCallbacksHooked()
    }
}
