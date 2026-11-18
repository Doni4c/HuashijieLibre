package app.morphe.patches.huashijie.tier2_privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toMutableMethod
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

private const val TOGGLE_KEY = "pure_studio_block_telemetry"
private const val SHOULD_BLOCK_TELEMETRY =
    "Lapp/morphe/extension/huashijie/GlobalCompatibilityController;->shouldBlockTelemetrySdk()Z"

internal fun BytecodePatchContext.injectBooleanSdkReturnGuard(
    method: MutableMethod,
    conditionSignature: String,
    labelName: String,
) {
    val excluded = if (AccessFlags.STATIC.isSet(method.accessFlags)) {
        emptyList()
    } else {
        listOf(method.thisRegisterIndex())
    }
    val (target, scratch, insertIdx) = resolveLowRegisterAt(
        method = method,
        insertIndex = 0,
        *excluded.toIntArray(),
    )
    val resumeInsn: Instruction = target.getInstruction(insertIdx)
    target.addInstructionsWithLabels(
        insertIdx,
        """
        invoke-static {}, $conditionSignature
        move-result v$scratch
        if-eqz v$scratch, :$labelName
        return-void
        """.trimIndent(),
        ExternalLabel(labelName, resumeInsn),
    )
}

internal fun BytecodePatchContext.guardVoidSdkMethods(
    classType: String,
    methodNames: Set<String>,
    conditionSignature: String,
    labelName: String,
) {
    val classDef = classDefByOrNull(classType) ?: return
    val mutableClass = mutableClassDefBy(classDef)
    mutableClass.methods.forEach { method ->
        if (method.name in methodNames &&
            method.returnType == "V" &&
            method.implementation != null
        ) {
            injectBooleanSdkReturnGuard(method, conditionSignature, labelName)
        }
    }
}

@Suppress("unused")
val blockTelemetryAndTrackingPatch = bytecodePatch(
    name = "Block Host Telemetry SDKs",
    description = "Prevents xCrash and MarsXlog native crash and logging libraries from initializing in FlashActivity.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)
        val classes = allClasses()
        val flashClass = MultiVersionResolvers.resolveFlashActivityClass(classes)
        val telemetryInit = toMutableMethod(MultiVersionResolvers.resolveFlashTelemetryInit(flashClass))
        injectBooleanSdkReturnGuard(
            telemetryInit,
            SHOULD_BLOCK_TELEMETRY,
            "cond_allow_telemetry",
        )
    }
}
