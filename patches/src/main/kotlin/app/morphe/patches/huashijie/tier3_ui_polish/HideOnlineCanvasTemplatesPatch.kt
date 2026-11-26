package app.morphe.patches.huashijie.tier3_ui_polish

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

private const val TOGGLE_KEY = "pure_studio_optimize_create_canvas"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private val createCanvasInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/CreateCanvasActivity;"
    },
)

@Suppress("unused")
val hideOnlineCanvasTemplatesPatch = bytecodePatch(
    name = "Hide Online Canvas Templates",
    description = "Hides the online template button on the New Canvas screen and centers the three local creation options.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val method = createCanvasInitViewFingerprint.method
        val instructions = method.implementation!!.instructions
        val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in CreateCanvasActivity.initView()V" }

        val thisReg = method.thisRegisterIndex()
        val invokeOptimize = formatInvokeStaticSingle(
            "${WORKSPACE_UI_CONTROLLER}->optimizeCreateCanvasLayout(Landroid/app/Activity;)V",
            thisReg,
        )
        method.addInstructionsPreservingLabels(retIdx, invokeOptimize)
    }
}
