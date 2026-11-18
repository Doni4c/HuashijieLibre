package app.morphe.patches.huashijie.tier3_ui_polish

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "pure_studio_hide_brush_library_shop"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_HIDE_BRUSH_LIBRARY_SHOP = 16

@Suppress("unused")
val hideBrushLibraryShopPatch = bytecodePatch(
    name = "Hide Brush Library Shop",
    description = "Hides the online brush shop button inside the full Brush Library modal independently from the canvas pen picker.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val chooseBrushClass = MultiVersionResolvers.resolveChooseBrushFragmentClass(classes)
        val chooseBrushMethod = toMutableMethod(MultiVersionResolvers.resolveChooseBrushCreateView(chooseBrushClass))
        val chooseInsns = chooseBrushMethod.implementation!!.instructions
        val retObjIdx = chooseInsns.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        check(retObjIdx >= 0) { "RETURN_OBJECT not found in ChooseBrushFragment createView" }
        val rootViewReg = (chooseInsns[retObjIdx] as OneRegisterInstruction).registerA
        val (targetChoose, chooseScratch, insertChooseIdx) = resolveLowRegisterAt(
            method = chooseBrushMethod,
            insertIndex = retObjIdx,
            rootViewReg,
        )
        targetChoose.addInstructionsPreservingLabels(
            insertChooseIdx,
            """
            const/16 v$chooseScratch, $OP_HIDE_BRUSH_LIBRARY_SHOP
            invoke-static {v$chooseScratch, v$rootViewReg}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )
    }
}
