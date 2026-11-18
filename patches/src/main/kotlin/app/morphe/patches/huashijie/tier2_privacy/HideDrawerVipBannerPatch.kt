package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.emitTranslationDispatch
import app.morphe.patches.huashijie.core.exactClassMethodFingerprint
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "pure_studio_hide_drawer_vip"
private const val OP_HIDE_VIP_DRAWER = 15

private val mainDrawerLayoutShowFingerprint = exactClassMethodFingerprint(
    "Lnet/huanci/hsjpro/views/MainDrawerLayout;",
    "show",
    "V",
    "I",
)

@Suppress("unused")
val hideDrawerVipBannerPatch = bytecodePatch(
    name = "Hide Drawer VIP Banner",
    description = "Safely hides the promotional VIP membership banner inside the main navigation drawer with View.GONE.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val drawerShow = mainDrawerLayoutShowFingerprint.method
        val drawerRetIdx = drawerShow.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(drawerRetIdx >= 0) { "RETURN_VOID not found in MainDrawerLayout.show(I)V" }
        emitTranslationDispatch(
            method = drawerShow,
            insertIndex = drawerRetIdx,
            opCode = OP_HIDE_VIP_DRAWER,
        ) { m, _ -> m.thisRegisterIndex() }
    }
}
