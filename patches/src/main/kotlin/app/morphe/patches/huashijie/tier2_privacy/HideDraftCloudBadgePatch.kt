package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "pure_studio_hide_draft_cloud_tick"
private const val WORKSPACE_UI_CONTROLLER =
    "Lapp/morphe/extension/huashijie/WorkspaceUiController;"

private val draftAdapterBindFingerprint = Fingerprint(
    name = "onBindViewHolder",
    returnType = "V",
    parameters = listOf("Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;", "I"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/adapter/DraftAdapter;"
    },
)

private val draftAdapter2BindFingerprint = Fingerprint(
    name = "onBindViewHolder",
    returnType = "V",
    parameters = listOf("Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;", "I"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/adapter/DraftAdapter2;"
    },
)

@Suppress("unused")
val hideDraftCloudBadgePatch = bytecodePatch(
    name = "Hide Draft Cloud Badge",
    description = "Hides cloud sync status icons and progress bars on draft thumbnails.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        listOf(draftAdapterBindFingerprint, draftAdapter2BindFingerprint).forEach { fp ->
            val method = fp.method
            val instructions = method.implementation!!.instructions
            val retIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            check(retIdx >= 0) { "RETURN_VOID not found in ${method.definingClass}->onBindViewHolder" }

            val holderReg = method.thisRegisterIndex() + 1
            val invokeHook = formatInvokeStaticSingle(
                "${WORKSPACE_UI_CONTROLLER}->handleDraftItemBinding(Ljava/lang/Object;)V",
                holderReg,
            )
            method.addInstructionsPreservingLabels(retIdx, invokeHook)
        }
    }
}
