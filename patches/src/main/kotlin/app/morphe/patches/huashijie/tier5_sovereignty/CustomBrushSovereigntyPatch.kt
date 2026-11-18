package app.morphe.patches.huashijie.tier5_sovereignty

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "libre_local_brush_studio"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_SHOULD_ALLOW_BRUSH_EDITING = 50

@Suppress("unused")
val customBrushSovereigntyPatch = bytecodePatch(
    name = "Custom Brush Sovereignty",
    description = "Unlocks full offline parameter editing, renaming, and duplicating for all preset and imported local brushes without account ownership checks.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val chooseBrushClass = MultiVersionResolvers.resolveChooseBrushFragmentClass(classes)
        var method = toMutableMethod(MultiVersionResolvers.resolveChooseBrushCanEdit(chooseBrushClass))
        val retIndices = method.implementation!!.instructions.indices
            .filter { method.implementation!!.instructions[it].opcode == Opcode.RETURN }
            .reversed()
        check(retIndices.isNotEmpty()) {
            "RETURN not found in ChooseBrushFragment canEdit"
        }
        for ((idxCounter, retIdx) in retIndices.withIndex()) {
            val origRetReg = (method.implementation!!.instructions[retIdx] as OneRegisterInstruction).registerA
            val (target1, opScratch, idx1) = resolveLowRegisterAt(
                method = method,
                insertIndex = retIdx,
                origRetReg,
            )
            val midRetReg = (target1.implementation!!.instructions[idx1] as OneRegisterInstruction).registerA
            val (target2, nullScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
                midRetReg,
            )
            val finalRetReg = (target2.implementation!!.instructions[idx2] as OneRegisterInstruction).registerA
            val resumeInsn = target2.getInstruction(idx2)
            val setTrueLine = if (finalRetReg <= 15) {
                "const/4 v$finalRetReg, 1"
            } else {
                "const/16 v$finalRetReg, 1"
            }
            val labelName = "cond_keep_orig_brush_edit_$idxCounter"
            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_SHOULD_ALLOW_BRUSH_EDITING
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
