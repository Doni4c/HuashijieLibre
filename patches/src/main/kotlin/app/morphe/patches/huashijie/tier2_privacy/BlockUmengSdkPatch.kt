package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel

private const val TOGGLE_KEY = "pure_studio_block_umeng_sdk"
private const val SHOULD_BLOCK_UMENG =
    "Lapp/morphe/extension/huashijie/GlobalCompatibilityController;->shouldBlockUmengSdk()Z"

@Suppress("unused")
val blockUmengSdkPatch = bytecodePatch(
    name = "Block Umeng Analytics SDK",
    description = "Prevents Umeng UMConfigure analytics and device tracking routines from initializing.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)
        guardVoidSdkMethods(
            classType = "Lcom/umeng/commonsdk/UMConfigure;",
            methodNames = setOf("init", "preInit"),
            conditionSignature = SHOULD_BLOCK_UMENG,
            labelName = "cond_allow_umeng",
            expectedSourceFile = "UMConfigure.java",
            sdkIdentifierStrings = setOf("UMConfigure", "com.umeng.commonsdk.UMConfigure"),
        )
    }
}
