package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "libre_free_ref_rotation"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_IS_REF_ROTATION_ENABLED = 70

private val referenceModelIsAllowRotateFingerprint = Fingerprint(
    name = "isAllowRotate",
    returnType = "Z",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/model/ReferenceModel;"
    },
)

@Suppress("unused")
val twoFingerRefRotationPatch = bytecodePatch(
    name = "Two-Finger Reference Rotation",
    description = "Unlocks smooth two-finger free rotation inside the floating reference image window.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        var method = referenceModelIsAllowRotateFingerprint.method
        val initImpl = method.implementation!!
        if (initImpl.registerCount < 4) {
            val (expanded, _) = method.addFreeRegisters(4 - initImpl.registerCount)
            method = expanded
        }

        val retIndices = method.implementation!!.instructions.indices
            .filter { method.implementation!!.instructions[it].opcode == Opcode.RETURN }
            .reversed()
        check(retIndices.isNotEmpty()) {
            "RETURN not found in ReferenceModel.isAllowRotate()Z"
        }

        for ((idxCounter, retIdx) in retIndices.withIndex()) {
            val origRetReg = (method.implementation!!.instructions[retIdx] as OneRegisterInstruction).registerA
            val thisReg1 = method.thisRegisterIndex()
            val (target1, opScratch, idx1) = resolveLowRegisterAt(
                method = method,
                insertIndex = retIdx,
                origRetReg,
                thisReg1,
            )
            val midRetReg = (target1.implementation!!.instructions[idx1] as OneRegisterInstruction).registerA
            val thisReg2 = target1.thisRegisterIndex()
            val (target2, nullScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
                midRetReg,
                thisReg2,
            )
            val finalRetReg = (target2.implementation!!.instructions[idx2] as OneRegisterInstruction).registerA
            val resumeInsn = target2.getInstruction(idx2)
            val setTrueLine = if (finalRetReg <= 15) {
                "const/4 v$finalRetReg, 1"
            } else {
                "const/16 v$finalRetReg, 1"
            }
            val labelName = "cond_keep_orig_ref_rot_$idxCounter"
            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_IS_REF_ROTATION_ENABLED
                const/4 v$nullScratch, 0
                invoke-static {v$opScratch, v$nullScratch}, $TRANSLATION_ENGINE_DISPATCH
                move-result-object v$opScratch
                if-eqz v$opScratch, :$labelName
                $setTrueLine
                """.trimIndent(),
                ExternalLabel(labelName, resumeInsn),
            )
            method = target2
        }
    }
}
