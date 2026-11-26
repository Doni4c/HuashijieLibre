package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel

private const val TOGGLE_KEY = "pure_studio_block_bugly_sdk"
private const val SHOULD_BLOCK_BUGLY =
    "Lapp/morphe/extension/huashijie/GlobalCompatibilityController;->shouldBlockBuglySdk()Z"

@Suppress("unused")
val blockBuglySdkPatch = bytecodePatch(
    name = "Block Bugly Crash SDK",
    description = "Disables Tencent Bugly remote crash reporting initialization.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)
        guardVoidSdkMethods(
            classType = "Lcom/tencent/bugly/crashreport/CrashReport;",
            methodNames = setOf("initCrashReport"),
            conditionSignature = SHOULD_BLOCK_BUGLY,
            labelName = "cond_allow_bugly",
            expectedSourceFile = "CrashReport.java",
            sdkIdentifierStrings = setOf("CrashReport", "com.tencent.bugly.crashreport.CrashReport"),
        )
    }
}
