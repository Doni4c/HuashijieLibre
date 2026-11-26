package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val TOGGLE_KEY = "libre_canvas_hsj_path_row"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private fun hookPopupPathRow(method: MutableMethod) {
    val instructions = method.implementation!!.instructions
    val castIdx = instructions.indexOfFirst { inst ->
        inst.opcode == Opcode.CHECK_CAST &&
            ((inst as? ReferenceInstruction)?.reference as? TypeReference)?.type ==
            "Lcom/lihang/ShadowLayout;"
    }
    check(castIdx >= 0) {
        "ShadowLayout check cast not found in ${method.definingClass}->${method.name}"
    }
    val castReg = method.getInstruction<OneRegisterInstruction>(castIdx).registerA
    method.addInstructionsPreservingLabels(
        castIdx + 1,
        formatInvokeStaticSingle(
            "${WORKSPACE_UI_CONTROLLER}->appendCanvasHsjPathRow(Landroid/view/View;)V",
            castReg,
        ),
    )
}

@Suppress("unused")
val canvasPopupPathRowPatch = bytecodePatch(
    name = "Canvas Popup HSJ Path Row",
    description = "Adds the active .hsj draft folder path inside the canvas settings tab with tap-to-copy clipboard support.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val fileHelper = MultiVersionResolvers.resolveFileHelperClass(classes)
        registerResolvedTarget("FileHelper.class", fileHelper.type.toJavaClassName())
        registerResolvedTarget(
            "FileHelper.getDraftDir",
            MultiVersionResolvers.resolveFileHelperGetDraftDir(fileHelper).name,
        )
        val bubbleType = MultiVersionResolvers.resolveBubblePopupClass(classes).type
        val paintClass = MultiVersionResolvers.resolvePaintActivityClass(classes)
        hookPopupPathRow(toMutableMethod(MultiVersionResolvers.resolvePaintPopup1Menu(paintClass, bubbleType)))
        hookPopupPathRow(toMutableMethod(MultiVersionResolvers.resolvePaintPopup2Brush(paintClass, bubbleType)))
    }
}
