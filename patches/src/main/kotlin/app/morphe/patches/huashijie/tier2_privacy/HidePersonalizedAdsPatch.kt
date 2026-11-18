package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.generalSettingInitViewFingerprint
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "pure_studio_hide_personalized_ads"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

@Suppress("unused")
val hidePersonalizedAdsPatch = bytecodePatch(
    name = "Hide Personalized Ads Setting",
    description = "Safely hides the domestic personalized ads preference row inside GeneralSettingActivity with View.GONE.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val method = generalSettingInitViewFingerprint.method
        val instructions = method.implementation!!.instructions
        val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in GeneralSettingActivity.initView()V" }

        val invokeHook = formatInvokeStaticSingle(
            "${WORKSPACE_UI_CONTROLLER}->applyPersonalizedAdsVisibility(Landroid/app/Activity;)V",
            method.thisRegisterIndex(),
        )
        method.addInstructionsPreservingLabels(retIdx, invokeHook)
    }
}
