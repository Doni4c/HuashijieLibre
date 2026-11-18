package app.morphe.patches.huashijie.tier5_sovereignty

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "libre_offline_user_stub"
private const val SETUP_OFFLINE_PROFILE =
    "Lapp/morphe/extension/huashijie/OfflineAccountController;->setupOfflineUserStub()V"

private val flashOnCreateFingerprint = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/FlashActivity;"
    },
)

private val mainActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/MainActivity;"
    },
)

@Suppress("unused")
val localArtistProfilePatch = bytecodePatch(
    name = "Local Artist Profile",
    description = "Maintains an offline artist profile in memory so local tools such as PDF export work smoothly without domestic phone login prompts.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val flashMethod = flashOnCreateFingerprint.method
        val flashRetIdx = flashMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(flashRetIdx >= 0) { "RETURN_VOID not found in FlashActivity.onCreate" }
        flashMethod.addInstructionsPreservingLabels(
            flashRetIdx,
            "invoke-static {}, $SETUP_OFFLINE_PROFILE",
        )

        val mainMethod = mainActivityInitViewFingerprint.method
        mainMethod.addInstructionsPreservingLabels(
            0,
            "invoke-static {}, $SETUP_OFFLINE_PROFILE",
        )
    }
}
