package app.morphe.patches.huashijie.tier3_ui_polish

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "pure_studio_hide_brush_shop"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_HIDE_BRUSH_SHOP = 12

private val selectPenOnFinishInflateFingerprint = Fingerprint(
    name = "onFinishInflate",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/paint/views/SelectPenView;"
    },
)

@Suppress("unused")
val hideBrushShopPatch = bytecodePatch(
    name = "Hide Online Brush Shop",
    description = "Hides the online store button in the quick brush selector while keeping all local brushes.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val selectPenMethod = selectPenOnFinishInflateFingerprint.method
        val selectInsns = selectPenMethod.implementation!!.instructions
        val retVoidIdx = selectInsns.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(retVoidIdx >= 0) { "RETURN_VOID not found in SelectPenView.onFinishInflate" }
        val origPenThis = selectPenMethod.thisRegisterIndex()
        val (targetPen, penScratch, insertPenIdx) = resolveLowRegisterAt(
            method = selectPenMethod,
            insertIndex = retVoidIdx,
            origPenThis,
        )
        val newPenThis = targetPen.thisRegisterIndex()
        targetPen.addInstructionsPreservingLabels(
            insertPenIdx,
            """
            const/16 v$penScratch, $OP_HIDE_BRUSH_SHOP
            invoke-static {v$penScratch, v$newPenThis}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )
    }
}
