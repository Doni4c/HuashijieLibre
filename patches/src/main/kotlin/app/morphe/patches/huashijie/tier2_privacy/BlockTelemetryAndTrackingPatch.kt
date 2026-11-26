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
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import java.util.logging.Logger

private val SDK_GUARD_LOGGER: Logger = Logger.getLogger("HuashijieLibre.SdkGuard")

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

private fun ClassDef.hasVoidMethodIn(methodNames: Set<String>): Boolean =
    methods.any { m ->
        m.name in methodNames &&
            m.returnType == "V" &&
            m.implementation != null
    }

private fun ClassDef.containsAnySdkIdentifier(tokens: Set<String>): Boolean {
    if (tokens.isEmpty()) return false
    val fieldMatch = fields.any { f ->
        val s = (f.initialValue as? StringEncodedValue)?.value ?: return@any false
        tokens.any { token -> s == token || s.contains(token) }
    }
    if (fieldMatch) return true
    return methods.any { m ->
        m.implementation?.instructions?.any { inst ->
            val str = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string
                ?: return@any false
            tokens.any { token -> str == token || str.contains(token) }
        } == true
    }
}

internal fun BytecodePatchContext.guardVoidSdkMethods(
    classType: String,
    methodNames: Set<String>,
    conditionSignature: String,
    labelName: String,
    expectedSourceFile: String? = null,
    sdkIdentifierStrings: Set<String> = emptySet(),
) {
    val simpleName = classType.removePrefix("L").removeSuffix(";").substringAfterLast('/')
    val effectiveSourceFile = expectedSourceFile ?: "$simpleName.java"
    val effectiveTokens = if (sdkIdentifierStrings.isNotEmpty()) sdkIdentifierStrings else setOf(simpleName)

    var classDef = classDefByOrNull(classType)
    if (classDef == null) {
        val classes = allClasses()
        val sfAll = classes.filter { it.sourceFile == effectiveSourceFile && !it.type.contains('$') }
        val sfCandidates = if (sfAll.size > 1) sfAll.filter { it.hasVoidMethodIn(methodNames) } else sfAll
        if (sfCandidates.size == 1) {
            classDef = sfCandidates[0]
        } else if (sfCandidates.size > 1) {
            SDK_GUARD_LOGGER.warning(
                "SDK guard skipped: ambiguous sourceFile='$effectiveSourceFile' fallback for '$classType' (${sfCandidates.map { it.type }}).",
            )
            return
        }

        if (classDef == null && effectiveTokens.isNotEmpty()) {
            val strCandidates = classes.filter { c ->
                !c.type.contains('$') &&
                    c.hasVoidMethodIn(methodNames) &&
                    c.containsAnySdkIdentifier(effectiveTokens)
            }
            if (strCandidates.size == 1) {
                classDef = strCandidates[0]
            } else if (strCandidates.size > 1) {
                SDK_GUARD_LOGGER.warning(
                    "SDK guard skipped: ambiguous SDK identifier fallback $effectiveTokens for '$classType' (${strCandidates.map { it.type }}).",
                )
                return
            }
        }
    }

    if (classDef == null) {
        SDK_GUARD_LOGGER.warning(
            "SDK guard skipped: '$classType' not found by exact descriptor, sourceFile='$effectiveSourceFile', or SDK tokens=$effectiveTokens.",
        )
        return
    }

    val mutableClass = mutableClassDefBy(classDef)
    var hookedCount = 0
    mutableClass.methods.forEach { method ->
        if (method.name in methodNames &&
            method.returnType == "V" &&
            method.implementation != null
        ) {
            injectBooleanSdkReturnGuard(method, conditionSignature, labelName)
            hookedCount++
        }
    }
    if (hookedCount == 0) {
        SDK_GUARD_LOGGER.warning(
            "SDK guard warning: resolved '${classDef.type}' for '$classType' but found 0 void methods matching $methodNames.",
        )
    }
}

@Suppress("unused")
val blockTelemetryAndTrackingPatch = bytecodePatch(
    name = "Block Host Telemetry SDKs",
    description = "Stops the built-in xCrash and MarsXlog logging libraries from initializing at startup.",
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
