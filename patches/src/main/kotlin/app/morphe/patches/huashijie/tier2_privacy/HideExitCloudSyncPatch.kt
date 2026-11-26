package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.findMoveResultObjectAfter
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "pure_studio_hide_canvas_exit_cloud"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private val paintOnBackPressedFingerprint = Fingerprint(
    name = "onBackPressed",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/PaintActivity;"
    },
)

@Suppress("unused")
val hideExitCloudSyncPatch = bytecodePatch(
    name = "Hide Exit Cloud Sync Option",
    description = "Hides cloud upload options in the canvas exit dialog while keeping local save and discard actions.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val prefUtil = MultiVersionResolvers.resolvePrefUtilClass(allClasses())
        registerResolvedTarget("PrefUtil.class", prefUtil.type.toJavaClassName())
        registerResolvedTarget(
            "PrefUtil.putBoolean",
            MultiVersionResolvers.resolvePrefUtilPutBoolean(prefUtil).name,
        )

        val method = paintOnBackPressedFingerprint.method
        val instructions = method.implementation!!.instructions
        val inflateIdx = instructions.indexOfFirst { inst ->
            if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) {
                return@indexOfFirst false
            }
            val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
            ref != null &&
                ref.definingClass == "Landroid/view/LayoutInflater;" &&
                ref.name == "inflate" &&
                ref.returnType == "Landroid/view/View;"
        }
        val (moveResultIdx, dialogViewReg) = checkNotNull(instructions.findMoveResultObjectAfter(inflateIdx)) {
            "Expected MOVE_RESULT_OBJECT after LayoutInflater.inflate in PaintActivity.onBackPressed()V"
        }

        val invokeOptimize = formatInvokeStaticSingle(
            "${WORKSPACE_UI_CONTROLLER}->optimizeCanvasExitDialog(Landroid/view/View;)V",
            dialogViewReg,
        )
        method.addInstructionsPreservingLabels(moveResultIdx + 1, invokeOptimize)
    }
}
