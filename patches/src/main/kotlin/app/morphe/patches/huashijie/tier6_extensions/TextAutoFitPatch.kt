package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
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
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_text_auto_fit"
private const val TRANSLATION_ENGINE_CLASS =
    "Lapp/morphe/extension/huashijie/TranslationEngine;"
private const val OP_APPLY_TEXT_AUTO_FIT = 72

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
    }
}
