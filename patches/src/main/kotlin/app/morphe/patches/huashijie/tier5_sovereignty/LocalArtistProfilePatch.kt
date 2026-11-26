package app.morphe.patches.huashijie.tier5_sovereignty

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.exactClassMethodFingerprint
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "libre_offline_user_stub"
private const val OP_SYNC_DRAWER_ACCOUNT_VISIBILITY = 52
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

private val mainDrawerLayoutShowFingerprint = exactClassMethodFingerprint(
    "Lnet/huanci/hsjpro/views/MainDrawerLayout;",
    "show",
    "V",
    "I",
)

@Suppress("unused")
val localArtistProfilePatch = bytecodePatch(
    name = "Local Artist Profile",
    description = "Provides an offline profile in memory when signed out so login-gated local tools such as PDF export work without signing in.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val globalData = MultiVersionResolvers.resolveGlobalDataClass(classes)
        registerResolvedTarget("GlobalData.class", globalData.type.toJavaClassName())
        registerResolvedTarget(
            "GlobalData.userField",
            MultiVersionResolvers.resolveGlobalDataUserField(globalData).name,
        )

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
        val mainRetIdx = mainMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(mainRetIdx >= 0) { "RETURN_VOID not found in MainActivity.initView" }
        emitTranslationDispatch(
            method = mainMethod,
            insertIndex = mainRetIdx,
            opCode = OP_SYNC_DRAWER_ACCOUNT_VISIBILITY,
        ) { m, _ -> m.thisRegisterIndex() }

        val drawerShow = mainDrawerLayoutShowFingerprint.method
        val drawerRetIdx = drawerShow.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(drawerRetIdx >= 0) { "RETURN_VOID not found in MainDrawerLayout.show(I)V" }
        emitTranslationDispatch(
            method = drawerShow,
            insertIndex = drawerRetIdx,
            opCode = OP_SYNC_DRAWER_ACCOUNT_VISIBILITY,
        ) { m, _ -> m.thisRegisterIndex() }
    }
}
