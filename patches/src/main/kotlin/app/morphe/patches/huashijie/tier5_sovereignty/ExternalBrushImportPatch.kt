package app.morphe.patches.huashijie.tier5_sovereignty

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "libre_external_brush_import"
private const val LOCAL_BRUSH_CONTROLLER =
    "Lapp/morphe/extension/huashijie/LocalBrushController;"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"
private const val OP_ENSURE_BRUSH_PACKAGE_LIST = 51

private val mainActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/MainActivity;"
    },
)

@Suppress("unused")
val externalBrushImportPatch = bytecodePatch(
    name = "External Local Brush Scanning",
    description = "Scans local ASBrush directories on startup and guards loaded brush package lists for offline brush import workflows.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val mainMethod = mainActivityInitViewFingerprint.method
        val mainRetIdx = mainMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(mainRetIdx >= 0) { "RETURN_VOID not found in MainActivity.initView()V" }
        mainMethod.addInstructionsPreservingLabels(
            mainRetIdx,
            formatInvokeStaticSingle(
                "${LOCAL_BRUSH_CONTROLLER}->scanCustomBrushes(Landroid/content/Context;)Ljava/util/List;",
                mainMethod.thisRegisterIndex(),
            ),
        )

        val classes = allClasses()
        val mgrClass = MultiVersionResolvers.resolveBrushPkgManagerClass(classes)
        var method = toMutableMethod(MultiVersionResolvers.resolveBrushPkgManagerGetList(mgrClass))
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
                    invoke-static {v$opScratch, v$resolvedRetReg}, $TRANSLATION_ENGINE_DISPATCH
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
                    invoke-static {v$opScratch, v$objScratch}, $TRANSLATION_ENGINE_DISPATCH
                    move-result-object v$objScratch
                    check-cast v$objScratch, Ljava/util/List;
                    move-object/from16 v$finalRetReg, v$objScratch
                    """.trimIndent(),
                )
                method = target2
            }
        }
    }
}
