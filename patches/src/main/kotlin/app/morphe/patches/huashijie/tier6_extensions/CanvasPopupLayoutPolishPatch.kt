package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleClass
import app.morphe.patches.huashijie.core.MultiVersionResolvers.resolveSingleMethod
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toFingerprint
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.findMoveResultObjectAfter
import app.morphe.patches.huashijie.core.hookBottomSheetOnStartDispatch
import app.morphe.patches.huashijie.core.hookPaintBubblePopupBeforeShow
import app.morphe.patches.huashijie.core.hookPopupShowAsDropDownDispatch
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "libre_canvas_popup_polish"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"
private const val OP_CANVAS_POPUP_POLISH = 73
private const val OP_ON_LIUYEBI_INIT = 77

@Suppress("unused")
val canvasPopupLayoutPolishPatch = bytecodePatch(
    name = "Canvas Popup Geometric Polish",
    description = "Adjusts spacing in canvas popups and reference windows and adapts the left tool grid between portrait and landscape columns.",
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
            hookPaintBubblePopupBeforeShow(
                toFingerprint(method),
                "${WORKSPACE_UI_CONTROLLER}->optimizeCanvasSettingPopup(Landroid/view/View;)V",
            )
        }

        val showCustomFp = toFingerprint(MultiVersionResolvers.resolveBubblePopupShowAtLocationCustom(bubbleClass))
        val showCustomMethod = showCustomFp.method
        val instrs = showCustomMethod.implementation!!.instructions
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
            method = showCustomMethod,
            insertIndex = moveIdx + 1,
            opCode = OP_CANVAS_POPUP_POLISH,
            preserveLabels = true,
        ) { m, _ ->
            m.thisRegisterIndex()
        }

        hookBottomSheetOnStartDispatch(OP_CANVAS_POPUP_POLISH)
        val searchHelperClass = MultiVersionResolvers.resolveSearchHelperClass(classes)
        val searchDropdown = MultiVersionResolvers.resolveSearchHelperShowDropdown(searchHelperClass)
        val fastKeyDropdown = MultiVersionResolvers.resolveFastKeyFragmentShowDropdown(classes)
        for (dropdownMethod in listOf(searchDropdown, fastKeyDropdown)) {
            hookPopupShowAsDropDownDispatch(toFingerprint(dropdownMethod), OP_CANVAS_POPUP_POLISH)
        }

        // Hook LiuyebiSettingView.initData(Z)V so switching Bucket <-> Gradient dynamically
        // recomputes bar width before Frame 0 instead of resetting to vanilla 210dp.
        val liuyebiClass = classes.resolveSingleClass("LiuyebiSettingView") {
            it.type == "Lnet/huanci/hsjpro/paint/views/LiuyebiSettingView;"
        }
        val initDataMethod = liuyebiClass.resolveSingleMethod("LiuyebiSettingView.initData(Z)V") { m ->
            m.name == "initData" &&
                m.returnType == "V" &&
                m.parameters.map { it.toString() } == listOf("Z")
        }
        var mutableInitData = toMutableMethod(initDataMethod)
        val retIndices = mutableInitData.implementation!!.instructions.indices
            .filter { mutableInitData.implementation!!.instructions[it].opcode == Opcode.RETURN_VOID }
            .reversed()
        check(retIndices.isNotEmpty()) { "RETURN_VOID not found in LiuyebiSettingView.initData(Z)V" }
        for (retIdx in retIndices) {
            mutableInitData = emitTranslationDispatch(
                method = mutableInitData,
                insertIndex = retIdx,
                opCode = OP_ON_LIUYEBI_INIT,
                preserveLabels = true,
            ) { m, _ -> m.thisRegisterIndex() }
        }
    }
}
