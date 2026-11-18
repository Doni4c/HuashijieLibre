package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "pure_studio_suppress_splash_ad"
private const val STARTUP_LIFECYCLE_CONTROLLER =
    "Lapp/morphe/extension/huashijie/StartupLifecycleController;"

@Suppress("unused")
val suppressSplashAdPatch = bytecodePatch(
    name = "Suppress Splash Ad Loading",
    description = "Blocks splash advertisement ad slot loading checks inside FlashActivity independently of Instant Launch.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val flashClass = MultiVersionResolvers.resolveFlashActivityClass(classes)
        val canLoadAd = toMutableMethod(MultiVersionResolvers.resolveFlashCanLoadAd(flashClass))
        val insns = canLoadAd.implementation!!.instructions
        val retIdx = insns.indexOfLast { it.opcode == Opcode.RETURN }
        check(retIdx >= 0) { "RETURN not found in FlashActivity canLoadAd" }
        val retReg = (insns[retIdx] as OneRegisterInstruction).registerA
        val invokeAllowAd = formatInvokeStaticSingle(
            "${STARTUP_LIFECYCLE_CONTROLLER}->shouldAllowSplashAd(Z)Z",
            retReg,
        )
        canLoadAd.addInstructionsPreservingLabels(
            retIdx,
            """
            $invokeAllowAd
            move-result v$retReg
            """.trimIndent(),
        )
    }
}
