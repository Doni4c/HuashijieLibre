package app.morphe.patches.huashijie.tier4_workflow

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

private const val TOGGLE_KEY = "libre_system_share_sheet"
private const val HANDLE_SHARE_METHOD =
    "Lapp/morphe/extension/huashijie/GlobalCompatibilityController;->handleShare(Landroid/app/Activity;Ljava/lang/String;)Z"

internal fun BytecodePatchContext.injectSystemShareGuard(method: MutableMethod) {
    val origThis = method.thisRegisterIndex()
    val origPath = origThis + 2
    val (target1, low1, idx1) = resolveLowRegisterAt(
        method = method,
        insertIndex = 0,
        origThis,
        origPath,
    )
    val thisReg = target1.thisRegisterIndex()
    val pathReg = thisReg + 2

    if (thisReg in 0..15 && pathReg in 0..15) {
        val resumeInsn: Instruction = target1.getInstruction(idx1)
        target1.addInstructionsWithLabels(
            idx1,
            """
            invoke-static {v$thisReg, v$pathReg}, $HANDLE_SHARE_METHOD
            move-result v$low1
            if-eqz v$low1, :cond_allow_share_sdk
            return-void
            """.trimIndent(),
            ExternalLabel("cond_allow_share_sdk", resumeInsn),
        )
    } else {
        val (target2, low2, idx2) = resolveLowRegisterAt(
            method = target1,
            insertIndex = idx1,
            low1,
            thisReg,
            pathReg,
        )
        val finalThis = target2.thisRegisterIndex()
        val finalPath = finalThis + 2
        val resumeInsn: Instruction = target2.getInstruction(idx2)
        target2.addInstructionsWithLabels(
            idx2,
            """
            move-object/from16 v$low1, v$finalThis
            move-object/from16 v$low2, v$finalPath
            invoke-static {v$low1, v$low2}, $HANDLE_SHARE_METHOD
            move-result v$low1
            if-eqz v$low1, :cond_allow_share_sdk
            return-void
            """.trimIndent(),
            ExternalLabel("cond_allow_share_sdk", resumeInsn),
        )
    }
}

@Suppress("unused")
val nativeAndroidShareSheetPatch = bytecodePatch(
    name = "Native Android Share Sheet",
    description = "Routes export sharing in PaintActivity to the Android System Share Sheet via Intent.createChooser instead of ShareSDK.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        val shareMethod = toMutableMethod(MultiVersionResolvers.resolveActivityShareCallback(paintClass))
        injectSystemShareGuard(shareMethod)
    }
}
