package app.morphe.patches.huashijie.tier3_ui_polish

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val TOGGLE_KEY = "pure_studio_hide_timelapse_export_social"
private const val OP_HIDE_TIMELAPSE_EXPORT_SOCIAL = 17

private val exportPicture3CreateViewFingerprint = Fingerprint(
    name = "onCreateView",
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/views/ExportPictureView3;"
    },
)

@Suppress("unused")
val hideTimelapseSocialIconsPatch = bytecodePatch(
    name = "Hide Timelapse Export Social Icons",
    description = "Hides domestic social channel buttons in the timelapse export dialog while keeping save and quality options.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val ep3BindMethod = toMutableMethod(MultiVersionResolvers.resolveExportPictureView3BindView(classes))
        val ep3BindRetIdx = ep3BindMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(ep3BindRetIdx >= 0) { "RETURN_VOID not found in ExportPictureView3 bindView" }
        emitTranslationDispatch(
            method = ep3BindMethod,
            insertIndex = ep3BindRetIdx,
            opCode = OP_HIDE_TIMELAPSE_EXPORT_SOCIAL,
            preserveLabels = true,
        ) { m, _ ->
            m.thisRegisterIndex() + 1
        }

        val ep3CreateMethod = exportPicture3CreateViewFingerprint.method
        val ep3CreateRetIdx = ep3CreateMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_OBJECT
        }
        check(ep3CreateRetIdx >= 0) { "RETURN_OBJECT not found in ExportPictureView3.onCreateView" }
        emitTranslationDispatch(
            method = ep3CreateMethod,
            insertIndex = ep3CreateRetIdx,
            opCode = OP_HIDE_TIMELAPSE_EXPORT_SOCIAL,
            preserveLabels = true,
        ) { m, idx ->
            (m.implementation!!.instructions[idx] as OneRegisterInstruction).registerA
        }
    }
}
