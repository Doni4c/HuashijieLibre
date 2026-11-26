package app.morphe.patches.huashijie.tier1_language

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleClass
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.findMoveResultObjectAfter
import app.morphe.patches.huashijie.core.findNextOpcode
import app.morphe.patches.huashijie.core.findPrevOpcode
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.hookBottomSheetOnStartDispatch
import app.morphe.patches.huashijie.core.invokeRegisterList
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val TRANSLATION_ENGINE_CLASS =
    "Lapp/morphe/extension/huashijie/TranslationEngine;"

private const val DISPATCH_SIGNATURE =
    "${TRANSLATION_ENGINE_CLASS}->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"

private const val OP_TRANSLATE_VIEW = 1
private const val OP_ATTACH_OBSERVER = 2
private const val OP_TRANSLATE_AND_OBSERVE = 3
private const val OP_DIALOG_PRE_SHOW = 4
private const val OP_TRANSLATE_STRING_ARRAY = 5
private const val OP_TRANSLATE_STRING = 6
private const val OP_ON_TOAST_TEXT = 7

private enum class TargetDex {
    CLASSES3,
    CLASSES4,
    OTHER,
}

private val baseActivityOnCreateFingerprint = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef ->
        classDef.sourceFile == "BaseActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val baseActivityOnResumeFingerprint = Fingerprint(
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "BaseActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val commonDialogShowFingerprint = Fingerprint(
    name = "show",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "CommonDialog.java" &&
            classDef.type == "Lnet/huanci/hsjpro/dialog/CommonDialog;"
    },
)

private val commonDialogOnStartFingerprint = Fingerprint(
    name = "onStart",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "CommonDialog.java" &&
            classDef.type == "Lnet/huanci/hsjpro/dialog/CommonDialog;"
    },
)

private val replayActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "ReplayActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val brushSettingInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "BrushSettingActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val uiBrushSettingInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "UiBrushSettingActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val baseFragmentOnCreateViewFingerprint = Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "BaseFragment.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val baseDialogFragmentOnStartFingerprint = Fingerprint(
    name = "onStart",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "BaseDialogFragment.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val exportPictureViewOnCreateViewFingerprint = Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "ExportPictureView.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val exportPictureView3OnCreateViewFingerprint = Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "ExportPictureView3.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

private val selectPenViewOnFinishInflateFingerprint = Fingerprint(
    name = "onFinishInflate",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "SelectPenView.java" &&
            classDef.type.startsWith("Lnet/huanci/")
    },
)

private fun invokeArgRegister(instruction: Instruction, argIndex: Int): Int? =
    when (instruction) {
        is FiveRegisterInstruction -> when (argIndex) {
            0 -> instruction.registerC
            1 -> instruction.registerD
            2 -> instruction.registerE
            3 -> instruction.registerF
            4 -> instruction.registerG
            else -> null
        }
        is RegisterRangeInstruction -> {
            if (argIndex in 0 until instruction.registerCount) {
                instruction.startRegister + argIndex
            } else {
                null
            }
        }
        else -> null
    }

private fun formatInvokeStatic2(signature: String, regA: Int, regB: Int): String =
    "invoke-static {v$regA, v$regB}, $signature"

private fun BytecodePatchContext.hookAssetsUtilBridge(
    targetMethod: MutableMethod,
    bridgeSignature: String,
) {
    var method = targetMethod
    val initialInstructions = method.implementation!!.instructions
    val returnIndices = initialInstructions.indices
        .filter { initialInstructions[it].opcode == Opcode.RETURN_OBJECT }
        .reversed()
    check(returnIndices.isNotEmpty()) {
        "RETURN_OBJECT not found in ${method.definingClass}->${method.name}"
    }

    for (retIdx in returnIndices) {
        val currentInstructions = method.implementation!!.instructions
        val prevMoveIdx = currentInstructions.findPrevOpcode(retIdx, Opcode.MOVE_RESULT_OBJECT, 2)
        if (prevMoveIdx < 0) {
            continue
        }
        val retReg = (currentInstructions[retIdx] as OneRegisterInstruction).registerA
        val moveReg = (currentInstructions[prevMoveIdx] as OneRegisterInstruction).registerA
        val origParamReg = method.thisRegisterIndex()

        if (moveReg == origParamReg) {
            val (updated, tmpReg, shiftedMoveIdx) = resolveLowRegisterAt(
                method = method,
                insertIndex = prevMoveIdx,
                origParamReg,
            )
            method = updated
            val resIdReg = updated.thisRegisterIndex()
            check(resIdReg < 16 && tmpReg < 16) {
                "Expected 4-bit registers for AssetsUtil hook in ${updated.definingClass}->${updated.name}"
            }
            updated.replaceInstruction(shiftedMoveIdx, "move-result-object v$tmpReg")
            updated.addInstructions(
                shiftedMoveIdx + 1,
                """
                invoke-static {v$resIdReg, v$tmpReg}, $bridgeSignature
                move-result-object v$retReg
                """.trimIndent(),
            )
        } else if (origParamReg < 16 && retReg < 16) {
            method.addInstructionsPreservingLabels(
                retIdx,
                """
                invoke-static {v$origParamReg, v$retReg}, $bridgeSignature
                move-result-object v$retReg
                """.trimIndent(),
            )
        }
    }
}

private fun hookBaseActivityLifecycle(fingerprint: Fingerprint) {
    val method = fingerprint.method
    val instructions = method.implementation!!.instructions
    val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
    check(retIdx >= 0) {
        "RETURN_VOID not found in ${method.definingClass}->${method.name}"
    }
    val thisReg = method.thisRegisterIndex()
    method.addInstructionsPreservingLabels(
        retIdx,
        formatInvokeStaticSingle(
            "${TRANSLATION_ENGINE_CLASS}->attachToActivity(Landroid/app/Activity;)V",
            thisReg,
        ),
    )
}

private fun hookPaintPopupShadowCast(method: MutableMethod) {
    val instructions = method.implementation!!.instructions
    val castIdx = instructions.indexOfFirst { inst ->
        inst.opcode == Opcode.CHECK_CAST &&
            ((inst as? ReferenceInstruction)?.reference as? TypeReference)?.type ==
            "Lcom/lihang/ShadowLayout;"
    }
    check(castIdx >= 0) {
        "ShadowLayout check-cast not found in ${method.definingClass}->${method.name}"
    }
    val castReg = method.getInstruction<OneRegisterInstruction>(castIdx).registerA
    method.addInstructionsPreservingLabels(
        castIdx + 1,
        formatInvokeStaticSingle(
            "${TRANSLATION_ENGINE_CLASS}->attachViewTreeObserver(Landroid/view/View;)V",
            castReg,
        ),
    )
}

private fun BytecodePatchContext.hookActivityInitViewWindow(
    fingerprint: Fingerprint,
    translateFirst: Boolean,
) {
    val method = fingerprint.method
    val instructions = method.implementation!!.instructions
    val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
    check(retIdx >= 0) {
        "RETURN_VOID not found in ${method.definingClass}->${method.name}"
    }

    val origThisReg = method.thisRegisterIndex()
    val (targetMethod, tmpReg, insertAt) = resolveLowRegisterAt(
        method = method,
        insertIndex = retIdx,
        origThisReg,
    )
    val thisReg = targetMethod.thisRegisterIndex()
    val getWindowLine = if (thisReg in 0..15) {
        "invoke-virtual {v$thisReg}, Landroid/app/Activity;->getWindow()Landroid/view/Window;"
    } else {
        "invoke-virtual/range {v$thisReg .. v$thisReg}, Landroid/app/Activity;->getWindow()Landroid/view/Window;"
    }
    val translateLine = if (translateFirst) {
        "invoke-static {v$tmpReg}, ${TRANSLATION_ENGINE_CLASS}->translateViewHierarchy(Landroid/view/View;)V\n"
    } else {
        ""
    }
    targetMethod.addInstructionsPreservingLabels(
        insertAt,
        """
        $getWindowLine
        move-result-object v$tmpReg
        invoke-virtual {v$tmpReg}, Landroid/view/Window;->getDecorView()Landroid/view/View;
        move-result-object v$tmpReg
        ${translateLine}invoke-static {v$tmpReg}, ${TRANSLATION_ENGINE_CLASS}->attachViewTreeObserver(Landroid/view/View;)V
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.emitDispatchConstantAt(
    method: MutableMethod,
    insertIndex: Int,
    opCode: Int,
    argReg: Int,
    preserveLabels: Boolean,
    vararg extraExcluded: Int,
): MutableMethod {
    check(opCode in -8..7) { "const/4 supports 4-bit signed opcodes only: $opCode" }
    val allExcluded = (extraExcluded.toList() + argReg).distinct().toIntArray()
    val (updatedMethod, opReg, shiftedIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = insertIndex,
        *allExcluded,
    )
    // cloneMutable copies incoming params into their original register slots at entry,
    // so existing instructions continue referencing argReg directly.
    val smali = if (argReg in 0..15) {
        """
        const/4 v$opReg, $opCode
        ${formatInvokeStatic2(DISPATCH_SIGNATURE, opReg, argReg)}
        """.trimIndent()
    } else {
        val (objUpdated, objReg, objIdx) = resolveLowRegisterAt(
            method = updatedMethod,
            insertIndex = shiftedIdx,
            *(allExcluded + opReg),
        )
        val block = """
            const/4 v$opReg, $opCode
            move-object/from16 v$objReg, v$argReg
            ${formatInvokeStatic2(DISPATCH_SIGNATURE, opReg, objReg)}
        """.trimIndent()
        if (preserveLabels) {
            objUpdated.addInstructionsPreservingLabels(objIdx, block)
        } else {
            objUpdated.addInstructions(objIdx, block)
        }
        return objUpdated
    }

    if (preserveLabels) {
        updatedMethod.addInstructionsPreservingLabels(shiftedIdx, smali)
    } else {
        updatedMethod.addInstructions(shiftedIdx, smali)
    }
    return updatedMethod
}

private fun BytecodePatchContext.findAnchorDex(descriptors: List<String>): DexBackedDexFile? =
    descriptors.firstNotNullOfOrNull { type ->
        (classDefByOrNull(type) as? DexBackedClassDef)?.dexFile
    }

private fun classifyTargetDex(
    classDef: ClassDef,
    classes3Dex: DexBackedDexFile?,
    classes4Dex: DexBackedDexFile?,
): TargetDex {
    val dexFile = (classDef as? DexBackedClassDef)?.dexFile
    if (dexFile != null) {
        if (classes3Dex != null && dexFile === classes3Dex) return TargetDex.CLASSES3
        if (classes4Dex != null && dexFile === classes4Dex) return TargetDex.CLASSES4
        return TargetDex.OTHER
    }

    val type = classDef.type
    if (type.startsWith("Lnet/huanci/hsjpro/activities/") ||
        type.startsWith("Lnet/huanci/hsj/paint/") ||
        type == "Lnet/huanci/hsjpro/DrawApplication;" ||
        type == "Lnet/huanci/hsjpro/dialog/CommonDialog;" ||
        type == "Lnet/huanci/hsjpro/dialog/CommonDialog\$Builder;"
    ) {
        return TargetDex.CLASSES3
    }
    if (type.startsWith("Lnet/huanci/hsjpro/fragments/") ||
        type.startsWith("Lnet/huanci/hsjpro/views/") ||
        type.startsWith("Lnet/huanci/hsjpro/utils/") ||
        type.startsWith("Lnet/huanci/hsjpro/adapter/") ||
        type.startsWith("Lnet/huanci/hsjpro/model/") ||
        type.startsWith("Lnet/huanci/hsjpro/viewmodel/") ||
        type.startsWith("Lnet/huanci/hsjpro/paint/") ||
        type.startsWith("Lnet/huanci/paintlib/")
    ) {
        return TargetDex.CLASSES4
    }
    return TargetDex.OTHER
}

private fun methodParamRegister(method: MutableMethod, logicalParamIndex: Int): Int {
    val impl = requireNotNull(method.implementation) {
        "Method ${method.definingClass}->${method.name} has no implementation."
    }
    val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
    var paramRegs = if (isStatic) 0 else 1
    for (p in method.parameters) {
        val t = p.type
        paramRegs += if (t == "J" || t == "D") 2 else 1
    }
    var reg = impl.registerCount - paramRegs + if (isStatic) 0 else 1
    for (i in 0 until logicalParamIndex) {
        val t = method.parameters[i].type
        reg += if (t == "J" || t == "D") 2 else 1
    }
    return reg
}

private fun invokeOpcodeName(opcode: Opcode): String = when (opcode) {
    Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE -> "invoke-virtual"
    Opcode.INVOKE_SUPER, Opcode.INVOKE_SUPER_RANGE -> "invoke-super"
    Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE -> "invoke-direct"
    Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE -> "invoke-static"
    Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE -> "invoke-interface"
    else -> error("Unsupported invoke opcode: $opcode")
}

private fun formatInvokeWithRegs(
    opcode: Opcode,
    regs: IntArray,
    signature: String,
): String {
    val base = invokeOpcodeName(opcode)
    return if (regs.all { it in 0..15 } && opcode !in setOf(
            Opcode.INVOKE_VIRTUAL_RANGE,
            Opcode.INVOKE_SUPER_RANGE,
            Opcode.INVOKE_DIRECT_RANGE,
            Opcode.INVOKE_STATIC_RANGE,
            Opcode.INVOKE_INTERFACE_RANGE,
        )
    ) {
        val argStr = regs.joinToString(", ") { "v$it" }
        "$base {$argStr}, $signature"
    } else {
        check(regs.isNotEmpty()) { "Empty registers for range invoke" }
        "$base/range {v${regs.first()} .. v${regs.last()}}, $signature"
    }
}

private fun methodRefSignature(ref: MethodReference): String =
    "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"

private enum class CallSiteKind {
    TOAST_MAKE_CHARSEQ,
    TOAST_MAKE_RESID,
    SINGLE_ARG_RES_STRING,
    FORMATTED_GET_STRING,
}

private data class CallSiteMatch(
    val index: Int,
    val kind: CallSiteKind,
)

private fun classifyStringCallSite(
    inst: Instruction,
    instructions: List<Instruction>,
    idx: Int,
    isClasses4: Boolean,
    allowResourcesText: Boolean,
): CallSiteKind? {
    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    if (inst.opcode == Opcode.INVOKE_STATIC || inst.opcode == Opcode.INVOKE_STATIC_RANGE) {
        if (ref.definingClass == "Landroid/widget/Toast;" && ref.name == "makeText") {
            val params = ref.parameterTypes.map { it.toString() }
            if (params == listOf("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I")) {
                return CallSiteKind.TOAST_MAKE_CHARSEQ
            }
            if (!isClasses4 && params == listOf("Landroid/content/Context;", "I", "I")) {
                return CallSiteKind.TOAST_MAKE_RESID
            }
        }
        return null
    }
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) {
        return null
    }
    val defCls = ref.definingClass
    val params = ref.parameterTypes.map { it.toString() }
    val ret = ref.returnType
    if (params.size == 1 && params[0] == "I" && instructions.findMoveResultObjectAfter(idx) != null) {
        if (defCls == "Landroid/content/res/Resources;") {
            if ((ref.name == "getString" && ret == "Ljava/lang/String;") ||
                (ref.name == "getStringArray" && ret == "[Ljava/lang/String;") ||
                (allowResourcesText && ref.name == "getText" && ret == "Ljava/lang/CharSequence;")
            ) {
                return CallSiteKind.SINGLE_ARG_RES_STRING
            }
        } else if (defCls == "Landroid/content/Context;" ||
            defCls == "Landroid/app/Activity;" ||
            defCls == "Landroid/app/Application;" ||
            defCls == "Landroidx/fragment/app/Fragment;"
        ) {
            if (ref.name == "getString" && ret == "Ljava/lang/String;") {
                return CallSiteKind.SINGLE_ARG_RES_STRING
            }
        }
    }
    if (params == listOf("I", "[Ljava/lang/Object;") &&
        ref.name == "getString" &&
        ret == "Ljava/lang/String;" &&
        instructions.findMoveResultObjectAfter(idx) != null &&
        (defCls == "Landroid/app/Activity;" ||
            defCls == "Landroidx/fragment/app/Fragment;" ||
            defCls == "Landroid/content/Context;" ||
            defCls == "Landroid/app/Application;" ||
            defCls == "Landroid/content/res/Resources;")
    ) {
        return CallSiteKind.FORMATTED_GET_STRING
    }
    return null
}

private fun BytecodePatchContext.applySingleCallSitePatch(
    method: MutableMethod,
    idx: Int,
    kind: CallSiteKind,
    useDispatchOnly: Boolean,
): MutableMethod {
    val instrs = method.implementation!!.instructions
    val inst = instrs[idx]
    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return method
    val regs = invokeRegisterList(inst)

    when (kind) {
        CallSiteKind.TOAST_MAKE_CHARSEQ -> {
            val charSeqReg = regs.getOrNull(1) ?: return method
            if (!useDispatchOnly) {
                if (charSeqReg < 16) {
                    method.addInstructionsPreservingLabels(
                        idx,
                        """
                        invoke-static {v$charSeqReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$charSeqReg
                        """.trimIndent(),
                    )
                } else if (charSeqReg <= 255) {
                    method.addInstructionsPreservingLabels(
                        idx,
                        """
                        invoke-static/range {v$charSeqReg .. v$charSeqReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$charSeqReg
                        """.trimIndent(),
                    )
                }
                return method
            }
            val (m1, opReg, idx1) = runCatching {
                resolveLowRegisterAt(method, idx, *regs)
            }.getOrNull() ?: return method
            val curRegs1 = invokeRegisterList(m1.implementation!!.instructions[idx1])
            val curCharReg1 = curRegs1[1]
            if (curCharReg1 < 16 && m1.implementation!!.instructions[idx1] is FiveRegisterInstruction) {
                val (m2, tmpReg, idx2) = runCatching {
                    resolveLowRegisterAt(m1, idx1, opReg, *curRegs1)
                }.getOrNull() ?: return m1
                val curRegs2 = invokeRegisterList(m2.implementation!!.instructions[idx2])
                val curCharReg2 = curRegs2[1]
                val newRegs = curRegs2.clone()
                newRegs[1] = tmpReg
                val curInst = m2.implementation!!.instructions[idx2]
                m2.replaceInstruction(
                    idx2,
                    formatInvokeWithRegs(curInst.opcode, newRegs, methodRefSignature(ref)),
                )
                m2.addInstructionsPreservingLabels(
                    idx2,
                    """
                    const/4 v$opReg, $OP_ON_TOAST_TEXT
                    invoke-static {v$opReg, v$curCharReg2}, $DISPATCH_SIGNATURE
                    move-result-object v$tmpReg
                    check-cast v$tmpReg, Ljava/lang/CharSequence;
                    """.trimIndent(),
                )
                return m2
            } else if (curCharReg1 <= 255) {
                val (m2, tmpReg, idx2) = runCatching {
                    resolveLowRegisterAt(m1, idx1, opReg, *curRegs1)
                }.getOrNull() ?: return m1
                val curRegs2 = invokeRegisterList(m2.implementation!!.instructions[idx2])
                val curCharReg2 = curRegs2[1]
                m2.addInstructionsPreservingLabels(
                    idx2,
                    """
                    const/4 v$opReg, $OP_ON_TOAST_TEXT
                    move-object/from16 v$tmpReg, v$curCharReg2
                    invoke-static {v$opReg, v$tmpReg}, $DISPATCH_SIGNATURE
                    move-result-object v$curCharReg2
                    check-cast v$curCharReg2, Ljava/lang/CharSequence;
                    """.trimIndent(),
                )
                return m2
            }
            return m1
        }

        CallSiteKind.TOAST_MAKE_RESID -> {
            if (!useDispatchOnly) {
                val replacement = when (inst) {
                    is FiveRegisterInstruction ->
                        "invoke-static {v${inst.registerC}, v${inst.registerD}, v${inst.registerE}}, " +
                            "${TRANSLATION_ENGINE_CLASS}->makeTranslatedToast(Landroid/content/Context;II)Landroid/widget/Toast;"
                    is RegisterRangeInstruction -> {
                        val endReg = inst.startRegister + inst.registerCount - 1
                        "invoke-static/range {v${inst.startRegister} .. v$endReg}, " +
                            "${TRANSLATION_ENGINE_CLASS}->makeTranslatedToast(Landroid/content/Context;II)Landroid/widget/Toast;"
                    }
                    else -> null
                } ?: return method
                method.replaceInstruction(idx, replacement)
                return method
            }
            val ctxReg = regs.getOrNull(0) ?: return method
            val idReg = regs.getOrNull(1) ?: return method
            val (m1, resReg, idx1) = runCatching {
                resolveLowRegisterAt(method, idx, *regs)
            }.getOrNull() ?: return method
            val r1 = invokeRegisterList(m1.implementation!!.instructions[idx1])
            val c1 = r1[0]
            val i1 = r1[1]
            val d1 = r1[2]
            if (i1 !in 0..15) return m1
            val getResLine = if (c1 in 0..15) {
                "invoke-virtual {v$c1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;"
            } else {
                "invoke-virtual/range {v$c1 .. v$c1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;"
            }
            val replaceToast = if (c1 in 0..15 && resReg in 0..15 && d1 in 0..15) {
                "invoke-static {v$c1, v$resReg, v$d1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;"
            } else {
                return m1
            }
            m1.replaceInstruction(idx1, replaceToast)
            m1.addInstructionsPreservingLabels(
                idx1,
                """
                $getResLine
                move-result-object v$resReg
                invoke-static {v$i1, v$resReg}, $DISPATCH_SIGNATURE
                move-result-object v$resReg
                check-cast v$resReg, Ljava/lang/String;
                """.trimIndent(),
            )
            return m1
        }

        CallSiteKind.SINGLE_ARG_RES_STRING -> {
            val (moveIdx, outReg) = instrs.findMoveResultObjectAfter(idx) ?: return method
            val recvReg = regs.getOrNull(0) ?: return method
            val idReg = regs.getOrNull(1) ?: return method
            val castType = when {
                ref.name == "getStringArray" && ref.returnType == "[Ljava/lang/String;" -> "[Ljava/lang/String;"
                ref.name == "getText" && ref.returnType == "Ljava/lang/CharSequence;" -> "Ljava/lang/CharSequence;"
                else -> "Ljava/lang/String;"
            }

            if (ref.definingClass == "Landroid/content/res/Resources;" &&
                idReg == outReg && recvReg < 16 && idReg < 16 && outReg < 256
            ) {
                method.addInstructions(idx + 2, "check-cast v$outReg, $castType")
                method.replaceInstruction(idx, "invoke-static {v$idReg, v$recvReg}, $DISPATCH_SIGNATURE")
                return method
            }

            if (idReg != outReg && idReg < 16 && outReg < 16) {
                val bridgeSmali = if (!useDispatchOnly && castType == "Ljava/lang/String;") {
                    """
                    invoke-static {v$idReg, v$outReg}, ${TRANSLATION_ENGINE_CLASS}->translateById(ILjava/lang/String;)Ljava/lang/String;
                    move-result-object v$outReg
                    """.trimIndent()
                } else if (!useDispatchOnly && castType == "[Ljava/lang/String;") {
                    """
                    invoke-static {v$idReg, v$outReg}, ${TRANSLATION_ENGINE_CLASS}->translateArrayById(I[Ljava/lang/String;)[Ljava/lang/String;
                    move-result-object v$outReg
                    """.trimIndent()
                } else {
                    """
                    invoke-static {v$idReg, v$outReg}, $DISPATCH_SIGNATURE
                    move-result-object v$outReg
                    check-cast v$outReg, $castType
                    """.trimIndent()
                }
                method.addInstructions(moveIdx + 1, bridgeSmali)
                return method
            }

            if (idReg == outReg && outReg <= 255) {
                val (m1, tmpIdReg, idx1) = runCatching {
                    resolveLowRegisterAt(method, idx, *regs)
                }.getOrNull() ?: return method
                val r1 = invokeRegisterList(m1.implementation!!.instructions[idx1])
                val i1 = r1.getOrNull(1) ?: return m1
                m1.addInstructionsPreservingLabels(idx1, "move/from16 v$tmpIdReg, v$i1")
                val shiftedInstrs = m1.implementation!!.instructions
                val shiftedInvokeIdx = shiftedInstrs.indices.drop(idx1).firstOrNull { pos ->
                    val candidateRef = (shiftedInstrs[pos] as? ReferenceInstruction)?.reference as? MethodReference
                    candidateRef != null && candidateRef.name == ref.name && candidateRef.definingClass == ref.definingClass
                } ?: return m1
                val (mMoveIdx, mOutReg) = shiftedInstrs.findMoveResultObjectAfter(shiftedInvokeIdx) ?: return m1
                if (mOutReg < 16) {
                    m1.addInstructions(
                        mMoveIdx + 1,
                        """
                        invoke-static {v$tmpIdReg, v$mOutReg}, $DISPATCH_SIGNATURE
                        move-result-object v$mOutReg
                        check-cast v$mOutReg, $castType
                        """.trimIndent(),
                    )
                    return m1
                } else if (mOutReg <= 255) {
                    val (m2, tmpOutReg, postMoveIdx2) = runCatching {
                        resolveLowRegisterAt(m1, mMoveIdx + 1, tmpIdReg, mOutReg)
                    }.getOrNull() ?: return m1
                    m2.addInstructions(
                        postMoveIdx2,
                        """
                        move-object/from16 v$tmpOutReg, v$mOutReg
                        invoke-static {v$tmpIdReg, v$tmpOutReg}, $DISPATCH_SIGNATURE
                        move-result-object v$mOutReg
                        check-cast v$mOutReg, $castType
                        """.trimIndent(),
                    )
                    return m2
                }
                return m1
            }

            if (idReg != outReg && outReg <= 255) {
                var curM = method
                var curPostMove = moveIdx + 1
                var curId = idReg
                var curOut = outReg
                val lowId = if (curId < 16) curId else {
                    val triple = runCatching {
                        resolveLowRegisterAt(curM, curPostMove, curId, curOut)
                    }.getOrNull() ?: return curM
                    curM = triple.first
                    curPostMove = triple.third
                    triple.second
                }
                val lowOut = if (curOut < 16) curOut else {
                    val triple = runCatching {
                        resolveLowRegisterAt(curM, curPostMove, curId, curOut, lowId)
                    }.getOrNull() ?: return curM
                    curM = triple.first
                    curPostMove = triple.third
                    triple.second
                }
                val sb = StringBuilder()
                if (lowId != curId) sb.append("move/from16 v$lowId, v$curId\n")
                if (lowOut != curOut) sb.append("move-object/from16 v$lowOut, v$curOut\n")
                sb.append("invoke-static {v$lowId, v$lowOut}, $DISPATCH_SIGNATURE\n")
                sb.append("move-result-object v$curOut\n")
                sb.append("check-cast v$curOut, $castType")
                curM.addInstructionsPreservingLabels(curPostMove, sb.toString())
                return curM
            }
            return method
        }

        CallSiteKind.FORMATTED_GET_STRING -> {
            if (regs.size != 3) return method
            val recvReg = regs[0]
            val idReg = regs[1]
            val argsReg = regs[2]
            val (m1, resReg, idx1) = runCatching {
                resolveLowRegisterAt(method, idx, recvReg, idReg, argsReg)
            }.getOrNull() ?: return method
            val r1 = invokeRegisterList(m1.implementation!!.instructions[idx1])
            val c1 = r1[0]
            val i1 = r1[1]
            val a1 = r1[2]

            var curM = m1
            var curIdx = idx1
            var curRecv = c1
            var curId = i1
            var curArgs = a1
            val lowId = if (curId < 16) curId else {
                val t = runCatching {
                    resolveLowRegisterAt(curM, curIdx, resReg, curRecv, curId, curArgs)
                }.getOrNull() ?: return curM
                curM = t.first
                curIdx = t.third
                val updatedR = invokeRegisterList(curM.implementation!!.instructions[curIdx])
                curRecv = updatedR[0]
                curId = updatedR[1]
                curArgs = updatedR[2]
                t.second
            }
            val lowArgs = if (curArgs < 16) curArgs else {
                val t = runCatching {
                    resolveLowRegisterAt(curM, curIdx, resReg, curRecv, curId, curArgs, lowId)
                }.getOrNull() ?: return curM
                curM = t.first
                curIdx = t.third
                val updatedR = invokeRegisterList(curM.implementation!!.instructions[curIdx])
                curRecv = updatedR[0]
                curId = updatedR[1]
                curArgs = updatedR[2]
                t.second
            }

            val resFetchSmali = when (ref.definingClass) {
                "Landroid/content/res/Resources;" -> "move-object/from16 v$resReg, v$curRecv"
                "Landroidx/fragment/app/Fragment;" -> {
                    val inv = if (curRecv in 0..15) {
                        "invoke-virtual {v$curRecv}, Landroidx/fragment/app/Fragment;->getResources()Landroid/content/res/Resources;"
                    } else {
                        "invoke-virtual/range {v$curRecv .. v$curRecv}, Landroidx/fragment/app/Fragment;->getResources()Landroid/content/res/Resources;"
                    }
                    "$inv\nmove-result-object v$resReg"
                }
                "Landroid/app/Activity;" -> {
                    val inv = if (curRecv in 0..15) {
                        "invoke-virtual {v$curRecv}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;"
                    } else {
                        "invoke-virtual/range {v$curRecv .. v$curRecv}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;"
                    }
                    "$inv\nmove-result-object v$resReg"
                }
                else -> {
                    val inv = if (curRecv in 0..15) {
                        "invoke-virtual {v$curRecv}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;"
                    } else {
                        "invoke-virtual/range {v$curRecv .. v$curRecv}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;"
                    }
                    "$inv\nmove-result-object v$resReg"
                }
            }

            val prep = StringBuilder()
            prep.append(resFetchSmali).append('\n')
            if (lowId != curId) prep.append("move/from16 v$lowId, v$curId\n")
            if (lowArgs != curArgs) prep.append("move-object/from16 v$lowArgs, v$curArgs\n")
            prep.append("invoke-static {v$lowId, v$resReg}, $DISPATCH_SIGNATURE\n")
            prep.append("move-result-object v$resReg\n")
            prep.append("check-cast v$resReg, Ljava/lang/String;")

            curM.replaceInstruction(
                curIdx,
                "invoke-static {v$resReg, v$lowArgs}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
            )
            curM.addInstructionsPreservingLabels(curIdx, prep.toString())
            return curM
        }
    }
    return method
}

private fun BytecodePatchContext.hookHuanciPackageCallSites(
    classes3Dex: DexBackedDexFile?,
    classes4Dex: DexBackedDexFile?,
    psdHelperType: String,
) {
    val pendingClasses = mutableListOf<Pair<ClassDef, TargetDex>>()
    classDefForEach { classDef ->
        val isTargetPkg = classDef.type.startsWith("Lnet/huanci/") ||
            classDef.type.startsWith("Lcom/yalantis/ucrop/") ||
            classDef.type == psdHelperType
        if (!isTargetPkg) {
            return@classDefForEach
        }
        val targetDex = classifyTargetDex(classDef, classes3Dex, classes4Dex)
        if (targetDex == TargetDex.OTHER) {
            return@classDefForEach
        }
        val isClasses4 = (targetDex == TargetDex.CLASSES4)
        val hasCandidateMethod = classDef.methods.any { m ->
            val instrs = m.implementation?.instructions?.toList() ?: return@any false
            instrs.indices.any { idx ->
                classifyStringCallSite(instrs[idx], instrs, idx, isClasses4, allowResourcesText = false) != null
            }
        }
        if (hasCandidateMethod) {
            pendingClasses += classDef to targetDex
        }
    }

    for ((classDef, targetDex) in pendingClasses) {
        val isClasses4 = (targetDex == TargetDex.CLASSES4)
        val mutableClass = mutableClassDefBy(classDef)
        val candidateMethods = mutableClass.methods.filter { m ->
            val instrs = m.implementation?.instructions?.toList() ?: return@filter false
            instrs.indices.any { idx ->
                classifyStringCallSite(instrs[idx], instrs, idx, isClasses4, allowResourcesText = false) != null
            }
        }
        for (origMethod in candidateMethods) {
            var currentMethod = origMethod
            val initialInstrs = currentMethod.implementation!!.instructions.toList()
            val matchCount = initialInstrs.indices.count { idx ->
                classifyStringCallSite(initialInstrs[idx], initialInstrs, idx, isClasses4, allowResourcesText = false) != null
            }
            for (occ in matchCount - 1 downTo 0) {
                val instrs = currentMethod.implementation!!.instructions.toList()
                val matches = instrs.indices.mapNotNull { idx ->
                    val kind = classifyStringCallSite(instrs[idx], instrs, idx, isClasses4, allowResourcesText = false)
                    if (kind != null) CallSiteMatch(idx, kind) else null
                }
                if (occ >= matches.size) continue
                val m = matches[occ]
                currentMethod = applySingleCallSitePatch(
                    method = currentMethod,
                    idx = m.index,
                    kind = m.kind,
                    useDispatchOnly = isClasses4,
                )
            }
        }
    }
}

private fun BytecodePatchContext.hookToastHelper() {
    val toastHelperClass = allClasses().firstOrNull {
        it.type == "Lcn/painterclub/painttools/utils/ToastHelper;"
    } ?: return
    val mutableClass = mutableClassDefBy(toastHelperClass)
    for (method in mutableClass.methods.toList()) {
        if (!AccessFlags.STATIC.isSet(method.accessFlags)) continue
        val instrs = method.implementation?.instructions?.toList() ?: continue
        val params = method.parameters.map { it.type }
        if (params.isNotEmpty() && params[0] == "I") {
            val matchIndices = instrs.indices.filter { idx ->
                val inst = instrs[idx]
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroid/app/Application;" &&
                    ref.name == "getString" &&
                    ref.parameterTypes.size == 1 &&
                    ref.parameterTypes[0].toString() == "I" &&
                    instrs.findMoveResultObjectAfter(idx) != null
            }
            if (matchIndices.isNotEmpty()) {
                var curMethod = method
                for (occ in matchIndices.size - 1 downTo 0) {
                    val curInstrs = curMethod.implementation!!.instructions.toList()
                    val curMatches = curInstrs.indices.filter { idx ->
                        val ref = (curInstrs[idx] as? ReferenceInstruction)?.reference as? MethodReference
                        ref != null &&
                            ref.definingClass == "Landroid/app/Application;" &&
                            ref.name == "getString" &&
                            ref.parameterTypes.size == 1 &&
                            ref.parameterTypes[0].toString() == "I" &&
                            curInstrs.findMoveResultObjectAfter(idx) != null
                    }
                    if (occ >= curMatches.size) continue
                    val idx = curMatches[occ]
                    val regs = invokeRegisterList(curInstrs[idx])
                    val (updated, tmpIdReg, shiftedIdx) = resolveLowRegisterAt(curMethod, idx, *regs)
                    val sRegs = invokeRegisterList(updated.implementation!!.instructions[shiftedIdx])
                    val idReg = sRegs[1]
                    updated.addInstructionsPreservingLabels(shiftedIdx, "move/from16 v$tmpIdReg, v$idReg")
                    val afterInstrs = updated.implementation!!.instructions
                    val shiftedInvokeIdx = afterInstrs.indices.drop(shiftedIdx).first { pos ->
                        val ref = (afterInstrs[pos] as? ReferenceInstruction)?.reference as? MethodReference
                        ref != null && ref.definingClass == "Landroid/app/Application;" && ref.name == "getString"
                    }
                    val (moveIdx, outReg) = afterInstrs.findMoveResultObjectAfter(shiftedInvokeIdx)!!
                    if (outReg < 16) {
                        updated.addInstructions(
                            moveIdx + 1,
                            """
                            invoke-static {v$tmpIdReg, v$outReg}, ${TRANSLATION_ENGINE_CLASS}->translateById(ILjava/lang/String;)Ljava/lang/String;
                            move-result-object v$outReg
                            """.trimIndent(),
                        )
                    }
                    curMethod = updated
                }
            }
        } else if (params.isNotEmpty() && params[0] == "Ljava/lang/String;") {
            val callsToasterShow = instrs.any { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null && ref.definingClass == "Lcom/hjq/toast/Toaster;" && ref.name == "show"
            }
            if (callsToasterShow) {
                val strReg = methodParamRegister(method, 0)
                if (strReg <= 255) {
                    val invokeLine = if (strReg < 16) {
                        "invoke-static {v$strReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;"
                    } else {
                        "invoke-static/range {v$strReg .. v$strReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;"
                    }
                    method.addInstructions(
                        0,
                        """
                        $invokeLine
                        move-result-object v$strReg
                        check-cast v$strReg, Ljava/lang/String;
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}

private fun BytecodePatchContext.hookClasses2ToastsAndDrawables() {
    val decoderCompat = allClasses().firstOrNull {
        it.type == "Lcom/bumptech/glide/load/resource/drawable/DrawableDecoderCompat;"
    }
    if (decoderCompat != null) {
        val mutableDecoder = mutableClassDefBy(decoderCompat)
        val getDrawableMethod = mutableDecoder.methods.firstOrNull { m ->
            m.name == "getDrawable" &&
                m.parameters.map { it.type } == listOf(
                    "Landroid/content/Context;",
                    "Landroid/content/Context;",
                    "I",
                    "Landroid/content/res/Resources\$Theme;",
                ) &&
                m.implementation != null
        }
        if (getDrawableMethod != null) {
            val idReg = methodParamRegister(getDrawableMethod, 2)
            if (idReg in 0..15) {
                val instrs = getDrawableMethod.implementation!!.instructions
                val retIndices = instrs.indices.filter { instrs[it].opcode == Opcode.RETURN_OBJECT }.reversed()
                for (retIdx in retIndices) {
                    val retReg = (getDrawableMethod.getInstruction<OneRegisterInstruction>(retIdx)).registerA
                    if (retReg in 0..15) {
                        getDrawableMethod.addInstructionsPreservingLabels(
                            retIdx,
                            """
                            invoke-static {v$idReg, v$retReg}, $DISPATCH_SIGNATURE
                            move-result-object v$retReg
                            check-cast v$retReg, Landroid/graphics/drawable/Drawable;
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }

    val toasterClass = allClasses().firstOrNull { it.type == "Lcom/hjq/toast/Toaster;" }
    if (toasterClass != null) {
        val mutableToaster = mutableClassDefBy(toasterClass)
        val showMethod = mutableToaster.methods.firstOrNull { m ->
            m.name == "show" &&
                m.parameters.map { it.type } == listOf("Ljava/lang/CharSequence;") &&
                m.implementation != null
        }
        if (showMethod != null) {
            val textReg = methodParamRegister(showMethod, 0)
            val (updated, opReg, _) = resolveLowRegisterAt(showMethod, 0, textReg)
            val shiftedTextReg = methodParamRegister(updated, 0)
            if (shiftedTextReg in 0..15) {
                updated.addInstructions(
                    0,
                    """
                    const/4 v$opReg, $OP_ON_TOAST_TEXT
                    invoke-static {v$opReg, v$shiftedTextReg}, $DISPATCH_SIGNATURE
                    move-result-object v$shiftedTextReg
                    check-cast v$shiftedTextReg, Ljava/lang/CharSequence;
                    """.trimIndent(),
                )
            }
        }

        val strIdMethod = mutableToaster.methods.firstOrNull { m ->
            m.name == "stringIdToCharSequence" &&
                m.parameters.map { it.type } == listOf("I") &&
                m.implementation != null
        }
        if (strIdMethod != null) {
            val instrs = strIdMethod.implementation!!.instructions.toList()
            val getTextIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null && ref.definingClass == "Landroid/content/res/Resources;" && ref.name == "getText"
            }
            check(getTextIdx >= 0) {
                "Resources.getText not found in Toaster.stringIdToCharSequence"
            }
            val moveResult = instrs.findMoveResultObjectAfter(getTextIdx)
            check(moveResult != null) {
                "MOVE_RESULT_OBJECT after Resources.getText not found in Toaster.stringIdToCharSequence"
            }
            val (moveIdx, origOutReg) = moveResult
            val retIdx = instrs.findNextOpcode(moveIdx + 1, Opcode.RETURN_OBJECT, maxLookahead = 6)
            check(retIdx > moveIdx) {
                "RETURN_OBJECT not found after MOVE_RESULT_OBJECT in Toaster.stringIdToCharSequence"
            }
            val retReg = (instrs[retIdx] as? OneRegisterInstruction)?.registerA ?: -1
            check(retReg == origOutReg) {
                "Expected RETURN_OBJECT register v$origOutReg, got v$retReg in Toaster.stringIdToCharSequence"
            }
            val regs = invokeRegisterList(instrs[getTextIdx])
            val resReg = regs.getOrNull(0) ?: -1
            val idReg = regs.getOrNull(1) ?: -1
            check(resReg in 0..15 && idReg in 0..15 && resReg != idReg) {
                "Unexpected registers for Resources.getText in Toaster.stringIdToCharSequence: res=v$resReg, id=v$idReg"
            }
            val intervening = instrs.subList(moveIdx + 1, retIdx)
            check(intervening.none { inst ->
                inst.opcode == Opcode.THROW ||
                    inst.opcode.name.startsWith("RETURN") ||
                    (inst.opcode.setsRegister() && (inst as? OneRegisterInstruction)?.registerA.let { reg ->
                        reg == resReg || reg == idReg || reg == origOutReg
                    })
            }) {
                "Unexpected register mutation or control transfer between MOVE_RESULT_OBJECT and RETURN_OBJECT in Toaster.stringIdToCharSequence"
            }
            // Use resReg (dead after Resources.getText) so idReg stays Integer across the try/catch block.
            strIdMethod.replaceInstruction(moveIdx, "move-result-object v$resReg")
            strIdMethod.replaceInstruction(retIdx, "return-object v$resReg")
            strIdMethod.addInstructionsPreservingLabels(
                retIdx,
                """
                invoke-static {v$idReg, v$resReg}, $DISPATCH_SIGNATURE
                move-result-object v$resReg
                check-cast v$resReg, Ljava/lang/CharSequence;
                """.trimIndent(),
            )
        }
    }

    val pickerClasses = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        if (classDef.type.startsWith("Lcom/chs/filepicker/")) {
            pickerClasses += classDef
        }
    }
    for (classDef in pickerClasses) {
        val mutableClass = mutableClassDefBy(classDef)
        for (origMethod in mutableClass.methods.toList()) {
            val initialInstrs = origMethod.implementation?.instructions?.toList() ?: continue
            val matchCount = initialInstrs.indices.count { idx ->
                val kind = classifyStringCallSite(initialInstrs[idx], initialInstrs, idx, isClasses4 = false, allowResourcesText = false)
                kind == CallSiteKind.TOAST_MAKE_CHARSEQ ||
                    kind == CallSiteKind.TOAST_MAKE_RESID ||
                    kind == CallSiteKind.SINGLE_ARG_RES_STRING
            }
            if (matchCount == 0) continue
            var curMethod = origMethod
            for (occ in matchCount - 1 downTo 0) {
                val instrs = curMethod.implementation!!.instructions.toList()
                val matches = instrs.indices.mapNotNull { idx ->
                    val kind = classifyStringCallSite(instrs[idx], instrs, idx, isClasses4 = false, allowResourcesText = false)
                    if (kind == CallSiteKind.TOAST_MAKE_CHARSEQ ||
                        kind == CallSiteKind.TOAST_MAKE_RESID ||
                        kind == CallSiteKind.SINGLE_ARG_RES_STRING
                    ) {
                        CallSiteMatch(idx, kind)
                    } else {
                        null
                    }
                }
                if (occ >= matches.size) continue
                val m = matches[occ]
                curMethod = applySingleCallSitePatch(curMethod, m.index, m.kind, useDispatchOnly = true)
            }
        }
    }
}

private fun BytecodePatchContext.hookRxToastAndDraftAdapters() {
    val rxToastClass = allClasses().firstOrNull {
        it.sourceFile == "RxToast.java" && it.methods.any { m ->
            AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.returnType == "Landroid/widget/Toast;" &&
                m.parameters.size >= 2 &&
                m.parameters[0].type == "Landroid/content/Context;" &&
                m.parameters[1].type == "Ljava/lang/String;"
        }
    }
    if (rxToastClass != null) {
        val mutableRx = mutableClassDefBy(rxToastClass)
        for (method in mutableRx.methods.toList()) {
            if (AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.returnType == "Landroid/widget/Toast;" &&
                method.parameters.size >= 2 &&
                method.parameters[0].type == "Landroid/content/Context;" &&
                method.parameters[1].type == "Ljava/lang/String;" &&
                method.implementation != null
            ) {
                val strReg = methodParamRegister(method, 1)
                if (strReg <= 255) {
                    val invokeLine = if (strReg < 16) {
                        "invoke-static {v$strReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;"
                    } else {
                        "invoke-static/range {v$strReg .. v$strReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;"
                    }
                    method.addInstructions(
                        0,
                        """
                        $invokeLine
                        move-result-object v$strReg
                        check-cast v$strReg, Ljava/lang/String;
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    for (adapterType in listOf(
        "Lnet/huanci/hsjpro/adapter/DraftAdapter;",
        "Lnet/huanci/hsjpro/adapter/DraftAdapter2;",
    )) {
        val adapterDef = allClasses().firstOrNull { it.type == adapterType } ?: continue
        val mutableAdapter = mutableClassDefBy(adapterDef)
        val bindMethods = mutableAdapter.methods.filter { m ->
            m.name == "onBindViewHolder" && m.implementation != null
        }
        for (origMethod in bindMethods) {
            var curMethod = origMethod
            val initialInstrs = curMethod.implementation!!.instructions.toList()
            val matchCount = initialInstrs.count { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Lcom/bumptech/glide/RequestManager;" &&
                    ref.name == "load" &&
                    ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")
            }
            for (occ in matchCount - 1 downTo 0) {
                val instrs = curMethod.implementation!!.instructions.toList()
                val matches = instrs.indices.filter { idx ->
                    val ref = (instrs[idx] as? ReferenceInstruction)?.reference as? MethodReference
                    ref != null &&
                        ref.definingClass == "Lcom/bumptech/glide/RequestManager;" &&
                        ref.name == "load" &&
                        ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")
                }
                if (occ >= matches.size) continue
                val loadIdx = matches[occ]
                val regs = invokeRegisterList(instrs[loadIdx])
                if (regs.size != 2) continue
                val (updated, scratchReg, shiftedIdx) = runCatching {
                    resolveLowRegisterAt(curMethod, loadIdx, *regs)
                }.getOrNull() ?: continue
                val sRegs = invokeRegisterList(updated.implementation!!.instructions[shiftedIdx])
                val mgrReg = sRegs[0]
                val urlReg = sRegs[1]
                if (mgrReg !in 0..15 || scratchReg !in 0..15) continue
                val invokeResolve = if (urlReg in 0..15) {
                    "invoke-static {v$urlReg}, ${TRANSLATION_ENGINE_CLASS}->resolveLocalizedDraftModel(Ljava/lang/Object;)Ljava/lang/Object;"
                } else {
                    "invoke-static/range {v$urlReg .. v$urlReg}, ${TRANSLATION_ENGINE_CLASS}->resolveLocalizedDraftModel(Ljava/lang/Object;)Ljava/lang/Object;"
                }
                updated.replaceInstruction(
                    shiftedIdx,
                    "invoke-virtual {v$mgrReg, v$scratchReg}, Lcom/bumptech/glide/RequestManager;->load(Ljava/lang/Object;)Lcom/bumptech/glide/RequestBuilder;",
                )
                updated.addInstructionsPreservingLabels(
                    shiftedIdx,
                    """
                    $invokeResolve
                    move-result-object v$scratchReg
                    """.trimIndent(),
                )
                curMethod = updated
            }
        }
    }
}

private fun BytecodePatchContext.hookD3ArrowBitmaps() {
    for (viewType in listOf(
        "Lnet/huanci/hsjpro/paint/views/D3MoveView;",
        "Lnet/huanci/hsjpro/paint/views/D3RotateView;",
    )) {
        val viewDef = allClasses().firstOrNull { it.type == viewType } ?: continue
        val mutableView = mutableClassDefBy(viewDef)

        val onDrawMethod = mutableView.methods.firstOrNull {
            it.name == "onDraw" &&
                it.parameters.map { p -> p.type } == listOf("Landroid/graphics/Canvas;") &&
                it.implementation != null
        }

        val showMethod = mutableView.methods.firstOrNull {
            it.name == "show" && it.parameters.isEmpty() && it.returnType == "V" && it.implementation != null
        }
        if (showMethod != null) {
            val origThis = showMethod.thisRegisterIndex()
            val (updated, nullReg, _) = resolveLowRegisterAt(showMethod, 0, origThis)
            val curThis = updated.thisRegisterIndex()
            val namedFields = listOf("lrBitmap", "udBitmap", "fbBitmap").filter { fname ->
                mutableView.fields.any {
                    !AccessFlags.STATIC.isSet(it.accessFlags) &&
                        it.name == fname &&
                        it.type == "Landroid/graphics/Bitmap;"
                }
            }
            val clearFields = if (namedFields.isNotEmpty()) {
                namedFields
            } else {
                val instanceBitmapFields = mutableView.fields
                    .filter {
                        !AccessFlags.STATIC.isSet(it.accessFlags) &&
                            it.type == "Landroid/graphics/Bitmap;"
                    }
                    .map { it.name }
                    .toSet()
                onDrawMethod?.implementation?.instructions
                    ?.mapNotNull { inst ->
                        if (inst.opcode != Opcode.IGET_OBJECT && inst.opcode != Opcode.IPUT_OBJECT) {
                            return@mapNotNull null
                        }
                        val ref = (inst as? ReferenceInstruction)?.reference as? FieldReference
                            ?: return@mapNotNull null
                        if (ref.definingClass == viewType &&
                            ref.type == "Landroid/graphics/Bitmap;" &&
                            ref.name in instanceBitmapFields
                        ) {
                            ref.name
                        } else {
                            null
                        }
                    }
                    ?.distinct()
                    .orEmpty()
            }
            check(clearFields.isNotEmpty()) {
                "No Bitmap fields resolved to clear in $viewType->show()V"
            }
            if (curThis in 0..15 && nullReg in 0..15) {
                val sb = StringBuilder("const/4 v$nullReg, 0\n")
                for (fname in clearFields) {
                    sb.append("iput-object v$nullReg, v$curThis, $viewType->$fname:Landroid/graphics/Bitmap;\n")
                }
                updated.addInstructions(0, sb.toString().trimEnd())
            }
        }

        var curMethod: MutableMethod = onDrawMethod ?: continue
        val initialInstrs = curMethod.implementation!!.instructions.toList()
        val matchCount = initialInstrs.indices.count { idx ->
            val ref = (initialInstrs[idx] as? ReferenceInstruction)?.reference as? MethodReference
            ref != null &&
                ref.definingClass == "Landroid/graphics/BitmapFactory;" &&
                ref.name == "decodeResource" &&
                ref.parameterTypes.map { it.toString() } == listOf("Landroid/content/res/Resources;", "I") &&
                initialInstrs.findMoveResultObjectAfter(idx) != null
        }
        for (occ in matchCount - 1 downTo 0) {
            val instrs = curMethod.implementation!!.instructions.toList()
            val matches = instrs.indices.filter { idx ->
                val ref = (instrs[idx] as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroid/graphics/BitmapFactory;" &&
                    ref.name == "decodeResource" &&
                    ref.parameterTypes.map { it.toString() } == listOf("Landroid/content/res/Resources;", "I") &&
                    instrs.findMoveResultObjectAfter(idx) != null
            }
            if (occ >= matches.size) continue
            val decIdx = matches[occ]
            val regs = invokeRegisterList(instrs[decIdx])
            if (regs.size != 2) continue
            val idReg = regs[1]
            val (moveIdx, outReg) = instrs.findMoveResultObjectAfter(decIdx)!!
            if (idReg != outReg && idReg < 16 && outReg < 16) {
                curMethod.addInstructions(
                    moveIdx + 1,
                    """
                    invoke-static {v$idReg, v$outReg}, $DISPATCH_SIGNATURE
                    move-result-object v$outReg
                    check-cast v$outReg, Landroid/graphics/Bitmap;
                    """.trimIndent(),
                )
            } else {
                val (updated, tmpIdReg, shiftedIdx) = runCatching {
                    resolveLowRegisterAt(curMethod, decIdx, *regs)
                }.getOrNull() ?: continue
                val sRegs = invokeRegisterList(updated.implementation!!.instructions[shiftedIdx])
                updated.addInstructionsPreservingLabels(shiftedIdx, "move/from16 v$tmpIdReg, v${sRegs[1]}")
                val afterInstrs = updated.implementation!!.instructions
                val shiftedDec = afterInstrs.indices.drop(shiftedIdx).first { pos ->
                    val ref = (afterInstrs[pos] as? ReferenceInstruction)?.reference as? MethodReference
                    ref != null && ref.definingClass == "Landroid/graphics/BitmapFactory;" && ref.name == "decodeResource"
                }
                val (mIdx, mOut) = afterInstrs.findMoveResultObjectAfter(shiftedDec)!!
                if (mOut < 16) {
                    updated.addInstructions(
                        mIdx + 1,
                        """
                        invoke-static {v$tmpIdReg, v$mOut}, $DISPATCH_SIGNATURE
                        move-result-object v$mOut
                        check-cast v$mOut, Landroid/graphics/Bitmap;
                        """.trimIndent(),
                    )
                }
                curMethod = updated
            }
        }
    }
}

@Suppress("unused")
val dynamicTranslationPatch = bytecodePatch(
    name = "Dynamic External Translation Engine",
    description = "Translates UI strings at runtime from bundled or external JSON packs while keeping filesystem folder names untranslated.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch, languagePackResourcesPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        val classes = allClasses()
        val assetsUtilClass = MultiVersionResolvers.resolveAssetsUtilClass(classes)
        val stringFogClass = MultiVersionResolvers.resolveStringFogClass(classes)
        val languageLocaleUtilsClass = MultiVersionResolvers.resolveLanguageLocaleUtilsClass(classes)
        val applyLocaleMethod = MultiVersionResolvers.resolveLanguageLocaleApply(languageLocaleUtilsClass)
        val bubblePopupClass = MultiVersionResolvers.resolveBubblePopupClass(classes)
        val paintActivityClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        val mainFragment2Class = MultiVersionResolvers.resolveMainFragment2Class(classes)
        val baseFragmentHookRef = MultiVersionResolvers.resolveBaseFragmentHookTarget(classes)

        // Capture unmutated DEX file identities before editing any target class.
        val classes3Dex = findAnchorDex(
            listOf(
                "Lnet/huanci/hsjpro/DrawApplication;",
                "Lnet/huanci/hsjpro/activities/BaseActivity;",
                "Lnet/huanci/hsjpro/activities/PaintActivity;",
                stringFogClass.type,
            ),
        )
        val classes4Dex = findAnchorDex(
            listOf(
                "Lnet/huanci/hsjpro/fragments/BaseDialogFragment;",
                "Lnet/huanci/hsjpro/paint/views/SelectPenView;",
                languageLocaleUtilsClass.type,
                "Lnet/huanci/hsjpro/fragments/BaseFragment;",
            ),
        )

        // 1. Register runtime toggles in General Settings before injectGeneralSettings runs.
        registerToggleBeforeSettingsPanel("libre_dynamic_i18n")
        registerToggleBeforeSettingsPanel("libre_lang_file")

        // 2. classes.dex hooks: AssetsUtil string and array translation.
        hookAssetsUtilBridge(
            toMutableMethod(MultiVersionResolvers.resolveAssetsUtilGetString(assetsUtilClass)),
            "${TRANSLATION_ENGINE_CLASS}->translateById(ILjava/lang/String;)Ljava/lang/String;",
        )
        hookAssetsUtilBridge(
            toMutableMethod(MultiVersionResolvers.resolveAssetsUtilGetStringArray(assetsUtilClass)),
            "${TRANSLATION_ENGINE_CLASS}->translateArrayById(I[Ljava/lang/String;)[Ljava/lang/String;",
        )

        // 3. classes3.dex hooks: StringFog, Activities, CommonDialog, and PaintActivity popups.
        run {
            val stringFogMethod = toMutableMethod(MultiVersionResolvers.resolveStringFogDecrypt(stringFogClass))
            val instrs = stringFogMethod.implementation!!.instructions
            val retIndices = instrs.indices
                .filter { instrs[it].opcode == Opcode.RETURN_OBJECT }
                .reversed()
            check(retIndices.isNotEmpty()) { "RETURN_OBJECT not found in StringFog.decrypt" }
            for (retIdx in retIndices) {
                val retReg = stringFogMethod.getInstruction<OneRegisterInstruction>(retIdx).registerA
                val invokeLine = formatInvokeStaticSingle(
                    "${TRANSLATION_ENGINE_CLASS}->translateStringFog(Ljava/lang/String;)Ljava/lang/String;",
                    retReg,
                )
                stringFogMethod.addInstructionsPreservingLabels(
                    retIdx,
                    """
                    $invokeLine
                    move-result-object v$retReg
                    """.trimIndent(),
                )
            }
        }

        hookBaseActivityLifecycle(baseActivityOnCreateFingerprint)
        hookBaseActivityLifecycle(baseActivityOnResumeFingerprint)

        for (fp in listOf(commonDialogShowFingerprint, commonDialogOnStartFingerprint)) {
            val m = fp.method
            m.addInstructions(
                0,
                formatInvokeStaticSingle(
                    "${TRANSLATION_ENGINE_CLASS}->onDialogPreShow(Landroid/app/Dialog;)V",
                    m.thisRegisterIndex(),
                ),
            )
        }

        run {
            val builderMethod = toMutableMethod(MultiVersionResolvers.resolveCommonDialogBuilderCreate(classes))
            val instrs = builderMethod.implementation!!.instructions
            val setViewIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroidx/appcompat/app/AlertDialog;" &&
                    ref.name == "setView" &&
                    ref.parameterTypes.size == 1 &&
                    ref.parameterTypes[0].toString() == "Landroid/view/View;"
            }
            check(setViewIdx >= 0) { "AlertDialog.setView not found in CommonDialog\$Builder.create" }
            val viewReg = invokeArgRegister(instrs[setViewIdx], 1)
                ?: error("Could not resolve AlertDialog.setView register")
            builderMethod.addInstructions(
                setViewIdx + 1,
                """
                ${formatInvokeStaticSingle("${TRANSLATION_ENGINE_CLASS}->translateViewHierarchy(Landroid/view/View;)V", viewReg)}
                ${formatInvokeStaticSingle("${TRANSLATION_ENGINE_CLASS}->attachViewTreeObserver(Landroid/view/View;)V", viewReg)}
                """.trimIndent(),
            )
        }

        hookPaintPopupShadowCast(
            toMutableMethod(MultiVersionResolvers.resolvePaintPopup1Menu(paintActivityClass, bubblePopupClass.type)),
        )
        hookPaintPopupShadowCast(
            toMutableMethod(MultiVersionResolvers.resolvePaintPopup2Brush(paintActivityClass, bubblePopupClass.type)),
        )
        hookPaintPopupShadowCast(
            toMutableMethod(MultiVersionResolvers.resolvePaintPopup3Canvas(paintActivityClass, bubblePopupClass.type)),
        )
        hookPaintPopupShadowCast(
            toMutableMethod(MultiVersionResolvers.resolvePaintPopup4Ref(paintActivityClass, bubblePopupClass.type)),
        )

        hookActivityInitViewWindow(replayActivityInitViewFingerprint, translateFirst = true)
        hookActivityInitViewWindow(brushSettingInitViewFingerprint, translateFirst = false)
        hookActivityInitViewWindow(uiBrushSettingInitViewFingerprint, translateFirst = false)

        // 4. classes4.dex hooks: strictly routed through TranslationEngine.dispatch(int, Object).
        toMutableMethod(applyLocaleMethod).addInstruction(0, "return-void")

        run {
            val method = baseFragmentOnCreateViewFingerprint.method
            val instrs = method.implementation!!.instructions
            val hookIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == baseFragmentHookRef.definingClass &&
                    ref.name == baseFragmentHookRef.name &&
                    ref.parameterTypes.size == 1 &&
                    ref.parameterTypes[0].toString() == "Landroid/view/View;"
            }
            check(hookIdx >= 0) { "BaseFragment(View) hook target not found in onCreateView" }
            val viewReg = invokeArgRegister(instrs[hookIdx], 1)
                ?: error("Could not resolve BaseFragment(View) View register")
            emitDispatchConstantAt(
                method = method,
                insertIndex = hookIdx + 1,
                opCode = OP_ATTACH_OBSERVER,
                argReg = viewReg,
                preserveLabels = false,
            )
        }

        run {
            val method = toMutableMethod(MultiVersionResolvers.resolveMainFragment2Inflate(mainFragment2Class))
            val instrs = method.implementation!!.instructions
            val inflateIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroid/view/LayoutInflater;" &&
                    ref.name == "inflate" &&
                    ref.parameterTypes.size == 2
            }
            val (moveIdx, viewReg) = checkNotNull(instrs.findMoveResultObjectAfter(inflateIdx)) {
                "Expected move-result-object after inflate in MainFragment2.inflate"
            }
            emitDispatchConstantAt(
                method = method,
                insertIndex = moveIdx + 1,
                opCode = OP_TRANSLATE_VIEW,
                argReg = viewReg,
                preserveLabels = false,
            )
        }

        run {
            val method = baseDialogFragmentOnStartFingerprint.method
            val instrs = method.implementation!!.instructions
            val getDialogIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroidx/fragment/app/DialogFragment;" &&
                    ref.name == "getDialog"
            }
            check(getDialogIdx >= 0) {
                "DialogFragment.getDialog pattern not found in BaseDialogFragment.onStart"
            }
            val (moveIdx, dialogReg) = checkNotNull(instrs.findMoveResultObjectAfter(getDialogIdx)) {
                "Expected move-result-object after getDialog in BaseDialogFragment.onStart"
            }
            val branchIdx = (moveIdx + 1 until minOf(instrs.size, moveIdx + 5)).firstOrNull { i ->
                val op = instrs[i].opcode
                op == Opcode.IF_NEZ || op == Opcode.IF_EQZ
            } ?: error("Expected null check after getDialog in BaseDialogFragment.onStart")

            // Guard directly before the branch so dispatch(4, dialog) runs only on non-null Dialog.
            val (updated, scratchReg, shiftedBranchIdx) = resolveLowRegisterAt(
                method = method,
                insertIndex = branchIdx,
                dialogReg,
            )

            if (dialogReg in 0..15) {
                updated.addInstructionsWithLabels(
                    shiftedBranchIdx,
                    """
                    if-eqz v$dialogReg, :libre_skip_dialog_preshow
                    const/4 v$scratchReg, $OP_DIALOG_PRE_SHOW
                    ${formatInvokeStatic2(DISPATCH_SIGNATURE, scratchReg, dialogReg)}
                    """.trimIndent(),
                    ExternalLabel("libre_skip_dialog_preshow", updated.getInstruction(shiftedBranchIdx)),
                )
            } else {
                val (objUpdated, objReg, objBranchIdx) = resolveLowRegisterAt(
                    method = updated,
                    insertIndex = shiftedBranchIdx,
                    scratchReg,
                    dialogReg,
                )
                objUpdated.addInstructionsWithLabels(
                    objBranchIdx,
                    """
                    if-eqz v$dialogReg, :libre_skip_dialog_preshow
                    const/4 v$scratchReg, $OP_DIALOG_PRE_SHOW
                    move-object/from16 v$objReg, v$dialogReg
                    ${formatInvokeStatic2(DISPATCH_SIGNATURE, scratchReg, objReg)}
                    """.trimIndent(),
                    ExternalLabel("libre_skip_dialog_preshow", objUpdated.getInstruction(objBranchIdx)),
                )
            }
        }

        run {
            val initMethod = toMutableMethod(MultiVersionResolvers.resolveBubblePopupInit(bubblePopupClass))
            val instrs = initMethod.implementation!!.instructions
            val superInitIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.definingClass == "Landroid/widget/PopupWindow;" &&
                    ref.name == "<init>"
            }
            check(superInitIdx >= 0) { "PopupWindow.<init> not found in BubblePopupWindow" }
            val viewReg = invokeArgRegister(instrs[superInitIdx], 1)
                ?: error("Could not resolve PopupWindow.<init> View register")
            emitDispatchConstantAt(
                method = initMethod,
                insertIndex = superInitIdx + 1,
                opCode = OP_TRANSLATE_AND_OBSERVE,
                argReg = viewReg,
                preserveLabels = false,
            )
        }

        run {
            val showMethod = toMutableMethod(
                MultiVersionResolvers.resolveBubblePopupShowAtLocationCustom(bubblePopupClass),
            )
            val instrs = showMethod.implementation!!.instructions
            for (idx in instrs.indices) {
                val ref = (instrs[idx] as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == applyLocaleMethod.definingClass &&
                    ref.name == applyLocaleMethod.name &&
                    ref.parameterTypes.size == applyLocaleMethod.parameterTypes.size
                ) {
                    showMethod.replaceInstruction(idx, "nop")
                }
            }
            val updatedInstrs = showMethod.implementation!!.instructions
            val getContentIdx = updatedInstrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.name == "getContentView" &&
                    ref.returnType == "Landroid/view/View;" &&
                    ref.parameterTypes.isEmpty()
            }
            val (moveIdx, contentViewReg) = checkNotNull(updatedInstrs.findMoveResultObjectAfter(getContentIdx)) {
                "Expected move-result-object after getContentView in BubblePopupWindow.showAtLocationCustom"
            }
            emitDispatchConstantAt(
                method = showMethod,
                insertIndex = moveIdx + 1,
                opCode = OP_TRANSLATE_AND_OBSERVE,
                argReg = contentViewReg,
                preserveLabels = false,
            )
        }

        for (fp in listOf(
            exportPictureViewOnCreateViewFingerprint,
            exportPictureView3OnCreateViewFingerprint,
        )) {
            var method = fp.method
            val initialInstrs = method.implementation!!.instructions
            val retIndices = initialInstrs.indices
                .filter { initialInstrs[it].opcode == Opcode.RETURN_OBJECT }
                .reversed()
            check(retIndices.isNotEmpty()) {
                "RETURN_OBJECT not found in ${method.definingClass}->onCreateView"
            }
            for (retIdx in retIndices) {
                val retReg = method.getInstruction<OneRegisterInstruction>(retIdx).registerA
                method = emitDispatchConstantAt(
                    method = method,
                    insertIndex = retIdx,
                    opCode = OP_TRANSLATE_VIEW,
                    argReg = retReg,
                    preserveLabels = true,
                )
            }
        }

        run {
            val method = selectPenViewOnFinishInflateFingerprint.method
            val instrs = method.implementation!!.instructions
            val retIdx = instrs.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            check(retIdx >= 0) { "RETURN_VOID not found in SelectPenView.onFinishInflate()V" }
            emitDispatchConstantAt(
                method = method,
                insertIndex = retIdx,
                opCode = OP_TRANSLATE_VIEW,
                argReg = method.thisRegisterIndex(),
                preserveLabels = true,
            )
        }

        hookBottomSheetOnStartDispatch(OP_DIALOG_PRE_SHOW)
        hookAllHuanciPopupWindows(classes3Dex = classes3Dex, classes4Dex = classes4Dex)
        hookRecentDelPopupMenu()
        hookAdapterBindTranslations()

        // 5. Toast, Glide Drawable, 3D arrow Bitmap, and call-site hooks across DEXs.
        hookToastHelper()
        hookClasses2ToastsAndDrawables()
        hookRxToastAndDraftAdapters()
        hookD3ArrowBitmaps()
        hookHuanciPackageCallSites(
            classes3Dex = classes3Dex,
            classes4Dex = classes4Dex,
            psdHelperType = MultiVersionResolvers.resolvePsdHelperClass(classes).type,
        )
        hookHuanciCanvasDrawText(
            shareBitmapWrapperType = MultiVersionResolvers.resolveShareBitmapWrapperClass(classes).type,
        )
    }
}

private sealed class PopupHookKind {
    data class BeforeInvokeArg(val argIndex: Int, val opCode: Int) : PopupHookKind()
    object AfterInflateResult : PopupHookKind()
}

private fun BytecodePatchContext.collectSubclassHierarchy(roots: Set<String>): Set<String> {
    val superToChildren = HashMap<String, MutableList<String>>()
    classDefForEach { classDef ->
        val sup = classDef.superclass ?: return@classDefForEach
        superToChildren.getOrPut(sup) { mutableListOf() }.add(classDef.type)
    }
    val visited = HashSet<String>(roots)
    val queue = ArrayDeque<String>(roots)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val children = superToChildren[current] ?: continue
        for (child in children) {
            if (visited.add(child)) {
                queue.addLast(child)
            }
        }
    }
    return visited
}

private fun classifyPopupOrMenuHook(
    classType: String,
    sourceFile: String?,
    instructions: List<Instruction>,
    idx: Int,
    popupWindowTypes: Set<String>,
    dialogTypes: Set<String>,
): PopupHookKind? {
    val inst = instructions[idx]
    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    val opcode = inst.opcode

    if (opcode == Opcode.INVOKE_VIRTUAL ||
        opcode == Opcode.INVOKE_VIRTUAL_RANGE ||
        opcode == Opcode.INVOKE_SUPER ||
        opcode == Opcode.INVOKE_SUPER_RANGE ||
        opcode == Opcode.INVOKE_INTERFACE ||
        opcode == Opcode.INVOKE_INTERFACE_RANGE
    ) {
        if (ref.definingClass in popupWindowTypes &&
            (ref.name == "showAsDropDown" || ref.name == "showAtLocation")
        ) {
            return PopupHookKind.BeforeInvokeArg(0, OP_TRANSLATE_AND_OBSERVE)
        }
        if (ref.definingClass in popupWindowTypes &&
            ref.name == "setContentView" &&
            ref.parameterTypes.size == 1 &&
            ref.parameterTypes[0].toString() == "Landroid/view/View;"
        ) {
            return PopupHookKind.BeforeInvokeArg(1, OP_TRANSLATE_AND_OBSERVE)
        }
        if (ref.definingClass in dialogTypes &&
            ref.name == "show" &&
            ref.parameterTypes.isEmpty() &&
            ref.returnType == "V" &&
            !classType.startsWith("Lnet/huanci/hsjpro/dialog/CommonDialog") &&
            !classType.startsWith("Lnet/huanci/hsjpro/dialog/BaseDialog")
        ) {
            return PopupHookKind.BeforeInvokeArg(0, OP_DIALOG_PRE_SHOW)
        }
        if ((ref.definingClass == "Landroid/view/WindowManager;" ||
                ref.definingClass == "Landroid/view/ViewManager;") &&
            ref.name == "addView" &&
            ref.parameterTypes.size == 2 &&
            ref.parameterTypes[0].toString() == "Landroid/view/View;"
        ) {
            return PopupHookKind.BeforeInvokeArg(1, OP_TRANSLATE_AND_OBSERVE)
        }
    }

    if ((opcode == Opcode.INVOKE_DIRECT || opcode == Opcode.INVOKE_DIRECT_RANGE) &&
        ref.definingClass == "Landroid/widget/PopupWindow;" &&
        ref.name == "<init>" &&
        ref.parameterTypes.firstOrNull()?.toString() == "Landroid/view/View;"
    ) {
        return PopupHookKind.BeforeInvokeArg(1, OP_TRANSLATE_AND_OBSERVE)
    }

    if ((opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
        ref.definingClass == "Landroidx/core/widget/PopupWindowCompat;" &&
        ref.name == "showAsDropDown" &&
        ref.parameterTypes.isNotEmpty()
    ) {
        return PopupHookKind.BeforeInvokeArg(0, OP_TRANSLATE_AND_OBSERVE)
    }

    if ((sourceFile == "SelectPenView.java" || classType.contains("SelectPenView")) &&
        ((ref.definingClass == "Landroid/view/LayoutInflater;" && ref.name == "inflate") ||
            (ref.definingClass == "Landroid/view/View;" && ref.name == "inflate")) &&
        instructions.findMoveResultObjectAfter(idx) != null
    ) {
        return PopupHookKind.AfterInflateResult
    }

    return null
}

@Suppress("UNUSED_PARAMETER")
private fun BytecodePatchContext.hookAllHuanciPopupWindows(
    classes3Dex: DexBackedDexFile?,
    classes4Dex: DexBackedDexFile?,
) {
    val popupWindowTypes = collectSubclassHierarchy(setOf("Landroid/widget/PopupWindow;"))
    val dialogTypes = collectSubclassHierarchy(
        setOf(
            "Landroid/app/Dialog;",
            "Landroidx/appcompat/app/AppCompatDialog;",
            "Landroidx/appcompat/app/AlertDialog;",
            "Lcom/google/android/material/bottomsheet/BottomSheetDialog;",
        ),
    )

    val candidateClasses = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith("Lnet/huanci/") && classDef.type !in popupWindowTypes) {
            return@classDefForEach
        }
        val hasMatch = classDef.methods.any { m ->
            val instrs = m.implementation?.instructions?.toList() ?: return@any false
            instrs.indices.any { idx ->
                classifyPopupOrMenuHook(
                    classDef.type,
                    classDef.sourceFile,
                    instrs,
                    idx,
                    popupWindowTypes,
                    dialogTypes,
                ) != null
            }
        }
        if (hasMatch) {
            candidateClasses += classDef
        }
    }

    for (classDef in candidateClasses) {
        val isSelectPenView = classDef.sourceFile == "SelectPenView.java" ||
            classDef.type.contains("SelectPenView")
        val mutableClass = mutableClassDefBy(classDef)
        val targetMethods = mutableClass.methods.filter { m ->
            val instrs = m.implementation?.instructions ?: return@filter false
            instrs.indices.any { idx ->
                classifyPopupOrMenuHook(
                    classDef.type,
                    classDef.sourceFile,
                    instrs,
                    idx,
                    popupWindowTypes,
                    dialogTypes,
                ) != null
            }
        }
        for (origMethod in targetMethods) {
            var currentMethod = origMethod
            val initialInstrs = currentMethod.implementation!!.instructions
            val matchCount = initialInstrs.indices.count { idx ->
                classifyPopupOrMenuHook(
                    classDef.type,
                    classDef.sourceFile,
                    initialInstrs,
                    idx,
                    popupWindowTypes,
                    dialogTypes,
                ) != null
            }
            for (occ in matchCount - 1 downTo 0) {
                val instrs = currentMethod.implementation!!.instructions
                val matchIndices = instrs.indices.filter { idx ->
                    classifyPopupOrMenuHook(
                        classDef.type,
                        classDef.sourceFile,
                        instrs,
                        idx,
                        popupWindowTypes,
                        dialogTypes,
                    ) != null
                }
                if (occ >= matchIndices.size) continue
                val matchIdx = matchIndices[occ]
                val kind = classifyPopupOrMenuHook(
                    classDef.type,
                    classDef.sourceFile,
                    instrs,
                    matchIdx,
                    popupWindowTypes,
                    dialogTypes,
                ) ?: continue
                try {
                    when (kind) {
                        is PopupHookKind.BeforeInvokeArg -> {
                            val allRegs = invokeRegisterList(instrs[matchIdx])
                            if (kind.argIndex >= allRegs.size) continue
                            currentMethod = emitTranslationDispatch(
                                method = currentMethod,
                                insertIndex = matchIdx,
                                opCode = kind.opCode,
                                preserveLabels = true,
                                *allRegs,
                            ) { m, shiftedIdx ->
                                invokeRegisterList(m.implementation!!.instructions[shiftedIdx])[kind.argIndex]
                            }
                        }
                        is PopupHookKind.AfterInflateResult -> {
                            val (moveIdx, resultReg) = instrs.findMoveResultObjectAfter(matchIdx) ?: continue
                            currentMethod = emitTranslationDispatch(
                                method = currentMethod,
                                insertIndex = moveIdx + 1,
                                opCode = OP_TRANSLATE_AND_OBSERVE,
                                preserveLabels = false,
                                resultReg,
                            ) { m, shiftedIdx ->
                                val prevMove = m.implementation!!.instructions.findPrevOpcode(
                                    shiftedIdx,
                                    Opcode.MOVE_RESULT_OBJECT,
                                    3,
                                )
                                check(prevMove >= 0) {
                                    "MOVE_RESULT_OBJECT not found before shifted index $shiftedIdx"
                                }
                                (m.implementation!!.instructions[prevMove] as OneRegisterInstruction).registerA
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    if (isSelectPenView) throw e
                }
            }
        }
    }
}

private fun isRegisterModifiedInInstructions(instructions: List<Instruction>, reg: Int): Boolean =
    instructions.any { inst ->
        inst.opcode != Opcode.CHECK_CAST &&
            inst.opcode.setsRegister() &&
            (inst as? OneRegisterInstruction)?.registerA == reg
    }

private fun BytecodePatchContext.hookAdapterBindTranslations() {
    val adapterClassDefs = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith("Lnet/huanci/")) {
            return@classDefForEach
        }
        val hasBind = classDef.methods.any { m ->
            m.name == "onBindViewHolder" &&
                m.returnType == "V" &&
                m.parameterTypes.size >= 2 &&
                m.parameterTypes[0].toString().contains("ViewHolder;") &&
                m.parameterTypes[1].toString() == "I" &&
                m.implementation != null
        }
        if (hasBind) {
            adapterClassDefs += classDef
        }
    }
    for (classDef in adapterClassDefs) {
        val mutableClass = mutableClassDefBy(classDef)
        val bindMethods = mutableClass.methods.filter { m ->
            m.name == "onBindViewHolder" &&
                m.returnType == "V" &&
                m.parameterTypes.size >= 2 &&
                m.parameterTypes[0].toString().contains("ViewHolder;") &&
                m.parameterTypes[1].toString() == "I" &&
                m.implementation != null
        }
        for (origMethod in bindMethods) {
            var currentMethod = origMethod
            val origHolderReg = currentMethod.thisRegisterIndex() + 1
            val origInstrs = currentMethod.implementation!!.instructions
            val holderOverwritten = isRegisterModifiedInInstructions(origInstrs, origHolderReg)
            if (holderOverwritten) {
                // Attach ViewTreeObserver at method entry when p1 is later reused inside the body.
                runCatching {
                    emitTranslationDispatch(
                        method = currentMethod,
                        insertIndex = 0,
                        opCode = OP_ATTACH_OBSERVER,
                        preserveLabels = false,
                    ) { m, _ -> m.thisRegisterIndex() + 1 }
                }
            } else {
                val retCount = currentMethod.implementation!!.instructions.count { it.opcode == Opcode.RETURN_VOID }
                for (occ in retCount - 1 downTo 0) {
                    val instrs = currentMethod.implementation!!.instructions
                    val retIndices = instrs.indices.filter { instrs[it].opcode == Opcode.RETURN_VOID }
                    if (occ >= retIndices.size) continue
                    val retIdx = retIndices[occ]
                    runCatching {
                        currentMethod = emitTranslationDispatch(
                            method = currentMethod,
                            insertIndex = retIdx,
                            opCode = OP_TRANSLATE_VIEW,
                            preserveLabels = true,
                        ) { m, _ -> m.thisRegisterIndex() + 1 }
                    }
                }
            }
        }
    }
}

private fun isCanvasDrawTextStringCall(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    if (ref.definingClass != "Landroid/graphics/Canvas;" || ref.name != "drawText" || ref.returnType != "V") {
        return false
    }
    return ref.parameterTypes.map { it.toString() } ==
        listOf("Ljava/lang/String;", "F", "F", "Landroid/graphics/Paint;")
}

private fun BytecodePatchContext.hookHuanciCanvasDrawText(shareBitmapWrapperType: String) {
    val targetClasses = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        val isTarget = (classDef.type.startsWith("Lnet/huanci/") &&
            classDef.type != "Lnet/huanci/hsjpro/paint/views/TextViewVertical;" &&
            classDef.type != "Lnet/huanci/hsjpro/paint/views/PaintTextView;") ||
            classDef.type == shareBitmapWrapperType
        if (!isTarget) return@classDefForEach
        val hasDrawText = classDef.methods.any { m ->
            m.implementation?.instructions?.any { isCanvasDrawTextStringCall(it) } == true
        }
        if (hasDrawText) {
            targetClasses += classDef
        }
    }
    for (classDef in targetClasses) {
        val mutableClass = mutableClassDefBy(classDef)
        val methods = mutableClass.methods.filter { m ->
            m.implementation?.instructions?.any { isCanvasDrawTextStringCall(it) } == true
        }
        for (origMethod in methods) {
            var currentMethod = origMethod
            val matchCount = currentMethod.implementation!!.instructions.count { isCanvasDrawTextStringCall(it) }
            for (occ in matchCount - 1 downTo 0) {
                val instrs = currentMethod.implementation!!.instructions
                val matches = instrs.indices.filter { isCanvasDrawTextStringCall(instrs[it]) }
                if (occ >= matches.size) continue
                val drawIdx = matches[occ]
                val regs = invokeRegisterList(instrs[drawIdx])
                if (regs.size != 5) continue
                val freeTriple = runCatching {
                    resolveLowRegisterAt(currentMethod, drawIdx, *regs)
                }.getOrNull() ?: continue
                val updatedMethod = freeTriple.first
                val scratchReg = freeTriple.second
                val shiftedIdx = freeTriple.third
                val shiftedInst = updatedMethod.implementation!!.instructions[shiftedIdx]
                val shiftedRegs = invokeRegisterList(shiftedInst)
                if (shiftedRegs.size != 5 || scratchReg !in 0..15 || scratchReg in shiftedRegs) {
                    continue
                }
                val textReg = shiftedRegs[1]
                if (textReg in 0..15 && shiftedRegs.all { it in 0..15 } && shiftedInst is FiveRegisterInstruction) {
                    val cReg = shiftedRegs[0]
                    val xReg = shiftedRegs[2]
                    val yReg = shiftedRegs[3]
                    val paintReg = shiftedRegs[4]
                    updatedMethod.replaceInstruction(
                        shiftedIdx,
                        "invoke-virtual {v$cReg, v$scratchReg, v$xReg, v$yReg, v$paintReg}, " +
                            "Landroid/graphics/Canvas;->drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V",
                    )
                    updatedMethod.addInstructionsPreservingLabels(
                        shiftedIdx,
                        """
                        const/4 v$scratchReg, $OP_TRANSLATE_STRING
                        ${formatInvokeStatic2(DISPATCH_SIGNATURE, scratchReg, textReg)}
                        move-result-object v$scratchReg
                        check-cast v$scratchReg, Ljava/lang/String;
                        """.trimIndent(),
                    )
                    currentMethod = updatedMethod
                } else if (textReg <= 255) {
                    val opTriple = runCatching {
                        resolveLowRegisterAt(updatedMethod, shiftedIdx, scratchReg, *shiftedRegs)
                    }.getOrNull() ?: continue
                    val curM = opTriple.first
                    val opReg = opTriple.second
                    val opIdx = opTriple.third
                    val curRegs = invokeRegisterList(curM.implementation!!.instructions[opIdx])
                    val curTextReg = curRegs[1]
                    curM.addInstructionsPreservingLabels(
                        opIdx,
                        """
                        const/4 v$opReg, $OP_TRANSLATE_STRING
                        move-object/from16 v$scratchReg, v$curTextReg
                        ${formatInvokeStatic2(DISPATCH_SIGNATURE, opReg, scratchReg)}
                        move-result-object v$curTextReg
                        check-cast v$curTextReg, Ljava/lang/String;
                        """.trimIndent(),
                    )
                    currentMethod = curM
                }
            }
        }
    }
}

private fun BytecodePatchContext.hookRecentDelPopupMenu() {
    val recentDelClass = allClasses().resolveSingleClass("RecentDelDraftActivity") {
        it.type == "Lnet/huanci/hsjpro/activities/RecentDelDraftActivity;"
    }
    val mutableClass = mutableClassDefBy(recentDelClass)
    val targetMethod = mutableClass.methods.firstOrNull { m ->
        val instrs = m.implementation?.instructions ?: return@firstOrNull false
        var hasInflate = false
        var hasTopRight = false
        for (insn in instrs) {
            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
            if (ref.definingClass == "Landroid/view/LayoutInflater;" && ref.name == "inflate") {
                hasInflate = true
            } else if (ref.definingClass == "Lnet/huanci/hsjpro/views/TopBar;" && ref.name == "getImageView_right") {
                hasTopRight = true
            }
        }
        hasInflate && hasTopRight
    } ?: error("Popup builder method not found in RecentDelDraftActivity")

    val instrs = targetMethod.implementation!!.instructions
    val inflateIdx = instrs.indexOfFirst { insn ->
        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
        ref.definingClass == "Landroid/view/LayoutInflater;" && ref.name == "inflate"
    }
    val (moveResultIdx, viewReg) = checkNotNull(instrs.findMoveResultObjectAfter(inflateIdx)) {
        "Expected MOVE_RESULT_OBJECT after LayoutInflater.inflate in RecentDelDraftActivity"
    }
    emitDispatchConstantAt(
        method = targetMethod,
        insertIndex = moveResultIdx + 1,
        opCode = OP_TRANSLATE_AND_OBSERVE,
        argReg = viewReg,
        preserveLabels = true,
    )
}
