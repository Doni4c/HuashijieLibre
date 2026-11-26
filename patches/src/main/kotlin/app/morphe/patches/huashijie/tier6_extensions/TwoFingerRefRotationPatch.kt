package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.findPrevOpcode
import app.morphe.patches.huashijie.core.invokeRegisterList
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_free_ref_rotation"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_IS_REF_ROTATION_ENABLED = 70
private const val OP_FILTER_REF_ROTATION = 74

private fun instructionReadsRegister(inst: Instruction, targetReg: Int): Boolean {
    val op = inst.opcode
    val opName = op.name
    if (opName.startsWith("INVOKE_")) {
        return targetReg in invokeRegisterList(inst)
    }
    if (opName.startsWith("APUT")) {
        val three = inst as? ThreeRegisterInstruction ?: return false
        return three.registerA == targetReg || three.registerB == targetReg || three.registerC == targetReg
    }
    if (inst is ThreeRegisterInstruction) {
        return inst.registerB == targetReg || inst.registerC == targetReg
    }
    if (inst is TwoRegisterInstruction) {
        if (!op.setsRegister() || opName.endsWith("_2ADDR")) {
            return inst.registerA == targetReg || inst.registerB == targetReg
        }
        return inst.registerB == targetReg
    }
    if (inst is OneRegisterInstruction) {
        if (!op.setsRegister() || op == Opcode.CHECK_CAST) {
            return inst.registerA == targetReg
        }
        return false
    }
    return false
}

private fun instructionWritesRegisterUnconditionally(inst: Instruction, targetReg: Int): Boolean {
    val op = inst.opcode
    if (!op.setsRegister() || op == Opcode.CHECK_CAST) return false
    val regA = (inst as? OneRegisterInstruction)?.registerA ?: return false
    return regA == targetReg || (op.setsWideRegister() && regA + 1 == targetReg)
}

private fun isRegisterDeadFrom(method: MutableMethod, startIdx: Int, targetReg: Int): Boolean {
    val impl = method.implementation ?: return false
    val instructions = impl.instructions
    val addrToIdx = HashMap<Int, Int>(instructions.size)
    for (i in instructions.indices) {
        addrToIdx[instructions[i].location.codeAddress] = i
    }

    val visited = HashSet<Int>()
    val queue = ArrayDeque<Int>()
    queue.addLast(startIdx)

    while (queue.isNotEmpty()) {
        val idx = queue.removeFirst()
        if (idx !in instructions.indices || !visited.add(idx)) continue

        val inst = instructions[idx]
        if (instructionReadsRegister(inst, targetReg)) {
            return false
        }
        if (instructionWritesRegisterUnconditionally(inst, targetReg)) {
            continue
        }

        val addr = inst.location.codeAddress
        for (tryBlock in impl.tryBlocks) {
            val tryStart = tryBlock.startCodeAddress
            val tryEnd = tryStart + tryBlock.codeUnitCount
            if (addr in tryStart until tryEnd) {
                for (handler in tryBlock.exceptionHandlers) {
                    addrToIdx[handler.handlerCodeAddress]?.let { queue.addLast(it) }
                }
            }
        }

        val op = inst.opcode
        if (op.canContinue() && idx + 1 < instructions.size) {
            queue.addLast(idx + 1)
        }
        if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH) {
            val payloadAddr = addr + (inst as OffsetInstruction).codeOffset
            val payloadIdx = addrToIdx[payloadAddr]
            if (payloadIdx != null) {
                when (val payload = instructions[payloadIdx]) {
                    is PackedSwitchPayload -> payload.switchElements.forEach { elem ->
                        addrToIdx[addr + elem.offset]?.let { queue.addLast(it) }
                    }
                    is SparseSwitchPayload -> payload.switchElements.forEach { elem ->
                        addrToIdx[addr + elem.offset]?.let { queue.addLast(it) }
                    }
                }
            }
        } else if (inst is OffsetInstruction) {
            addrToIdx[addr + inst.codeOffset]?.let { queue.addLast(it) }
        }
    }
    return true
}

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
    description = "Enables two-finger rotation inside the floating reference window with an 8-degree anti-tilt deadzone during pinch zoom.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val navTargets = MultiVersionResolvers.resolveNavigatorTouchRunnableTargets(allClasses())
        registerResolvedTarget("NavigatorTouchRunnable.navViewField", navTargets.navViewField.name)
        registerResolvedTarget("NavigatorTouchRunnable.gestureTokenField", navTargets.gestureTokenField.name)
        registerResolvedTarget("NavigatorTouchRunnable.x0Field", navTargets.x0Field.name)
        registerResolvedTarget("NavigatorTouchRunnable.y0Field", navTargets.y0Field.name)
        registerResolvedTarget("NavigatorTouchRunnable.x1Field", navTargets.x1Field.name)
        registerResolvedTarget("NavigatorTouchRunnable.y1Field", navTargets.y1Field.name)

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

        // Hook NavigatorTouchRunnable.run()V to enforce an 8-degree two-finger rotation deadzone
        // on the floating reference window (mRenderType == 1) without altering classes4.dex method count.
        val navRun = toMutableMethod(navTargets.runMethod)
        val navInsns = navRun.implementation!!.instructions
        val atan2Idx = navInsns.indexOfFirst { insn ->
            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
            insn.opcode == Opcode.INVOKE_STATIC &&
                ref.definingClass == "Ljava/lang/Math;" &&
                ref.name == "atan2" &&
                ref.parameterTypes.map { it.toString() } == listOf("D", "D") &&
                ref.returnType == "D"
        }
        check(atan2Idx >= 0) {
            "Math.atan2 call not found in ${navRun.definingClass}->run()V"
        }

        val searchEnd = minOf(navInsns.lastIndex, atan2Idx + 24)
        val rotSetterMatches = ((atan2Idx + 1)..searchEnd).filter { idx ->
            val insn = navInsns[idx]
            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
            insn.opcode == Opcode.INVOKE_STATIC &&
                ref.definingClass == "Lnet/huanci/paintlib/views/NavigatorView;" &&
                ref.parameterTypes.map { it.toString() } == listOf("Lnet/huanci/paintlib/views/NavigatorView;", "F") &&
                ref.returnType == "F"
        }
        check(rotSetterMatches.size == 1) {
            "Expected exactly 1 static (NavigatorView, F)F rotation setter after Math.atan2 in ${navRun.definingClass}->run()V, found ${rotSetterMatches.size}"
        }
        val rotSetterIdx = rotSetterMatches[0]
        val setterRegs = invokeRegisterList(navInsns[rotSetterIdx])
        check(setterRegs.size == 2) {
            "Expected 2 argument registers on rotation setter at $rotSetterIdx in ${navRun.definingClass}->run()V"
        }

        val igetNavIdx = navInsns.findPrevOpcode(rotSetterIdx, Opcode.IGET_OBJECT, maxLookback = 6)
        check(igetNavIdx > atan2Idx) {
            "Expected IGET_OBJECT for NavigatorView between Math.atan2 ($atan2Idx) and rotation setter ($rotSetterIdx) in ${navRun.definingClass}->run()V"
        }
        val igetNavInsn = navInsns[igetNavIdx] as TwoRegisterInstruction
        val igetNavField = (igetNavInsn as? ReferenceInstruction)?.reference as? FieldReference
        check(
            igetNavField != null &&
                igetNavField.definingClass == navTargets.runnableClass.type &&
                igetNavField.name == navTargets.navViewField.name &&
                igetNavField.type == "Lnet/huanci/paintlib/views/NavigatorView;" &&
                igetNavInsn.registerA == setterRegs[0],
        ) {
            "Unexpected IGET_OBJECT target $igetNavField before rotation setter in ${navRun.definingClass}->run()V"
        }

        val subFloatIdx = maxOf(
            navInsns.findPrevOpcode(rotSetterIdx, Opcode.SUB_FLOAT, maxLookback = 6),
            navInsns.findPrevOpcode(rotSetterIdx, Opcode.SUB_FLOAT_2ADDR, maxLookback = 6),
        )
        check(subFloatIdx > atan2Idx) {
            "Expected SUB_FLOAT/SUB_FLOAT_2ADDR between Math.atan2 ($atan2Idx) and rotation setter ($rotSetterIdx) in ${navRun.definingClass}->run()V"
        }
        val subFloatInsn = navInsns[subFloatIdx] as OneRegisterInstruction

        val scratchReg = igetNavInsn.registerA
        val thisObjReg = igetNavInsn.registerB
        val deltaReg = subFloatInsn.registerA
        val insertIdx = rotSetterIdx + 1
        check(insertIdx < navInsns.size) {
            "Unexpected end of method after rotation setter in ${navRun.definingClass}->run()V"
        }
        check(
            scratchReg in 0..15 &&
                thisObjReg in 0..15 &&
                deltaReg in 0..15 &&
                scratchReg != thisObjReg &&
                scratchReg != deltaReg,
        ) {
            "Unexpected registers in ${navRun.definingClass}->run()V: scratch=$scratchReg, this=$thisObjReg, delta=$deltaReg"
        }
        check(isRegisterDeadFrom(navRun, insertIdx, scratchReg)) {
            "Register v$scratchReg is still read after insertIdx=$insertIdx in ${navRun.definingClass}->run()V"
        }

        navRun.addInstructionsWithLabels(
            insertIdx,
            """
            const/16 v$scratchReg, $OP_FILTER_REF_ROTATION
            invoke-static {v$scratchReg, v$thisObjReg}, $TRANSLATION_ENGINE_DISPATCH
            move-result-object v$scratchReg
            if-eqz v$scratchReg, :libre_keep_native_rot
            check-cast v$scratchReg, Ljava/lang/Float;
            invoke-virtual {v$scratchReg}, Ljava/lang/Float;->floatValue()F
            move-result v$deltaReg
            """.trimIndent(),
            ExternalLabel("libre_keep_native_rot", navRun.getInstruction(insertIdx)),
        )
    }
}
