package app.morphe.patches.huashijie.tier3_ui_polish

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

private const val TOGGLE_KEY = "pure_studio_block_brush_upgrade_tip"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_SHOULD_BLOCK_BRUSH_UPGRADE_TIP = 13

private val scheduleBrushUpgradeTipFingerprint = Fingerprint(
    name = "scheduleBrushUpgradeTip",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/paint/views/SelectPenView;"
    },
)

@Suppress("unused")
val blockBrushUpgradeTipPatch = bytecodePatch(
    name = "Block Brush Upgrade Tip",
    description = "Silences promotional brush upgrade banners and tooltips in SelectPenView while routing classes4.dex through TranslationEngine.dispatch.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val method = scheduleBrushUpgradeTipFingerprint.method
        val origThis = method.thisRegisterIndex()
        val (target, scratch, insertIdx) = resolveLowRegisterAt(
            method = method,
            insertIndex = 0,
            origThis,
        )
        val newThis = target.thisRegisterIndex()
        check(newThis in 0..15) {
            "Expected low thisRegister in SelectPenView.scheduleBrushUpgradeTip, got v$newThis"
        }

        val resumeInsn: Instruction = target.getInstruction(insertIdx)
        target.addInstructionsWithLabels(
            insertIdx,
            """
            const/16 v$scratch, $OP_SHOULD_BLOCK_BRUSH_UPGRADE_TIP
            invoke-static {v$scratch, v$newThis}, $TRANSLATION_ENGINE_DISPATCH
            move-result-object v$scratch
            if-eqz v$scratch, :cond_allow_brush_tip
            return-void
            """.trimIndent(),
            ExternalLabel("cond_allow_brush_tip", resumeInsn),
        )
    }
}
