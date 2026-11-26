package app.morphe.patches.huashijie.tier6_extensions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "storage_transparency_mode"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private val draftStorageInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/DraftStorageSettingActivity;"
    },
)

@Suppress("unused")
val hsjFilePathDisplayPatch = bytecodePatch(
    name = "HSJ File Path Display",
    description = "Shows the active local .hsj draft and export folder paths inside Draft Storage Settings.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val fileHelper = MultiVersionResolvers.resolveFileHelperClass(allClasses())
        registerResolvedTarget("FileHelper.class", fileHelper.type.toJavaClassName())
        registerResolvedTarget(
            "FileHelper.getDraftDir",
            MultiVersionResolvers.resolveFileHelperGetDraftDir(fileHelper).name,
        )
        registerResolvedTarget(
            "FileHelper.getExportDraftsDir",
            MultiVersionResolvers.resolveFileHelperGetExportDraftsDir(fileHelper).name,
        )

        val storageMethod = draftStorageInitViewFingerprint.method
        val retIdx = storageMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(retIdx >= 0) { "RETURN_VOID not found in DraftStorageSettingActivity.initView()V" }
        storageMethod.addInstructionsPreservingLabels(
            retIdx,
            formatInvokeStaticSingle(
                "${WORKSPACE_UI_CONTROLLER}->onDraftStorageInitView(Landroid/app/Activity;)V",
                storageMethod.thisRegisterIndex(),
            ),
        )
    }
}
