package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleClass
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleMethod
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.findMoveResultObjectAfter
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.hookBottomSheetOnStartDispatch
import app.morphe.patches.huashijie.core.hookPaintBubblePopupDispatch
import app.morphe.patches.huashijie.core.hookPopupShowAsDropDownDispatch
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_text_auto_fit"
private const val TRANSLATION_ENGINE_CLASS =
    "Lapp/morphe/extension/huashijie/TranslationEngine;"
private const val DISPATCH_SIGNATURE =
    "${TRANSLATION_ENGINE_CLASS}->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_APPLY_TEXT_AUTO_FIT = 72
private const val OP_ON_TOGGLE_SWITCH_UPDATE = 78
private const val OP_DRAW_TOGGLE_SWITCH_LABELS = 79
private const val OP_DRAW_HSV_NAME = 80

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
        classDef.type == "Lnet/huanci/hsjpro/dialog/CommonDialog;"
    },
)

private val commonDialogOnStartFingerprint = Fingerprint(
    name = "onStart",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/dialog/CommonDialog;"
    },
)

@Suppress("unused")
val textAutoFitPatch = bytecodePatch(
    name = "Narrow Button Text Auto Fit",
    description = "Scales text sizes and handles wrapping cleanly on narrow tabs, buttons, and dialogs for longer translated labels across non-Chinese scripts.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val bubbleClass = MultiVersionResolvers.resolveBubblePopupClass(classes)
        val bubbleType = bubbleClass.type
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)

        for (method in listOf(
            MultiVersionResolvers.resolvePaintPopup1Menu(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup2Brush(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup3Canvas(paintClass, bubbleType),
            MultiVersionResolvers.resolvePaintPopup4Ref(paintClass, bubbleType),
        )) {
            hookPaintBubblePopupDispatch(toFingerprint(method), OP_APPLY_TEXT_AUTO_FIT)
        }

        for (fp in listOf(baseActivityOnCreateFingerprint, baseActivityOnResumeFingerprint)) {
            val method = fp.method
            val retIdx = method.implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }
            check(retIdx >= 0) {
                "RETURN_VOID not found in ${method.definingClass}->${method.name}"
            }
            method.addInstructionsPreservingLabels(
                retIdx,
                formatInvokeStaticSingle(
                    "${TRANSLATION_ENGINE_CLASS}->attachTextAutoFitToActivity(Landroid/app/Activity;)V",
                    method.thisRegisterIndex(),
                ),
            )
        }

        for (fp in listOf(commonDialogShowFingerprint, commonDialogOnStartFingerprint)) {
            val method = fp.method
            method.addInstructionsPreservingLabels(
                0,
                formatInvokeStaticSingle(
                    "${TRANSLATION_ENGINE_CLASS}->onDialogAutoFitPreShow(Landroid/app/Dialog;)V",
                    method.thisRegisterIndex(),
                ),
            )
        }

        run {
            val showCustomFp = toFingerprint(MultiVersionResolvers.resolveBubblePopupShowAtLocationCustom(bubbleClass))
            val method = showCustomFp.method
            val instrs = method.implementation!!.instructions
            val getContentIdx = instrs.indexOfFirst { inst ->
                val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
                ref != null &&
                    ref.name == "getContentView" &&
                    ref.returnType == "Landroid/view/View;" &&
                    ref.parameterTypes.isEmpty()
            }
            val (moveIdx, _) = checkNotNull(instrs.findMoveResultObjectAfter(getContentIdx)) {
                "Expected MOVE_RESULT_OBJECT after getContentView in BubblePopupWindow"
            }
            emitTranslationDispatch(
                method = method,
                insertIndex = moveIdx + 1,
                opCode = OP_APPLY_TEXT_AUTO_FIT,
                preserveLabels = true,
            ) { m, _ ->
                m.thisRegisterIndex()
            }
        }

        hookBottomSheetOnStartDispatch(OP_APPLY_TEXT_AUTO_FIT)
        val searchHelperClass = MultiVersionResolvers.resolveSearchHelperClass(classes)
        val searchDropdown = MultiVersionResolvers.resolveSearchHelperShowDropdown(searchHelperClass)
        val fastKeyDropdown = MultiVersionResolvers.resolveFastKeyFragmentShowDropdown(classes)
        for (dropdownMethod in listOf(searchDropdown, fastKeyDropdown)) {
            hookPopupShowAsDropDownDispatch(toFingerprint(dropdownMethod), OP_APPLY_TEXT_AUTO_FIT)
        }

        // Hook ToggleSwitchView.setNames and onDraw through TranslationEngine.dispatch (classes4 delta = 0).
        val toggleSwitchClass = classes.resolveSingleClass("ToggleSwitchView") {
            it.type == "Lnet/huanci/hsjpro/views/ToggleSwitchView;"
        }

        val setNamesMethod = toggleSwitchClass.resolveSingleMethod("ToggleSwitchView.setNames") { m ->
            m.name == "setNames" &&
                m.returnType == "V" &&
                m.parameters.map { it.toString() } == listOf("Ljava/util/ArrayList;")
        }
        var mutableSetNames = toMutableMethod(setNamesMethod)
        val setNamesRets = mutableSetNames.implementation!!.instructions.indices
            .filter { mutableSetNames.implementation!!.instructions[it].opcode == Opcode.RETURN_VOID }
            .reversed()
        check(setNamesRets.isNotEmpty()) { "RETURN_VOID not found in ToggleSwitchView.setNames" }
        for (retIdx in setNamesRets) {
            mutableSetNames = emitTranslationDispatch(
                method = mutableSetNames,
                insertIndex = retIdx,
                opCode = OP_ON_TOGGLE_SWITCH_UPDATE,
                preserveLabels = true,
            ) { m, _ -> m.thisRegisterIndex() }
        }

        val onDrawMethod = toggleSwitchClass.resolveSingleMethod("ToggleSwitchView.onDraw") { m ->
            m.name == "onDraw" &&
                m.returnType == "V" &&
                m.parameters.map { it.toString() } == listOf("Landroid/graphics/Canvas;")
        }
        var targetOnDraw = toMutableMethod(onDrawMethod)
        fun findToggleTextPaintIdx(instrs: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>): Int {
            fun hasGetFontMetricsSoon(idx: Int, paintReg: Int): Boolean =
                (idx + 1..minOf(instrs.lastIndex, idx + 4)).any { j ->
                    val mRef = (instrs[j] as? ReferenceInstruction)?.reference as? MethodReference
                    mRef != null &&
                        mRef.definingClass == "Landroid/graphics/Paint;" &&
                        mRef.name == "getFontMetrics" &&
                        app.morphe.patches.huashijie.core.invokeRegisterList(instrs[j]).firstOrNull() == paintReg
                }

            val namedIdx = instrs.indices.firstOrNull { i ->
                val inst = instrs[i]
                if (inst.opcode != Opcode.IGET_OBJECT || inst !is TwoRegisterInstruction) return@firstOrNull false
                val fRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@firstOrNull false
                fRef.definingClass == toggleSwitchClass.type &&
                    fRef.name == "textPaint" &&
                    fRef.type == "Landroid/graphics/Paint;" &&
                    hasGetFontMetricsSoon(i, inst.registerA)
            }
            if (namedIdx != null) return namedIdx

            val structuralMatches = instrs.indices.filter { i ->
                val inst = instrs[i]
                if (inst.opcode != Opcode.IGET_OBJECT || inst !is TwoRegisterInstruction) return@filter false
                val fRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@filter false
                fRef.definingClass == toggleSwitchClass.type &&
                    fRef.type == "Landroid/graphics/Paint;" &&
                    hasGetFontMetricsSoon(i, inst.registerA)
            }
            return structuralMatches.singleOrNull() ?: -1
        }
        fun findToggleShowLineIdx(instrs: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>, afterIdx: Int): Int {
            val namedIdx = ((afterIdx + 1) until instrs.size).firstOrNull { i ->
                val inst = instrs[i]
                if (inst.opcode != Opcode.IGET_BOOLEAN) return@firstOrNull false
                val fRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@firstOrNull false
                fRef.definingClass == toggleSwitchClass.type &&
                    fRef.name == "showLine" &&
                    fRef.type == "Z"
            }
            if (namedIdx != null) return namedIdx

            val structuralMatches = ((afterIdx + 1) until instrs.size).filter { i ->
                val inst = instrs[i]
                if (inst.opcode != Opcode.IGET_BOOLEAN || inst !is TwoRegisterInstruction) return@filter false
                val fRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return@filter false
                if (fRef.definingClass != toggleSwitchClass.type || fRef.type != "Z") return@filter false
                val boolReg = inst.registerA
                (i + 1..minOf(instrs.lastIndex, i + 2)).any { j ->
                    instrs[j].opcode == Opcode.IF_EQZ &&
                        (instrs[j] as? OneRegisterInstruction)?.registerA == boolReg
                }
            }
            return structuralMatches.singleOrNull() ?: -1
        }

        val initTextPaintIdx = findToggleTextPaintIdx(targetOnDraw.implementation!!.instructions)
        check(initTextPaintIdx >= 0) { "ToggleSwitchView.onDraw textPaint anchor not found" }
        val origThisReg = targetOnDraw.thisRegisterIndex()
        val origCanvasReg = origThisReg + 1
        val (m1, arrReg, idx1) = resolveLowRegisterAt(targetOnDraw, initTextPaintIdx, origThisReg, origCanvasReg)
        val m1ThisReg = m1.thisRegisterIndex()
        val m1CanvasReg = m1ThisReg + 1
        val (m2, opReg, idx2) = resolveLowRegisterAt(m1, idx1, arrReg, m1ThisReg, m1CanvasReg)
        val finalThisReg = m2.thisRegisterIndex()
        val finalCanvasReg = finalThisReg + 1
        check(finalThisReg in 0..15 && finalCanvasReg in 0..15) {
            "Expected low parameter registers in ToggleSwitchView.onDraw ($finalThisReg, $finalCanvasReg)"
        }
        val finalShowLineIdx = findToggleShowLineIdx(m2.implementation!!.instructions, idx2)
        check(finalShowLineIdx > idx2) { "ToggleSwitchView.onDraw showLine anchor not found" }
        val showLineInst = m2.getInstruction(finalShowLineIdx)
        m2.addInstructionsPreservingLabels(idx2, "nop")
        m2.addInstructionsWithLabels(
            idx2 + 1,
            """
            const/4 v$opReg, 2
            new-array v$arrReg, v$opReg, [Ljava/lang/Object;
            const/4 v$opReg, 0
            aput-object v$finalThisReg, v$arrReg, v$opReg
            const/4 v$opReg, 1
            aput-object v$finalCanvasReg, v$arrReg, v$opReg
            const/16 v$opReg, $OP_DRAW_TOGGLE_SWITCH_LABELS
            invoke-static {v$opReg, v$arrReg}, $DISPATCH_SIGNATURE
            move-result-object v$opReg
            if-nez v$opReg, :libre_toggle_show_line
            """.trimIndent(),
            ExternalLabel("libre_toggle_show_line", showLineInst),
        )

        // Hook HSVSeekbarView.drawName(Canvas) through TranslationEngine.dispatch (classes4 delta = 0).
        val hsvClass = classes.resolveSingleClass("HSVSeekbarView") {
            it.type == "Lnet/huanci/hsjpro/paint/views/HSVSeekbarView;"
        }
        val drawNameMethod = hsvClass.resolveSingleMethod("HSVSeekbarView.drawName") { m ->
            m.name == "drawName" &&
                m.returnType == "V" &&
                m.parameters.map { it.toString() } == listOf("Landroid/graphics/Canvas;")
        }
        val mutableDrawName = toMutableMethod(drawNameMethod)
        val hsvThisOrig = mutableDrawName.thisRegisterIndex()
        val hsvCanvasOrig = hsvThisOrig + 1
        val (hm1, hsvArrReg, hIdx1) = resolveLowRegisterAt(mutableDrawName, 0, hsvThisOrig, hsvCanvasOrig)
        val hm1This = hm1.thisRegisterIndex()
        val hm1Canvas = hm1This + 1
        val (hm2, hsvOpReg, hIdx2) = resolveLowRegisterAt(hm1, hIdx1, hsvArrReg, hm1This, hm1Canvas)
        val finalHsvThis = hm2.thisRegisterIndex()
        val finalHsvCanvas = finalHsvThis + 1
        check(finalHsvThis in 0..15 && finalHsvCanvas in 0..15) {
            "Expected low parameter registers in HSVSeekbarView.drawName ($finalHsvThis, $finalHsvCanvas)"
        }
        val vanillaDrawNameInst = hm2.getInstruction(hIdx2)
        hm2.addInstructionsWithLabels(
            hIdx2,
            """
            const/4 v$hsvOpReg, 2
            new-array v$hsvArrReg, v$hsvOpReg, [Ljava/lang/Object;
            const/4 v$hsvOpReg, 0
            aput-object v$finalHsvThis, v$hsvArrReg, v$hsvOpReg
            const/4 v$hsvOpReg, 1
            aput-object v$finalHsvCanvas, v$hsvArrReg, v$hsvOpReg
            const/16 v$hsvOpReg, $OP_DRAW_HSV_NAME
            invoke-static {v$hsvOpReg, v$hsvArrReg}, $DISPATCH_SIGNATURE
            move-result-object v$hsvOpReg
            if-eqz v$hsvOpReg, :libre_vanilla_hsv_name
            return-void
            """.trimIndent(),
            ExternalLabel("libre_vanilla_hsv_name", vanillaDrawNameInst),
        )
    }
}
