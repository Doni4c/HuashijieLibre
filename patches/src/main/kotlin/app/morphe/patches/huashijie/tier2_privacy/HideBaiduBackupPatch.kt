package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "pure_studio_hide_baidu_backup"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private val mainActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/MainActivity;"
    },
)

@Suppress("unused")
val hideBaiduBackupPatch = bytecodePatch(
    name = "Hide Baidu Cloud Backup Entry",
    description = "Safely hides the domestic third-party Baidu Netdisk backup entry inside the main navigation drawer with View.GONE.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val method = mainActivityInitViewFingerprint.method
        val instructions = method.implementation!!.instructions
        val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in MainActivity.initView()V" }

        val invokeHook = formatInvokeStaticSingle(
            "${WORKSPACE_UI_CONTROLLER}->applyBaiduBackupVisibility(Landroid/app/Activity;)V",
            method.thisRegisterIndex(),
        )
        method.addInstructionsPreservingLabels(retIdx, invokeHook)
    }
}
