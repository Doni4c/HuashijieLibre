package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleClass
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_clean_speedpaint"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_IS_CLEAN_SPEEDPAINT = 71

private fun resolveLastBooleanParamRegister(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod): Int {
    val params = method.parameterTypes.map { it.toString() }
    val targetIdx = params.indexOfLast { it == "Z" }
    check(targetIdx >= 0) {
        "Boolean parameter not found in ${method.definingClass}->${method.name}(${params.joinToString("")})"
    }
    var reg = method.thisRegisterIndex() + if (AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1
    for (i in 0 until targetIdx) {
        val t = params[i]
        reg += if (t == "J" || t == "D") 2 else 1
    }
    return reg
}

@Suppress("unused")
val watermarkFreeTimelapsePatch = bytecodePatch(
    name = "Watermark Free Timelapse Export",
    description = "Removes the brand watermark overlay during ReplayActivity preview and speedpaint video export while preserving video quality.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val wmClass = classes.resolveSingleClass("WaterMarkView") {
            it.type == "Lnet/huanci/hsjpro/views/WaterMarkView;"
        }
        val wmOnDraw = toMutableMethod(
            wmClass.methods.first {
                it.name == "onDraw" &&
                    it.returnType == "V" &&
                    it.parameterTypes.map { p -> p.toString() } == listOf("Landroid/graphics/Canvas;")
            },
        )
        run {
            val origThis = wmOnDraw.thisRegisterIndex()
            val origCanvas = origThis + 1
            val (target1, opScratch, idx1) = resolveLowRegisterAt(
                method = wmOnDraw,
                insertIndex = 0,
                origThis,
                origCanvas,
            )
            val shiftedThis = target1.thisRegisterIndex()
            val (target2, nullScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
                shiftedThis,
                shiftedThis + 1,
            )
            val resumeInsn = target2.getInstruction(idx2)
            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_IS_CLEAN_SPEEDPAINT
                const/4 v$nullScratch, 0
                invoke-static {v$opScratch, v$nullScratch}, $TRANSLATION_ENGINE_DISPATCH
                move-result-object v$opScratch
                if-eqz v$opScratch, :cond_draw_watermark
                return-void
                """.trimIndent(),
                ExternalLabel("cond_draw_watermark", resumeInsn),
            )
        }

        val wmFieldRef = MultiVersionResolvers.resolveTimelapseWatermarkField(classes)
        val paintViewClass = classes.resolveSingleClass("PaintView") {
            it.type == "Lnet/huanci/paintlib/views/PaintView;"
        }
        val setExportMethod = toMutableMethod(
            paintViewClass.methods.first {
                it.name == "setExportVideoParams" &&
                    it.returnType == "V" &&
                    it.parameterTypes.map { p -> p.toString() } == listOf("Z", "Z")
            },
        )
        run {
            val instructions = setExportMethod.implementation!!.instructions
            val iputIdx = instructions.indexOfFirst { inst ->
                if (inst.opcode != Opcode.IPUT_BOOLEAN) return@indexOfFirst false
                val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference
                ref?.definingClass == wmFieldRef.definingClass && ref.name == wmFieldRef.name
            }
            check(iputIdx >= 0) { "PAINT_PARAMS watermark IPUT_BOOLEAN not found in PaintView.setExportVideoParams" }
            val origIput = instructions[iputIdx] as TwoRegisterInstruction
            val (target1, opScratch, idx1) = resolveLowRegisterAt(
                method = setExportMethod,
                insertIndex = iputIdx,
                origIput.registerA,
                origIput.registerB,
            )
            val midIput = target1.implementation!!.instructions[idx1] as TwoRegisterInstruction
            val (target2, nullScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
                midIput.registerA,
                midIput.registerB,
            )
            val finalIput = target2.implementation!!.instructions[idx2] as TwoRegisterInstruction
            val wmFlagReg = finalIput.registerA
            val clearFlagLine = if (wmFlagReg <= 15) {
                "const/4 v$wmFlagReg, 0"
            } else {
                "const/16 v$wmFlagReg, 0"
            }
            val resumeInsn = target2.getInstruction(idx2)
            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_IS_CLEAN_SPEEDPAINT
                const/4 v$nullScratch, 0
                invoke-static {v$opScratch, v$nullScratch}, $TRANSLATION_ENGINE_DISPATCH
                move-result-object v$opScratch
                if-eqz v$opScratch, :cond_keep_video_wm
                $clearFlagLine
                """.trimIndent(),
                ExternalLabel("cond_keep_video_wm", resumeInsn),
            )
        }

        val glRenderMethod = toMutableMethod(MultiVersionResolvers.resolveGlVideoWatermarkRenderFrame(classes))
        run {
            val wmParamReg = resolveLastBooleanParamRegister(glRenderMethod)
            val instructions = glRenderMethod.implementation!!.instructions
            val ifEqzIdx = instructions.indexOfFirst { inst ->
                inst.opcode == Opcode.IF_EQZ &&
                    (inst as? OneRegisterInstruction)?.registerA == wmParamReg
            }
            check(ifEqzIdx >= 0) {
                "if eqz watermark gate not found in EglConfigBase renderFrame"
            }
            val insertIdx = ifEqzIdx + 1
            val (target1, opScratch, idx1) = resolveLowRegisterAt(
                method = glRenderMethod,
                insertIndex = insertIdx,
            )
            val (target2, nullScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
            )
            val targetInstructions = target2.implementation!!.instructions
            val glDisableInsn = targetInstructions.drop(idx2).firstOrNull { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Landroid/opengl/GLES20;" && ref.name == "glDisable"
            } ?: error("GLES20.glDisable skip target not found in EglConfigBase renderFrame")

            target2.addInstructionsWithLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_IS_CLEAN_SPEEDPAINT
                const/4 v$nullScratch, 0
                invoke-static {v$opScratch, v$nullScratch}, $TRANSLATION_ENGINE_DISPATCH
                move-result-object v$opScratch
                if-nez v$opScratch, :cond_skip_gl_wm
                """.trimIndent(),
                ExternalLabel("cond_skip_gl_wm", glDisableInsn),
            )
        }
    }
}
