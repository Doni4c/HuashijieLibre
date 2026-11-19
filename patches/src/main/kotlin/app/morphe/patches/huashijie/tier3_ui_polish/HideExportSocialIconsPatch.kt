package app.morphe.patches.huashijie.tier3_ui_polish

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.findPrevOpcode
import app.morphe.patches.huashijie.core.hookPaintBubblePopupBeforeShow
import app.morphe.patches.huashijie.core.invokeRegisterList
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val CUSTOM_PLATFORM_ADAPTER_CLASS =
    "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;"
private const val BIND_CUSTOM_EXPORT_CLICK_SIG =
    "Lapp/morphe/extension/huashijie/GlobalCompatibilityController;->bindCustomExportClick(Ljava/lang/Object;Landroid/view/View;)V"
private val customAdapterOnBindFingerprint = Fingerprint(
    name = "onBindViewHolder",
    returnType = "V",
    parameters = listOf(
        "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter\$ViewHolder;",
        "I",
    ),
    custom = { _, classDef ->
        classDef.type == CUSTOM_PLATFORM_ADAPTER_CLASS
    },
)

private const val TOGGLE_KEY = "pure_studio_hide_domestic_export"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_HIDE_EXPORT_SOCIAL_ICONS = 14

private val exportPictureCreateViewFingerprint = Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/views/ExportPictureView;"
    },
)

private fun BytecodePatchContext.injectDispatchOnObjectReg(
    method: MutableMethod,
    insertIndex: Int,
    argRegProvider: (MutableMethod, Int) -> Int,
) {
    val origArgReg = argRegProvider(method, insertIndex)
    val (target, scratch, shiftedIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = insertIndex,
        origArgReg,
    )
    val resolvedArgReg = argRegProvider(target, shiftedIdx)
    if (resolvedArgReg in 0..15) {
        target.addInstructionsPreservingLabels(
            shiftedIdx,
            """
            const/16 v$scratch, $OP_HIDE_EXPORT_SOCIAL_ICONS
            invoke-static {v$scratch, v$resolvedArgReg}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )
    } else {
        val (target2, scratchObj, shiftedIdx2) = resolveLowRegisterAt(
            method = target,
            insertIndex = shiftedIdx,
            scratch,
            resolvedArgReg,
        )
        val finalArgReg = argRegProvider(target2, shiftedIdx2)
        target2.addInstructionsPreservingLabels(
            shiftedIdx2,
            """
            const/16 v$scratch, $OP_HIDE_EXPORT_SOCIAL_ICONS
            move-object/from16 v$scratchObj, v$finalArgReg
            invoke-static {v$scratch, v$scratchObj}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )
    }
}

private fun findExportListRegistersBefore(
    instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    endIndex: Int,
): List<Int> {
    val liveArrayLists = LinkedHashSet<Int>()
    for (i in 0 until endIndex) {
        val insn = instructions[i]
        val opcode = insn.opcode
        if (opcode == Opcode.NEW_INSTANCE) {
            val typeRef = (insn as? ReferenceInstruction)?.reference as? TypeReference
            if (typeRef?.type == "Ljava/util/ArrayList;") {
                liveArrayLists.add((insn as OneRegisterInstruction).registerA)
            }
        } else if (opcode == Opcode.IGET_OBJECT) {
            val fieldRef = (insn as? ReferenceInstruction)?.reference
                as? com.android.tools.smali.dexlib2.iface.reference.FieldReference
            if (fieldRef?.type == "Ljava/util/ArrayList;") {
                liveArrayLists.add((insn as com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction).registerA)
            }
        }
    }
    val ordered = LinkedHashSet<Int>()
    for (i in endIndex until minOf(instructions.size, endIndex + 6)) {
        val insn = instructions[i]
        if (insn.opcode == Opcode.INVOKE_DIRECT || insn.opcode == Opcode.INVOKE_DIRECT_RANGE) {
            val methodRef = (insn as? ReferenceInstruction)?.reference as? MethodReference
            if (methodRef?.definingClass == "Lcn/sharesdk/onekeyshare/themes/classic/CustomPlatformPageAdapter;" &&
                methodRef.name == "<init>"
            ) {
                methodRef.parameterTypes.forEachIndexed { idx, paramType ->
                    if (paramType.toString() == "Ljava/util/ArrayList;") {
                        val pos = idx + 1
                        val listReg = when (insn) {
                            is FiveRegisterInstruction -> when (pos) {
                                0 -> insn.registerC
                                1 -> insn.registerD
                                2 -> insn.registerE
                                3 -> insn.registerF
                                4 -> insn.registerG
                                else -> null
                            }
                            is RegisterRangeInstruction -> insn.startRegister + pos
                            else -> null
                        }
                        if (listReg != null) ordered.add(listReg)
                    }
                }
            }
        }
    }
    ordered.addAll(liveArrayLists)
    check(ordered.isNotEmpty()) {
        "No live ArrayList register found in ExportPictureView prepareAdapter before CustomPlatformPageAdapter"
    }
    return ordered.toList()
}

internal fun BytecodePatchContext.ensureExportAdapterShareHooksInjected() {
    val classes = allClasses()
    val prepMethod = toMutableMethod(MultiVersionResolvers.resolveExportPictureViewPrepareAdapter(classes))
    val prepInsns = prepMethod.implementation!!.instructions
    val alreadyPrepared = prepInsns.indices.any { idx ->
        if (idx == 0) return@any false
        val ref = (prepInsns[idx] as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
        val prev = prepInsns[idx - 1] as? com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
            ?: return@any false
        prev.narrowLiteral == OP_HIDE_EXPORT_SOCIAL_ICONS &&
            "${ref.definingClass}->${ref.name}${ref.parameterTypes.joinToString("", "(", ")")}${ref.returnType}" == TRANSLATION_ENGINE_DISPATCH
    }
    if (!alreadyPrepared) {
        val newInstanceIdx = prepInsns.indexOfFirst { insn ->
            if (insn.opcode != Opcode.NEW_INSTANCE) return@indexOfFirst false
            val typeRef = (insn as? ReferenceInstruction)?.reference as? TypeReference
            typeRef?.type == CUSTOM_PLATFORM_ADAPTER_CLASS
        }
        check(newInstanceIdx >= 0) {
            "CustomPlatformPageAdapter NEW_INSTANCE not found in ExportPictureView prepareAdapter"
        }

        val origListRegs = findExportListRegistersBefore(prepInsns, newInstanceIdx)
        val arrReg = (prepInsns[newInstanceIdx] as OneRegisterInstruction).registerA
        check(arrReg in 0..15) {
            "Expected CustomPlatformPageAdapter register < 16, got v$arrReg"
        }
        val excluded = buildList {
            add(prepMethod.thisRegisterIndex())
            addAll(origListRegs)
            add(arrReg)
        }
        val (prepTarget, scratchReg, prepInsertIdx) = resolveLowRegisterAt(
            method = prepMethod,
            insertIndex = newInstanceIdx,
            *excluded.toIntArray(),
        )
        val newInsns = prepTarget.implementation!!.instructions
        val newInstanceAfterExpandIdx = newInsns.indexOfFirst { insn ->
            if (insn.opcode != Opcode.NEW_INSTANCE) return@indexOfFirst false
            val typeRef = (insn as? ReferenceInstruction)?.reference as? TypeReference
            typeRef?.type == CUSTOM_PLATFORM_ADAPTER_CLASS
        }
        val finalListRegs = findExportListRegistersBefore(newInsns, newInstanceAfterExpandIdx)
        val finalList1Reg = finalListRegs[0]
        val finalList2Reg = finalListRegs.getOrNull(1)
        val finalThisReg = prepTarget.thisRegisterIndex()

        val thirdSlotCode = if (finalList2Reg != null) {
            """
            const/4 v$scratchReg, 2
            aput-object v$finalList2Reg, v$arrReg, v$scratchReg
            """.trimIndent()
        } else {
            ""
        }

        prepTarget.addInstructionsPreservingLabels(
            prepInsertIdx,
            """
            const/4 v$scratchReg, 3
            new-array v$arrReg, v$scratchReg, [Ljava/lang/Object;
            const/4 v$scratchReg, 0
            aput-object v$finalThisReg, v$arrReg, v$scratchReg
            const/4 v$scratchReg, 1
            aput-object v$finalList1Reg, v$arrReg, v$scratchReg
            $thirdSlotCode
            const/16 v$scratchReg, $OP_HIDE_EXPORT_SOCIAL_ICONS
            invoke-static {v$scratchReg, v$arrReg}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )
    }

    val bindMethod = customAdapterOnBindFingerprint.method
    val bindInsns = bindMethod.implementation!!.instructions
    val alreadyBound = bindInsns.any { insn ->
        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
        ref != null &&
            "${ref.definingClass}->${ref.name}${ref.parameterTypes.joinToString("", "(", ")")}${ref.returnType}" == BIND_CUSTOM_EXPORT_CLICK_SIG
    }
    if (!alreadyBound) {
        val retIdx = bindInsns.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retIdx >= 0) { "RETURN_VOID not found in CustomPlatformPageAdapter.onBindViewHolder" }
        val setTagIdx = bindInsns.findPrevOpcode(retIdx, Opcode.INVOKE_VIRTUAL, maxLookback = 4)
        check(setTagIdx >= 0) { "View.setTag not found before RETURN_VOID in CustomPlatformPageAdapter.onBindViewHolder" }
        val setTagRef = (bindInsns[setTagIdx] as? ReferenceInstruction)?.reference as? MethodReference
        check(
            setTagRef != null &&
                setTagRef.definingClass == "Landroid/view/View;" &&
                setTagRef.name == "setTag",
        ) {
            "Expected View.setTag before RETURN_VOID in CustomPlatformPageAdapter.onBindViewHolder, got $setTagRef"
        }

        val thisReg = bindMethod.thisRegisterIndex()
        val rootViewReg = invokeRegisterList(bindInsns[setTagIdx])[0]
        if (thisReg in 0..15 && rootViewReg in 0..15) {
            bindMethod.addInstructionsPreservingLabels(
                retIdx,
                "invoke-static {v$thisReg, v$rootViewReg}, $BIND_CUSTOM_EXPORT_CLICK_SIG",
            )
        } else {
            val (t1, lowThis, idx1) = resolveLowRegisterAt(bindMethod, retIdx, thisReg, rootViewReg)
            val finalThis = t1.thisRegisterIndex()
            val finalSetTagIdx = t1.implementation!!.instructions.findPrevOpcode(idx1, Opcode.INVOKE_VIRTUAL, maxLookback = 4)
            val finalRootView = invokeRegisterList(t1.implementation!!.instructions[finalSetTagIdx])[0]
            val (t2, lowRoot, idx2) = resolveLowRegisterAt(t1, idx1, lowThis, finalThis, finalRootView)
            val fThis = t2.thisRegisterIndex()
            val fSetTagIdx = t2.implementation!!.instructions.findPrevOpcode(idx2, Opcode.INVOKE_VIRTUAL, maxLookback = 4)
            val fRoot = invokeRegisterList(t2.implementation!!.instructions[fSetTagIdx])[0]
            t2.addInstructionsPreservingLabels(
                idx2,
                """
                move-object/from16 v$lowThis, v$fThis
                move-object/from16 v$lowRoot, v$fRoot
                invoke-static {v$lowThis, v$lowRoot}, $BIND_CUSTOM_EXPORT_CLICK_SIG
                """.trimIndent(),
            )
        }
    }
}

@Suppress("unused")
val hideExportSocialIconsPatch = bytecodePatch(
    name = "Hide Domestic Export Social Icons",
    description = "Hides domestic social sharing channels in ExportPictureView and PaintActivity export popups via TranslationEngine.dispatch.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val epCreateMethod = exportPictureCreateViewFingerprint.method
        val epCreateRetIdx = epCreateMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_OBJECT
        }
        check(epCreateRetIdx >= 0) { "RETURN_OBJECT not found in ExportPictureView.onCreateView" }
        injectDispatchOnObjectReg(epCreateMethod, epCreateRetIdx) { m, idx ->
            (m.implementation!!.instructions[idx] as OneRegisterInstruction).registerA
        }

        ensureExportAdapterShareHooksInjected()

        val bubbleType = MultiVersionResolvers.resolveBubblePopupClass(classes).type
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        for (method in listOf(
            MultiVersionResolvers.resolvePaintPopup1Menu(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup3Canvas(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup4Ref(paintClass, bubbleType),
        )) {
            hookPaintBubblePopupBeforeShow(
                toFingerprint(method),
                "${WORKSPACE_UI_CONTROLLER}->optimizeExportDialog(Landroid/view/View;)V",
            )
        }
    }
}
