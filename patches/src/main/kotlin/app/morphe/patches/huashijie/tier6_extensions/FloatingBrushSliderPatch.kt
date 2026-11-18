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
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_floating_brush_sliders"
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
    description = "Adds a draggable grip handle above the brush size and opacity sliders so artists can reposition them freely.",
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

        // Guard null View.setPadding calls after seekbars_view is promoted to paint_root_view.
        val leftSettingClass = classDefByOrNull("Lnet/huanci/hsjpro/paint/views/PaintLeftSettingView;")
            ?: error("PaintLeftSettingView not found")
        val mutableLeftSetting = mutableClassDefBy(leftSettingClass)
        val locationMethods = mutableLeftSetting.methods.filter {
            it.name == "setSettingViewLocation" && it.returnType == "V" && it.implementation != null
        }
        check(locationMethods.isNotEmpty()) { "PaintLeftSettingView.setSettingViewLocation not found" }
        var guardedCount = 0
        for (locMethod in locationMethods) {
            val instrs = locMethod.implementation!!.instructions
            val padIndices = instrs.indices.filter { idx ->
                val inst = instrs[idx]
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    (ref.definingClass == "Landroid/view/View;" || ref.definingClass == "Landroid/view/ViewGroup;" || ref.definingClass == "Landroid/widget/RelativeLayout;" || ref.definingClass == "Landroid/widget/LinearLayout;") &&
                    ref.name == "setPadding"
            }.reversed()
            for (padIdx in padIndices) {
                val padInst = locMethod.implementation!!.instructions[padIdx]
                val viewReg = when (padInst) {
                    is FiveRegisterInstruction -> padInst.registerC
                    is RegisterRangeInstruction -> padInst.registerCount.takeIf { it > 0 }?.let { padInst.startRegister }
                    else -> null
                } ?: continue
                if (viewReg !in 0..255) continue
                locMethod.addInstructionsPreservingLabels(padIdx, "nop")
                val targetInst = locMethod.getInstruction(padIdx + 1)
                val labelName = "libre_pad_nonnull_$padIdx"
                locMethod.addInstructionsWithLabels(
                    padIdx + 1,
                    """
                    if-nez v$viewReg, :$labelName
                    return-void
                    """.trimIndent(),
                    ExternalLabel(labelName, targetInst),
                )
                guardedCount++
            }
        }
        check(guardedCount > 0) { "View.setPadding not found in PaintLeftSettingView.setSettingViewLocation" }
    }
}
