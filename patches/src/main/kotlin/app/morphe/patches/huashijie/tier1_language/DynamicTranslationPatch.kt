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
import app.morphe.patches.huashijie.core.findPrevOpcode
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.hookBottomSheetOnStartDispatch
import app.morphe.patches.huashijie.core.invokeRegisterList
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
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
private const val OP_TRANSLATE_STRING = 6

private enum class TargetDex {
    CLASSES3,
    CLASSES4,
    OTHER,
}

private data class CallSiteEdit(
    val index: Int,
    val smali: String,
    val mode: EditMode,
    val castSmali: String? = null,
)

private enum class EditMode {
    INSERT_BEFORE_PRESERVING_LABELS,
    INSERT_AFTER_NEXT,
    REPLACE_SINGLE,
    REPLACE_INVOKE_AND_CAST_AFTER_MOVE,
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

private fun collectClasses3Edits(instructions: List<Instruction>): List<CallSiteEdit> {
    val edits = mutableListOf<CallSiteEdit>()
    for (idx in instructions.indices) {
        val inst = instructions[idx]
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: continue

        if ((inst.opcode == Opcode.INVOKE_STATIC || inst.opcode == Opcode.INVOKE_STATIC_RANGE) &&
            ref.definingClass == "Landroid/widget/Toast;" &&
            ref.name == "makeText"
        ) {
            val params = ref.parameterTypes.map { it.toString() }
            if (params == listOf("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I")) {
                val charSeqReg = invokeArgRegister(inst, 1) ?: continue
                if (charSeqReg < 16) {
                    edits += CallSiteEdit(
                        index = idx,
                        smali = """
                            invoke-static {v$charSeqReg}, ${TRANSLATION_ENGINE_CLASS}->onToastText(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                            move-result-object v$charSeqReg
                        """.trimIndent(),
                        mode = EditMode.INSERT_BEFORE_PRESERVING_LABELS,
                    )
                }
            } else if (params == listOf("Landroid/content/Context;", "I", "I")) {
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
                }
                if (replacement != null) {
                    edits += CallSiteEdit(
                        index = idx,
                        smali = replacement,
                        mode = EditMode.REPLACE_SINGLE,
                    )
                }
            }
            continue
        }

        if ((inst.opcode == Opcode.INVOKE_VIRTUAL || inst.opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
            ref.definingClass == "Landroid/content/res/Resources;" &&
            ref.parameterTypes.size == 1 &&
            ref.parameterTypes[0].toString() == "I"
        ) {
            val (_, outReg) = instructions.findMoveResultObjectAfter(idx) ?: continue
            val resReg = invokeArgRegister(inst, 0) ?: continue
            val idReg = invokeArgRegister(inst, 1) ?: continue
            if (idReg == outReg) {
                if (resReg >= 16 || idReg >= 16 || outReg >= 256) {
                    continue
                }
                val castType = when {
                    ref.name == "getStringArray" && ref.returnType == "[Ljava/lang/String;" -> "[Ljava/lang/String;"
                    ref.name == "getString" && ref.returnType == "Ljava/lang/String;" -> "Ljava/lang/String;"
                    else -> null
                } ?: continue
                edits += CallSiteEdit(
                    index = idx,
                    smali = "invoke-static {v$idReg, v$resReg}, $DISPATCH_SIGNATURE",
                    mode = EditMode.REPLACE_INVOKE_AND_CAST_AFTER_MOVE,
                    castSmali = "check-cast v$outReg, $castType",
                )
                continue
            }
            if (idReg >= 16 || outReg >= 16) {
                continue
            }
            if (ref.name == "getString" && ref.returnType == "Ljava/lang/String;") {
                edits += CallSiteEdit(
                    index = idx,
                    smali = """
                        invoke-static {v$idReg, v$outReg}, ${TRANSLATION_ENGINE_CLASS}->translateById(ILjava/lang/String;)Ljava/lang/String;
                        move-result-object v$outReg
                    """.trimIndent(),
                    mode = EditMode.INSERT_AFTER_NEXT,
                )
            } else if (ref.name == "getStringArray" && ref.returnType == "[Ljava/lang/String;") {
                edits += CallSiteEdit(
                    index = idx,
                    smali = """
                        invoke-static {v$idReg, v$outReg}, ${TRANSLATION_ENGINE_CLASS}->translateArrayById(I[Ljava/lang/String;)[Ljava/lang/String;
                        move-result-object v$outReg
                    """.trimIndent(),
                    mode = EditMode.INSERT_AFTER_NEXT,
                )
            }
        }
    }
    return edits
}

private fun collectClasses4Edits(instructions: List<Instruction>): List<CallSiteEdit> {
    val edits = mutableListOf<CallSiteEdit>()
    for (idx in instructions.indices) {
        val inst = instructions[idx]
        if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) {
            continue
        }
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: continue
        if (ref.definingClass != "Landroid/content/res/Resources;" ||
            ref.parameterTypes.size != 1 ||
            ref.parameterTypes[0].toString() != "I"
        ) {
            continue
        }
        val (_, outReg) = instructions.findMoveResultObjectAfter(idx) ?: continue
        val resReg = invokeArgRegister(inst, 0) ?: continue
        val idReg = invokeArgRegister(inst, 1) ?: continue
        val castType = when {
            ref.name == "getStringArray" && ref.returnType == "[Ljava/lang/String;" -> "[Ljava/lang/String;"
            ref.name == "getString" && ref.returnType == "Ljava/lang/String;" -> "Ljava/lang/String;"
            else -> null
        } ?: continue

        if (idReg == outReg) {
            if (resReg >= 16 || idReg >= 16 || outReg >= 256) {
                continue
            }
            edits += CallSiteEdit(
                index = idx,
                smali = "invoke-static {v$idReg, v$resReg}, $DISPATCH_SIGNATURE",
                mode = EditMode.REPLACE_INVOKE_AND_CAST_AFTER_MOVE,
                castSmali = "check-cast v$outReg, $castType",
            )
            continue
        }
        if (idReg >= 16 || outReg >= 16) {
            continue
        }

        // Route all classes4.dex resource hooks through the single dispatch signature.
        edits += CallSiteEdit(
            index = idx,
            smali = """
                invoke-static {v$idReg, v$outReg}, $DISPATCH_SIGNATURE
                move-result-object v$outReg
                check-cast v$outReg, $castType
            """.trimIndent(),
            mode = EditMode.INSERT_AFTER_NEXT,
        )
    }
    return edits
}

private fun BytecodePatchContext.hookHuanciPackageCallSites(
    classes3Dex: DexBackedDexFile?,
    classes4Dex: DexBackedDexFile?,
) {
    val pendingClasses = mutableListOf<Pair<ClassDef, TargetDex>>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith("Lnet/huanci/")) {
            return@classDefForEach
        }
        val targetDex = classifyTargetDex(classDef, classes3Dex, classes4Dex)
        if (targetDex == TargetDex.OTHER) {
            return@classDefForEach
        }
        val hasCandidateMethod = classDef.methods.any { m ->
            val instrs = m.implementation?.instructions?.toList() ?: return@any false
            when (targetDex) {
                TargetDex.CLASSES3 -> collectClasses3Edits(instrs).isNotEmpty()
                TargetDex.CLASSES4 -> collectClasses4Edits(instrs).isNotEmpty()
                TargetDex.OTHER -> false
            }
        }
        if (hasCandidateMethod) {
            pendingClasses += classDef to targetDex
        }
    }

    for ((classDef, targetDex) in pendingClasses) {
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods.toList()) {
            val impl = method.implementation ?: continue
            val edits = when (targetDex) {
                TargetDex.CLASSES3 -> collectClasses3Edits(impl.instructions)
                TargetDex.CLASSES4 -> collectClasses4Edits(impl.instructions)
                TargetDex.OTHER -> emptyList()
            }
            if (edits.isEmpty()) {
                continue
            }
            for (edit in edits.sortedByDescending { it.index }) {
                when (edit.mode) {
                    EditMode.INSERT_BEFORE_PRESERVING_LABELS ->
                        method.addInstructionsPreservingLabels(edit.index, edit.smali)
                    EditMode.INSERT_AFTER_NEXT ->
                        method.addInstructions(edit.index + 2, edit.smali)
                    EditMode.REPLACE_SINGLE ->
                        method.replaceInstruction(edit.index, edit.smali)
                    EditMode.REPLACE_INVOKE_AND_CAST_AFTER_MOVE -> {
                        val cast = requireNotNull(edit.castSmali)
                        method.addInstructions(edit.index + 2, cast)
                        method.replaceInstruction(edit.index, edit.smali)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
val dynamicTranslationPatch = bytecodePatch(
    name = "Dynamic External Translation Engine",
    description = "Translates the HuashijiePro studio interface from external JSON packs with instant switching.",
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

        // 5. Global DEX call-site hooks across net/huanci/ in classes3.dex and classes4.dex.
        hookHuanciPackageCallSites(classes3Dex = classes3Dex, classes4Dex = classes4Dex)
        hookHuanciCanvasDrawText()
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
    if (inst !is FiveRegisterInstruction) return false
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    if (ref.definingClass != "Landroid/graphics/Canvas;" || ref.name != "drawText" || ref.returnType != "V") {
        return false
    }
    return ref.parameterTypes.map { it.toString() } ==
        listOf("Ljava/lang/String;", "F", "F", "Landroid/graphics/Paint;")
}

private fun BytecodePatchContext.hookHuanciCanvasDrawText() {
    val targetClasses = mutableListOf<ClassDef>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith("Lnet/huanci/")) return@classDefForEach
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
                val shiftedRegs = invokeRegisterList(updatedMethod.implementation!!.instructions[shiftedIdx])
                if (shiftedRegs.size != 5 || scratchReg !in 0..15 || scratchReg in shiftedRegs || shiftedRegs.any { it !in 0..15 }) {
                    continue
                }
                val cReg = shiftedRegs[0]
                val textReg = shiftedRegs[1]
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
