package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_floating_brush_sliders"
private const val OP_BEFORE_LEFT_SETTING_LAYOUT = 75
private const val OP_AFTER_LEFT_SETTING_LAYOUT = 76
private const val SETUP_FLOATING_SLIDERS =
    "Lapp/morphe/extension/huashijie/UiUxController;->setupFloatingSliders(Landroid/app/Activity;Landroid/view/View;)V"

private val paintActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/PaintActivity;"
    },
)

@Suppress("unused")
val floatingBrushSliderPatch = bytecodePatch(
    name = "Floating Brush Sliders",
    description = "Adds a 2x2 drag handle between the brush size and opacity sliders to reposition them and compact landscape sidebars.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        val seekbarsField = MultiVersionResolvers.resolvePaintSeekbarsViewField(paintClass)
        val seekbarsFieldSig = "${seekbarsField.definingClass}->${seekbarsField.name}:${seekbarsField.type}"

        var method = paintActivityInitViewFingerprint.method
        val returnCount = method.implementation!!.instructions.count {
            it.opcode == Opcode.RETURN_VOID
        }
        check(returnCount > 0) { "RETURN_VOID not found in PaintActivity.initView()V" }

        for (occurrence in (returnCount - 1) downTo 0) {
            val retIdx = method.implementation!!.instructions
                .withIndex()
                .filter { it.value.opcode == Opcode.RETURN_VOID }[occurrence]
                .index
            val origThisReg = method.thisRegisterIndex()
            val (target1, sliderReg, idx1) = resolveLowRegisterAt(
                method = method,
                insertIndex = retIdx,
                origThisReg,
            )
            val shiftedThisReg = target1.thisRegisterIndex()
            if (shiftedThisReg in 0..15) {
                target1.addInstructionsPreservingLabels(
                    idx1,
                    """
                    iget-object v$sliderReg, v$shiftedThisReg, $seekbarsFieldSig
                    invoke-static {v$shiftedThisReg, v$sliderReg}, $SETUP_FLOATING_SLIDERS
                    """.trimIndent(),
                )
                method = target1
            } else {
                val (target2, lowThisReg, idx2) = resolveLowRegisterAt(
                    method = target1,
                    insertIndex = idx1,
                    sliderReg,
                    shiftedThisReg,
                )
                val finalThisReg = target2.thisRegisterIndex()
                target2.addInstructionsPreservingLabels(
                    idx2,
                    """
                    move-object/from16 v$lowThisReg, v$finalThisReg
                    iget-object v$sliderReg, v$lowThisReg, $seekbarsFieldSig
                    invoke-static {v$lowThisReg, v$sliderReg}, $SETUP_FLOATING_SLIDERS
                    """.trimIndent(),
                )
                method = target2
            }
        }

        // Temporarily restore promoted seekbars_view around PaintLeftSettingView.setSettingViewLocation()V
        // so the full original layout computation runs without truncating recyclerView height rules.
        val leftSettingClass = classDefByOrNull("Lnet/huanci/hsjpro/paint/views/PaintLeftSettingView;")
            ?: error("PaintLeftSettingView not found")
        val mutableLeftSetting = mutableClassDefBy(leftSettingClass)
        val locationMethods = mutableLeftSetting.methods.filter {
            it.name == "setSettingViewLocation" && it.returnType == "V" && it.implementation != null
        }
        check(locationMethods.isNotEmpty()) { "PaintLeftSettingView.setSettingViewLocation not found" }
        var hookedCount = 0
        for (locMethod in locationMethods) {
            var currentMethod = locMethod
            val padIndices = currentMethod.implementation!!.instructions.indices
                .filter { idx ->
                    val insn = currentMethod.implementation!!.instructions[idx]
                    if (insn.opcode != Opcode.INVOKE_VIRTUAL) return@filter false
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
                    ref.definingClass == "Landroid/view/View;" &&
                        ref.name == "setPadding" &&
                        ref.parameterTypes == listOf("I", "I", "I", "I") &&
                        ref.returnType == "V"
                }
                .reversed()
            for (padIdx in padIndices) {
                val insns = currentMethod.implementation!!.instructions
                val fiveReg = insns[padIdx] as? FiveRegisterInstruction ?: continue
                val viewReg = fiveReg.registerC
                if (viewReg !in 0..255) continue
                var insertAt = padIdx
                if (currentMethod.getInstruction(insertAt).location.labels.isNotEmpty()) {
                    currentMethod.addInstructionsPreservingLabels(insertAt, "nop")
                    insertAt++
                }
                val afterPadInsn = currentMethod.getInstruction(insertAt + 1)
                currentMethod.addInstructionsWithLabels(
                    insertAt,
                    "if-eqz v$viewReg, :libre_skip_null_pad_$padIdx",
                    ExternalLabel("libre_skip_null_pad_$padIdx", afterPadInsn),
                )
            }
            val retIndices = currentMethod.implementation!!.instructions.indices
                .filter { currentMethod.implementation!!.instructions[it].opcode == Opcode.RETURN_VOID }
                .reversed()
            check(retIndices.isNotEmpty()) {
                "RETURN_VOID not found in PaintLeftSettingView.setSettingViewLocation"
            }
            for (retIdx in retIndices) {
                currentMethod = emitTranslationDispatch(
                    method = currentMethod,
                    insertIndex = retIdx,
                    opCode = OP_AFTER_LEFT_SETTING_LAYOUT,
                    preserveLabels = true,
                ) { m, _ -> m.thisRegisterIndex() }
            }
            emitTranslationDispatch(
                method = currentMethod,
                insertIndex = 0,
                opCode = OP_BEFORE_LEFT_SETTING_LAYOUT,
                preserveLabels = false,
            ) { m, _ -> m.thisRegisterIndex() }
            hookedCount++
        }
        check(hookedCount > 0) { "No setSettingViewLocation method hooked in PaintLeftSettingView" }
    }
}
