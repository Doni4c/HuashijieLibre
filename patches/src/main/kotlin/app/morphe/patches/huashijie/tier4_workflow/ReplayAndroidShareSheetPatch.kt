package app.morphe.patches.huashijie.tier4_workflow

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleClass
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel

private const val TOGGLE_KEY = "libre_replay_system_share"

@Suppress("unused")
val replayAndroidShareSheetPatch = bytecodePatch(
    name = "Replay Android Share Sheet",
    description = "Routes timelapse export sharing inside ReplayActivity to the Android System Share Sheet instead of ShareSDK.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        registerRxFileToolTargets(classes)
        val replayClass = classes.resolveSingleClass("ReplayActivity") {
            it.type == "Lnet/huanci/hsjpro/activities/ReplayActivity;"
        }
        val shareMethod = toMutableMethod(MultiVersionResolvers.resolveActivityShareCallback(replayClass))
        injectSystemShareGuard(shareMethod)
    }
}
