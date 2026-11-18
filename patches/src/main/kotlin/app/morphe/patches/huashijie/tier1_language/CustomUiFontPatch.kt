package app.morphe.patches.huashijie.tier1_language

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

private const val UI_UX_CONTROLLER_CLASS =
    "Lapp/morphe/extension/huashijie/UiUxController;"

private val drawApplicationOnCreateFingerprint = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/DrawApplication;"
    },
)

@Suppress("unused")
val customUiFontPatch = bytecodePatch(
    name = "Custom UI Font Injection",
    description = "Applies your chosen .ttf or .otf fonts from /sdcard/HuashijiePro/fonts/ across the app while keeping bold and italic styles.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel("libre_ui_font_file")

        val method = drawApplicationOnCreateFingerprint.method
        val instructions = method.implementation!!.instructions
        val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in DrawApplication.onCreate()V" }

        val thisReg = method.thisRegisterIndex()
        val invokeInitFontLine = formatInvokeStaticSingle(
            "${UI_UX_CONTROLLER_CLASS}->initFontLifecycle(Landroid/app/Application;)V",
            thisReg,
        )
        method.addInstructionsPreservingLabels(retIdx, invokeInitFontLine)
    }
}
