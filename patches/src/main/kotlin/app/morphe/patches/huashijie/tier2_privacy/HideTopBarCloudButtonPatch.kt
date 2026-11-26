package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
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
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "pure_studio_hide_topbar_cloud"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"
private const val TRANSLATION_ENGINE_DISPATCH =
    "Lapp/morphe/extension/huashijie/TranslationEngine;->dispatch(ILjava/lang/Object;)Ljava/lang/Object;"

private const val OP_HIDE_TOPBAR_CLOUD_SYNC = 10
private const val OP_IS_HIDE_TOPBAR_CLOUD = 11

private val mainActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/MainActivity;"
    },
)

private val mainFragment2OnClickFingerprint = Fingerprint(
    name = "onClick",
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/fragments/MainFragment2;"
    },
)

@Suppress("unused")
val hideTopBarCloudButtonPatch = bytecodePatch(
    name = "Hide Top Bar Cloud Button",
    description = "Hides the cloud sync icon on the gallery top bar and blocks accidental taps.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val mainInit = mainActivityInitViewFingerprint.method
        val mainInitInsns = mainInit.implementation!!.instructions
        val mainRetIdx = mainInitInsns.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        check(mainRetIdx >= 0) { "RETURN_VOID not found in MainActivity.initView()V" }
        val mainThisReg = mainInit.thisRegisterIndex()
        mainInit.addInstructionsPreservingLabels(
            mainRetIdx,
            formatInvokeStaticSingle(
                "${WORKSPACE_UI_CONTROLLER}->hideCloudSyncEntry(Landroid/app/Activity;)V",
                mainThisReg,
            ),
        )

        val classes = allClasses()
        val mainFragment2Class = MultiVersionResolvers.resolveMainFragment2Class(classes)
        val appModuleSwitchClass = MultiVersionResolvers.resolveAppModuleSwitchHelperClass(classes)
        val appModuleSwitchSingleton =
            MultiVersionResolvers.resolveAppModuleSwitchSingletonField(appModuleSwitchClass)

        val fragBind = toMutableMethod(MultiVersionResolvers.resolveMainFragment2BindView(mainFragment2Class))
        val fragBindInsns = fragBind.implementation!!.instructions
        val postCallIdx = fragBindInsns.indexOfFirst { inst ->
            inst.opcode == Opcode.INVOKE_VIRTUAL &&
                (inst as? ReferenceInstruction)?.reference
                    .let { it as? MethodReference }
                    ?.let {
                        it.definingClass == "Landroid/view/View;" &&
                            it.name == "post"
                    } == true
        }
        check(postCallIdx >= 0) { "View.post not found in MainFragment2.bindView" }
        val fragViewReg = fragBind.thisRegisterIndex() + 1
        val (targetFragBind, fragScratch, insertFragIdx) = resolveLowRegisterAt(
            method = fragBind,
            insertIndex = postCallIdx + 1,
            fragViewReg,
        )
        val shiftedFragViewReg = targetFragBind.thisRegisterIndex() + 1
        targetFragBind.addInstructionsPreservingLabels(
            insertFragIdx,
            """
            const/16 v$fragScratch, $OP_HIDE_TOPBAR_CLOUD_SYNC
            invoke-static {v$fragScratch, v$shiftedFragViewReg}, $TRANSLATION_ENGINE_DISPATCH
            """.trimIndent(),
        )

        val updateCloud = toMutableMethod(MultiVersionResolvers.resolveMainFragment2UpdateCloud(mainFragment2Class))
        val updateInsns = updateCloud.implementation!!.instructions
        val visCallIndices = updateInsns.indices.filter { idx ->
            val inst = updateInsns[idx]
            inst.opcode == Opcode.INVOKE_VIRTUAL &&
                (inst as? ReferenceInstruction)?.reference
                    .let { it as? MethodReference }
                    ?.let {
                        it.definingClass == "Landroid/widget/ImageView;" &&
                            it.name == "setVisibility"
                    } == true
        }
        check(visCallIndices.isNotEmpty()) { "ImageView.setVisibility not found in MainFragment2.updateCloud" }
        var currentUpdateMethod = updateCloud
        for (visCallIdx in visCallIndices.reversed()) {
            val origVisInst = currentUpdateMethod.implementation!!.instructions[visCallIdx] as FiveRegisterInstruction
            val (targetUpdate, updateScratch, shiftedVisIdx) = resolveLowRegisterAt(
                method = currentUpdateMethod,
                insertIndex = visCallIdx,
                origVisInst.registerC,
                origVisInst.registerD,
            )
            val shiftedVisInst = targetUpdate.implementation!!.instructions[shiftedVisIdx] as FiveRegisterInstruction
            val viewReg = shiftedVisInst.registerC
            targetUpdate.addInstructionsWithLabels(
                shiftedVisIdx,
                """
                const/16 v$updateScratch, $OP_HIDE_TOPBAR_CLOUD_SYNC
                invoke-static {v$updateScratch, v$viewReg}, $TRANSLATION_ENGINE_DISPATCH
                move-result-object v$updateScratch
                if-nez v$updateScratch, :cond_skip_cloud_vis
                """.trimIndent(),
                ExternalLabel("cond_skip_cloud_vis", targetUpdate.getInstruction(shiftedVisIdx + 1)),
            )
            currentUpdateMethod = targetUpdate
        }

        val onClickMethod = mainFragment2OnClickFingerprint.method
        val onClickInsns = onClickMethod.implementation!!.instructions
        val cloudClickIdx = onClickInsns.indexOfFirst { inst ->
            inst.opcode == Opcode.SGET_OBJECT &&
                (inst as? ReferenceInstruction)?.reference
                    .let { it as? FieldReference }
                    ?.let {
                        it.definingClass == appModuleSwitchSingleton.definingClass &&
                            it.name == appModuleSwitchSingleton.name
                    } == true
        }
        check(cloudClickIdx >= 0) { "Cloud click case not found in MainFragment2.onClick" }
        val origOnClickThis = onClickMethod.thisRegisterIndex()
        val (targetOnClick, clickScratch, insertClickIdx) = resolveLowRegisterAt(
            method = onClickMethod,
            insertIndex = cloudClickIdx,
            origOnClickThis,
        )
        val newOnClickThis = targetOnClick.thisRegisterIndex()
        targetOnClick.addInstructionsPreservingLabels(insertClickIdx, "nop")
        targetOnClick.addInstructionsWithLabels(
            insertClickIdx + 1,
            """
            const/16 v$clickScratch, $OP_IS_HIDE_TOPBAR_CLOUD
            invoke-static {v$clickScratch, v$newOnClickThis}, $TRANSLATION_ENGINE_DISPATCH
            move-result-object v$clickScratch
            if-eqz v$clickScratch, :cond_allow_cloud_click
            return-void
            """.trimIndent(),
            ExternalLabel("cond_allow_cloud_click", targetOnClick.getInstruction(insertClickIdx + 1)),
        )
    }
}
