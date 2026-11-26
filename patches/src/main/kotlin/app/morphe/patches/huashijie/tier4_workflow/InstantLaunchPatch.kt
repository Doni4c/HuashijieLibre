package app.morphe.patches.huashijie.tier4_workflow

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.MultiVersionResolvers
import app.morphe.patches.huashijie.core.MultiVersionResolvers.allClasses
import app.morphe.patches.huashijie.core.MultiVersionResolvers.registerResolvedTarget
import app.morphe.patches.huashijie.core.MultiVersionResolvers.toJavaClassName
import app.morphe.patches.huashijie.core.ensureBrushPkgManagerGetListHooked
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerBrushPackageListTargets
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.resolveLowRegisterAt
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TOGGLE_KEY = "pure_studio_instant_launch"
private const val STARTUP_LIFECYCLE_CONTROLLER =
    "Lapp/morphe/extension/huashijie/StartupLifecycleController;"

private val flashOnCreateFingerprint = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/FlashActivity;"
    },
)

@Suppress("unused")
val instantLaunchPatch = bytecodePatch(
    name = "Instant Launch to Studio",
    description = "Skips the startup countdown screen into the draft gallery while preserving external file open intents.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val classes = allClasses()
        val drawApp = MultiVersionResolvers.resolveDrawApplicationClass(classes)
        registerResolvedTarget("DrawApplication.class", drawApp.type.toJavaClassName())
        registerResolvedTarget(
            "DrawApplication.getInstance",
            MultiVersionResolvers.resolveDrawApplicationGetInstance(drawApp).name,
        )
        registerResolvedTarget(
            "DrawApplication.watchdogField",
            MultiVersionResolvers.resolveDrawApplicationWatchdogField(classes).name,
        )

        val brushMgr = MultiVersionResolvers.resolveBrushPkgManagerClass(classes)
        registerResolvedTarget(
            "BrushPkgManager.init",
            MultiVersionResolvers.resolveBrushPkgManagerInit(brushMgr).name,
        )
        registerBrushPackageListTargets(classes)

        val colorMgr = MultiVersionResolvers.resolveColorSpaceManagerClass(classes)
        registerResolvedTarget("ColorSpaceManager.class", colorMgr.type.toJavaClassName())
        registerResolvedTarget(
            "ColorSpaceManager.getInstance",
            MultiVersionResolvers.resolveColorSpaceManagerGetInstance(colorMgr).name,
        )
        registerResolvedTarget(
            "ColorSpaceManager.init",
            MultiVersionResolvers.resolveColorSpaceManagerInit(colorMgr).name,
        )

        val histMgr = MultiVersionResolvers.resolveHistoryBrushManagerClass(classes)
        registerResolvedTarget("HistoryBrushManager.class", histMgr.type.toJavaClassName())
        registerResolvedTarget(
            "HistoryBrushManager.getInstance",
            MultiVersionResolvers.resolveHistoryBrushManagerGetInstance(histMgr).name,
        )
        registerResolvedTarget(
            "HistoryBrushManager.init",
            MultiVersionResolvers.resolveHistoryBrushManagerInit(histMgr).name,
        )

        val flashClass = MultiVersionResolvers.resolveFlashActivityClass(classes)
        val splashJumpMethod = MultiVersionResolvers.resolveFlashSplashJump(flashClass)

        val flashOnCreate = flashOnCreateFingerprint.method
        val onCreateInsns = flashOnCreate.implementation!!.instructions
        val splashCallIdx = onCreateInsns.indexOfFirst { inst ->
            inst.opcode == Opcode.INVOKE_VIRTUAL &&
                (inst as? ReferenceInstruction)?.reference
                    .let { it as? MethodReference }
                    ?.let {
                        it.definingClass == splashJumpMethod.definingClass &&
                            it.name == splashJumpMethod.name &&
                            it.returnType == splashJumpMethod.returnType &&
                            it.parameterTypes.isEmpty()
                    } == true
        }
        check(splashCallIdx >= 0) { "FlashActivity splashJump call not found in onCreate" }

        val origThisReg = flashOnCreate.thisRegisterIndex()
        val (targetOnCreate, scratchReg, shiftedCallIdx) = resolveLowRegisterAt(
            method = flashOnCreate,
            insertIndex = splashCallIdx,
            origThisReg,
        )
        val newThisReg = targetOnCreate.thisRegisterIndex()
        val invokeHandle = formatInvokeStaticSingle(
            "${STARTUP_LIFECYCLE_CONTROLLER}->handleFlashLifecycle(Landroid/app/Activity;)Z",
            newThisReg,
        )
        targetOnCreate.addInstructionsWithLabels(
            shiftedCallIdx,
            """
            $invokeHandle
            move-result v$scratchReg
            if-nez v$scratchReg, :cond_skip_splash_ad
            """.trimIndent(),
            ExternalLabel("cond_skip_splash_ad", targetOnCreate.getInstruction(shiftedCallIdx + 1)),
        )

        ensureBrushPkgManagerGetListHooked()
    }
}
