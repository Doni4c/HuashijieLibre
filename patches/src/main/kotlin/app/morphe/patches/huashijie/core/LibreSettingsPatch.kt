package app.morphe.patches.huashijie.core

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val LIBRE_SETTINGS_INJECTOR_CLASS =
    "Lapp/morphe/extension/huashijie/LibreSettingsInjector;"
internal const val LIBRE_TRANSLATION_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_ENSURE_BRUSH_PACKAGE_LIST = 51

private val drawApplicationOnCreateFingerprint = exactClassMethodFingerprint(
    "Lnet/huanci/hsjpro/DrawApplication;",
    "onCreate",
    "V",
)

internal val generalSettingInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.sourceFile == "GeneralSettingActivity.java" &&
            classDef.type.startsWith("Lnet/huanci/hsjpro/")
    },
)

internal fun MutableMethod.parameterRegisterCount(): Int {
    var count = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    for (param in parameterTypes) {
        count += if (param == "J" || param == "D") 2 else 1
    }
    return count
}

internal fun MutableMethod.thisRegisterIndex(): Int {
    val impl = requireNotNull(implementation) {
        "Method $definingClass->$name has no implementation."
    }
    return impl.registerCount - parameterRegisterCount()
}

internal fun MutableMethod.addInstructionsPreservingLabels(index: Int, smali: String) {
    val impl = requireNotNull(implementation) {
        "Method $definingClass->$name has no implementation."
    }
    val hasLabels = index < impl.instructions.size &&
        getInstruction(index).location.labels.isNotEmpty()
    if (hasLabels) {
        addInstructionsAtControlFlowLabel(index, smali)
    } else {
        addInstructions(index, smali)
    }
}

internal fun formatInvokeStaticSingle(signature: String, register: Int): String =
    if (register in 0..15) {
        "invoke-static {v$register}, $signature"
    } else {
        "invoke-static/range {v$register .. v$register}, $signature"
    }

internal fun BytecodePatchContext.resolveLowRegisterAt(
    method: MutableMethod,
    insertIndex: Int,
    vararg excludeRegisters: Int,
): Triple<MutableMethod, Int, Int> {
    val impl = requireNotNull(method.implementation) {
        "Method ${method.definingClass}->${method.name} has no implementation."
    }
    val excludedSet = excludeRegisters.toMutableSet()
    val found = runCatching {
        method.findFreeRegister(insertIndex, excludedSet.toList())
    }.getOrNull()

    if (found != null && found in 0..15) {
        return Triple(method, found, insertIndex)
    }

    if (impl.registerCount < 16) {
        val (expanded, addedMoves) = method.addFreeRegisters(1)
        val shiftedIndex = insertIndex + addedMoves
        val expandedImpl = requireNotNull(expanded.implementation)
        val newExcluded = if (!AccessFlags.STATIC.isSet(expanded.accessFlags) &&
            expanded.thisRegisterIndex() !in excludedSet
        ) {
            // Preserve newly shifted p0 in case the caller invokes an instance hook afterwards.
            excludedSet + expanded.thisRegisterIndex()
        } else {
            excludedSet
        }
        val liveCandidate = runCatching {
            expanded.findFreeRegister(shiftedIndex, newExcluded.toList())
        }.getOrNull()
        if (liveCandidate != null && liveCandidate in 0..15 && liveCandidate !in newExcluded) {
            return Triple(expanded, liveCandidate, shiftedIndex)
        }
        val topAddedReg = expandedImpl.registerCount - 1
        if (topAddedReg in 0..15 && topAddedReg !in newExcluded) {
            return Triple(expanded, topAddedReg, shiftedIndex)
        }
    }

    error(
        "No free 4-bit register available at index $insertIndex in " +
            "${method.definingClass}->${method.name}"
    )
}

internal fun exactClassMethodFingerprint(
    classType: String,
    methodName: String,
    returnType: String,
    vararg params: String,
): Fingerprint = Fingerprint(
    name = methodName,
    returnType = returnType,
    parameters = params.toList(),
    custom = { _, classDef -> classDef.type == classType },
)

internal fun invokeRegisterList(inst: Instruction): IntArray = when (inst) {
    is FiveRegisterInstruction -> intArrayOf(
        inst.registerC,
        inst.registerD,
        inst.registerE,
        inst.registerF,
        inst.registerG,
    ).copyOf(inst.registerCount)
    is RegisterRangeInstruction -> IntArray(inst.registerCount) { inst.startRegister + it }
    else -> IntArray(0)
}

internal fun BytecodePatchContext.emitTranslationDispatch(
    method: MutableMethod,
    insertIndex: Int,
    opCode: Int,
    preserveLabels: Boolean = true,
    vararg extraExcluded: Int,
    argRegProvider: (MutableMethod, Int) -> Int,
): MutableMethod {
    val origArgReg = argRegProvider(method, insertIndex)
    val allExcluded = (extraExcluded.toList() + origArgReg).distinct().toIntArray()
    val (targetMethod, scratchReg, shiftedIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = insertIndex,
        *allExcluded,
    )
    val opConst = if (opCode in -8..7) {
        "const/4 v$scratchReg, $opCode"
    } else {
        "const/16 v$scratchReg, $opCode"
    }
    val resolvedArgReg = argRegProvider(targetMethod, shiftedIdx)
    if (resolvedArgReg in 0..15) {
        val block = """
            $opConst
            invoke-static {v$scratchReg, v$resolvedArgReg}, $LIBRE_TRANSLATION_DISPATCH
        """.trimIndent()
        if (preserveLabels) {
            targetMethod.addInstructionsPreservingLabels(shiftedIdx, block)
        } else {
            targetMethod.addInstructions(shiftedIdx, block)
        }
        return targetMethod
    }

    val (target2, objReg, shiftedIdx2) = resolveLowRegisterAt(
        method = targetMethod,
        insertIndex = shiftedIdx,
        scratchReg,
        resolvedArgReg,
        *extraExcluded,
    )
    val finalArgReg = argRegProvider(target2, shiftedIdx2)
    val block = """
        $opConst
        move-object/from16 v$objReg, v$finalArgReg
        invoke-static {v$scratchReg, v$objReg}, $LIBRE_TRANSLATION_DISPATCH
    """.trimIndent()
    if (preserveLabels) {
        target2.addInstructionsPreservingLabels(shiftedIdx2, block)
    } else {
        target2.addInstructions(shiftedIdx2, block)
    }
    return target2
}

internal fun BytecodePatchContext.hookPaintBubblePopupBeforeShow(
    fingerprint: Fingerprint,
    controllerMethodSignature: String,
) {
    val classes = allClasses()
    val bubbleClass = MultiVersionResolvers.resolveBubblePopupClass(classes)
    val showAnchored = MultiVersionResolvers.resolveBubblePopupShowAnchored(bubbleClass)
    val method = fingerprint.method
    val instructions = method.implementation!!.instructions
    val showIdx = instructions.indexOfFirst { inst ->
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
        ref != null &&
            ref.definingClass == showAnchored.definingClass &&
            ref.name == showAnchored.name &&
            ref.returnType == showAnchored.returnType &&
            ref.parameterTypes.map { it.toString() } == showAnchored.parameterTypes.map { it.toString() }
    }
    check(showIdx >= 0) {
        "BubblePopup showAnchored not found in ${method.definingClass}->${method.name}"
    }
    val allRegs = invokeRegisterList(instructions[showIdx])
    check(allRegs.isNotEmpty()) {
        "Empty register list at BubblePopup showAnchored in ${method.definingClass}->${method.name}"
    }
    val (targetMethod, scratchReg, shiftedShowIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = showIdx,
        *allRegs,
    )
    val popupReg = invokeRegisterList(targetMethod.implementation!!.instructions[shiftedShowIdx])[0]
    check(popupReg in 0..15) {
        "Expected 4 bit popup register at BubblePopup showAnchored in ${targetMethod.definingClass}->${targetMethod.name}"
    }
    targetMethod.addInstructionsPreservingLabels(
        shiftedShowIdx,
        """
        invoke-virtual {v$popupReg}, Landroid/widget/PopupWindow;->getContentView()Landroid/view/View;
        move-result-object v$scratchReg
        invoke-static {v$scratchReg}, $controllerMethodSignature
        """.trimIndent(),
    )
}

internal fun BytecodePatchContext.hookPaintBubblePopupDispatch(
    fingerprint: Fingerprint,
    opCode: Int,
) {
    val classes = allClasses()
    val bubbleClass = MultiVersionResolvers.resolveBubblePopupClass(classes)
    val showAnchored = MultiVersionResolvers.resolveBubblePopupShowAnchored(bubbleClass)
    val method = fingerprint.method
    val instructions = method.implementation!!.instructions
    val showIdx = instructions.indexOfFirst { inst ->
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
        ref != null &&
            ref.definingClass == showAnchored.definingClass &&
            ref.name == showAnchored.name &&
            ref.returnType == showAnchored.returnType &&
            ref.parameterTypes.map { it.toString() } == showAnchored.parameterTypes.map { it.toString() }
    }
    check(showIdx >= 0) {
        "BubblePopup showAnchored not found in ${method.definingClass}->${method.name}"
    }
    val allRegs = invokeRegisterList(instructions[showIdx])
    check(allRegs.isNotEmpty()) {
        "Empty register list at BubblePopup showAnchored in ${method.definingClass}->${method.name}"
    }
    val (targetMethod, scratchReg, shiftedShowIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = showIdx,
        *allRegs,
    )
    val popupReg = invokeRegisterList(targetMethod.implementation!!.instructions[shiftedShowIdx])[0]
    check(popupReg in 0..15) {
        "Expected 4 bit popup register at BubblePopup showAnchored in ${targetMethod.definingClass}->${targetMethod.name}"
    }
    targetMethod.addInstructionsPreservingLabels(
        shiftedShowIdx,
        """
        invoke-virtual {v$popupReg}, Landroid/widget/PopupWindow;->getContentView()Landroid/view/View;
        move-result-object v$scratchReg
        """.trimIndent(),
    )
    val postGetIdx = shiftedShowIdx + 2
    emitTranslationDispatch(
        method = targetMethod,
        insertIndex = postGetIdx,
        opCode = opCode,
        preserveLabels = false,
        scratchReg,
    ) { _, _ -> scratchReg }
}

internal fun BytecodePatchContext.hookPopupShowAsDropDownDispatch(
    fingerprint: Fingerprint,
    opCode: Int,
) {
    val method = fingerprint.method
    val instructions = method.implementation!!.instructions
    val showIdx = instructions.indexOfFirst { inst ->
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
        ref != null &&
            ref.definingClass == "Landroid/widget/PopupWindow;" &&
            ref.name == "showAsDropDown"
    }
    check(showIdx >= 0) {
        "PopupWindow.showAsDropDown not found in ${method.definingClass}->${method.name}"
    }
    val allRegs = invokeRegisterList(instructions[showIdx])
    check(allRegs.isNotEmpty()) {
        "Empty register list at showAsDropDown in ${method.definingClass}->${method.name}"
    }
    emitTranslationDispatch(
        method = method,
        insertIndex = showIdx,
        opCode = opCode,
        preserveLabels = true,
        *allRegs,
    ) { m, idx ->
        invokeRegisterList(m.implementation!!.instructions[idx])[0]
    }
}

internal fun BytecodePatchContext.hookBottomSheetOnStartDispatch(opCode: Int) {
    val method = exactClassMethodFingerprint(
        "Lnet/huanci/hsjpro/views/BaseBottomSheetDialog;",
        "onStart",
        "V",
    ).method
    val retIdx = method.implementation!!.instructions.indexOfLast {
        it.opcode == Opcode.RETURN_VOID
    }
    check(retIdx >= 0) { "RETURN_VOID not found in BaseBottomSheetDialog.onStart()V" }
    emitTranslationDispatch(
        method = method,
        insertIndex = retIdx,
        opCode = opCode,
        preserveLabels = true,
    ) { m, _ -> m.thisRegisterIndex() }
}

internal fun List<Instruction>.findNextOpcode(
    startIndex: Int,
    opcode: Opcode,
    maxLookahead: Int = 8,
): Int {
    val start = maxOf(0, startIndex)
    val end = minOf(size - 1, start + maxLookahead)
    for (i in start..end) {
        if (this[i].opcode == opcode) return i
    }
    return -1
}

internal fun List<Instruction>.findPrevOpcode(
    beforeIndex: Int,
    opcode: Opcode,
    maxLookback: Int = 6,
): Int {
    val start = minOf(size - 1, beforeIndex - 1)
    val end = maxOf(0, start - maxLookback)
    for (i in start downTo end) {
        if (this[i].opcode == opcode) return i
    }
    return -1
}

internal fun List<Instruction>.findMoveResultObjectAfter(invokeIndex: Int): Pair<Int, Int>? {
    val moveIdx = findNextOpcode(invokeIndex + 1, Opcode.MOVE_RESULT_OBJECT, 2)
    if (moveIdx < 0) return null
    val reg = (this[moveIdx] as? OneRegisterInstruction)?.registerA ?: return null
    return moveIdx to reg
}

internal fun BytecodePatchContext.registerToggleBeforeSettingsPanel(toggleKey: String) {
    val injectorClassDef = checkNotNull(classDefByOrNull(LIBRE_SETTINGS_INJECTOR_CLASS)) {
        "Extension class $LIBRE_SETTINGS_INJECTOR_CLASS not found. Ensure extendWith(\"extensions/huashijie-libre.mpe\") is declared."
    }
    val mutableInjector = mutableClassDefBy(injectorClassDef)
    val regMethod = checkNotNull(
        mutableInjector.methods.firstOrNull {
            it.name == "registerInstalledToggles" &&
                it.parameters.isEmpty() &&
                it.returnType == "V"
        }
    ) {
        "registerInstalledToggles()V not found in $LIBRE_SETTINGS_INJECTOR_CLASS"
    }

    val instructions = requireNotNull(regMethod.implementation?.instructions) {
        "registerInstalledToggles()V has no implementation."
    }
    val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
    check(retIdx >= 0) {
        "RETURN_VOID not found in $LIBRE_SETTINGS_INJECTOR_CLASS->registerInstalledToggles()V"
    }

    val (targetMethod, scratchReg, insertAt) = resolveLowRegisterAt(
        method = regMethod,
        insertIndex = retIdx,
    )
    val invokeRegisterLine = formatInvokeStaticSingle(
        "${LIBRE_SETTINGS_INJECTOR_CLASS}->registerToggle(Ljava/lang/String;)V",
        scratchReg,
    )
    targetMethod.addInstructionsPreservingLabels(
        insertAt,
        """
        const-string v$scratchReg, "$toggleKey"
        $invokeRegisterLine
        """.trimIndent(),
    )
}

internal fun BytecodePatchContext.registerBrushPackageListTargets(classes: List<ClassDef> = allClasses()) {
    val brushMgr = MultiVersionResolvers.resolveBrushPkgManagerClass(classes)
    registerResolvedTarget("BrushPkgManager.class", brushMgr.type.toJavaClassName())
    registerResolvedTarget(
        "BrushPkgManager.getInstance",
        MultiVersionResolvers.resolveBrushPkgManagerGetInstance(brushMgr).name,
    )
    registerResolvedTarget(
        "BrushPkgManager.reload",
        MultiVersionResolvers.resolveBrushPkgManagerReload(brushMgr).name,
    )
    registerResolvedTarget(
        "BrushPkgManager.listField",
        MultiVersionResolvers.resolveBrushPkgManagerListField(brushMgr).name,
    )
}

internal fun BytecodePatchContext.ensureBrushPkgManagerGetListHooked() {
    val classes = allClasses()
    val mgrClass = MultiVersionResolvers.resolveBrushPkgManagerClass(classes)
    var method = toMutableMethod(MultiVersionResolvers.resolveBrushPkgManagerGetList(mgrClass))
    val alreadyHooked = method.implementation!!.instructions.any { inst ->
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
        "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}" ==
            LIBRE_TRANSLATION_DISPATCH
    }
    if (alreadyHooked) {
        return
    }

    val retIndices = method.implementation!!.instructions.indices
        .filter { method.implementation!!.instructions[it].opcode == Opcode.RETURN_OBJECT }
        .reversed()
    check(retIndices.isNotEmpty()) {
        "RETURN_OBJECT not found in BrushPkgManager getList(Z)List"
    }
    for (retIdx in retIndices) {
        val origRetReg = (method.implementation!!.instructions[retIdx] as OneRegisterInstruction).registerA
        val (target1, opScratch, idx1) = resolveLowRegisterAt(
            method = method,
            insertIndex = retIdx,
            origRetReg,
        )
        val resolvedRetReg = (target1.implementation!!.instructions[idx1] as OneRegisterInstruction).registerA
        if (resolvedRetReg in 0..15) {
            target1.addInstructionsPreservingLabels(
                idx1,
                """
                const/16 v$opScratch, $OP_ENSURE_BRUSH_PACKAGE_LIST
                invoke-static {v$opScratch, v$resolvedRetReg}, $LIBRE_TRANSLATION_DISPATCH
                move-result-object v$resolvedRetReg
                check-cast v$resolvedRetReg, Ljava/util/List;
                """.trimIndent(),
            )
            method = target1
        } else {
            val (target2, objScratch, idx2) = resolveLowRegisterAt(
                method = target1,
                insertIndex = idx1,
                opScratch,
                resolvedRetReg,
            )
            val finalRetReg = (target2.implementation!!.instructions[idx2] as OneRegisterInstruction).registerA
            target2.addInstructionsPreservingLabels(
                idx2,
                """
                const/16 v$opScratch, $OP_ENSURE_BRUSH_PACKAGE_LIST
                move-object/from16 v$objScratch, v$finalRetReg
                invoke-static {v$opScratch, v$objScratch}, $LIBRE_TRANSLATION_DISPATCH
                move-result-object v$objScratch
                check-cast v$objScratch, Ljava/util/List;
                move-object/from16 v$finalRetReg, v$objScratch
                """.trimIndent(),
            )
            method = target2
        }
    }
}

@Suppress("unused")
val libreSettingsPatch = bytecodePatch(
    name = "Libre In App Settings Panel",
    description = "Adds the HuashijieLibre settings menu inside General Settings with update checks, credits, license info, and diagnostics export.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel("libre_enable_animations")

        val appOnCreate = drawApplicationOnCreateFingerprint.method
        val appInstrs = appOnCreate.implementation!!.instructions
        check(appInstrs.isNotEmpty() && appInstrs[0].opcode == Opcode.INVOKE_SUPER) {
            "Expected INVOKE_SUPER at index 0 of DrawApplication.onCreate()V"
        }
        appOnCreate.addInstructionsPreservingLabels(
            1,
            formatInvokeStaticSingle(
                "Lapp/morphe/extension/huashijie/LibreConfig;->init(Landroid/content/Context;)V",
                appOnCreate.thisRegisterIndex(),
            ),
        )

        val method = generalSettingInitViewFingerprint.method
        val instructions = method.implementation!!.instructions
        val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in GeneralSettingActivity.initView()V" }

        val thisReg = method.thisRegisterIndex()
        val invokeInjectLine = formatInvokeStaticSingle(
            "${LIBRE_SETTINGS_INJECTOR_CLASS}->injectGeneralSettings(Landroid/app/Activity;)V",
            thisReg,
        )
        method.addInstructionsPreservingLabels(retIdx, invokeInjectLine)
    }
}
