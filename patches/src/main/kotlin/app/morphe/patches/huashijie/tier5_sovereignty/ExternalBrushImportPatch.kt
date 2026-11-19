package app.morphe.patches.huashijie.tier5_sovereignty

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.huashijie.core.HUASHIJIE_COMPATIBILITY
import app.morphe.patches.huashijie.core.addInstructionsPreservingLabels
import app.morphe.patches.huashijie.core.ensureBrushPkgManagerGetListHooked
import app.morphe.patches.huashijie.core.formatInvokeStaticSingle
import app.morphe.patches.huashijie.core.libreSettingsPatch
import app.morphe.patches.huashijie.core.nativeSignatureBypassPatch
import app.morphe.patches.huashijie.core.registerToggleBeforeSettingsPanel
import app.morphe.patches.huashijie.core.thisRegisterIndex
import com.android.tools.smali.dexlib2.Opcode

private const val TOGGLE_KEY = "libre_external_brush_import"
private const val LOCAL_BRUSH_CONTROLLER =
    "Lapp/morphe/extension/huashijie/LocalBrushController;"

private val mainActivityInitViewFingerprint = Fingerprint(
    name = "initView",
    returnType = "V",
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "Lnet/huanci/hsjpro/activities/MainActivity;"
    },
)

@Suppress("unused")
val externalBrushImportPatch = bytecodePatch(
    name = "External Local Brush Scanning",
    description = "Scans local ASBrush directories on startup and guards loaded brush package lists for offline brush import workflows.",
    default = true,
) {
    dependsOn(nativeSignatureBypassPatch, libreSettingsPatch)
    compatibleWith(HUASHIJIE_COMPATIBILITY)
    extendWith("extensions/huashijie-libre.mpe")

    execute {
        registerToggleBeforeSettingsPanel(TOGGLE_KEY)

        val mainMethod = mainActivityInitViewFingerprint.method
        val mainRetIdx = mainMethod.implementation!!.instructions.indexOfLast {
            it.opcode == Opcode.RETURN_VOID
        }
        check(mainRetIdx >= 0) { "RETURN_VOID not found in MainActivity.initView()V" }
        mainMethod.addInstructionsPreservingLabels(
            mainRetIdx,
            formatInvokeStaticSingle(
                "${LOCAL_BRUSH_CONTROLLER}->scanCustomBrushes(Landroid/content/Context;)Ljava/util/List;",
                mainMethod.thisRegisterIndex(),
            ),
        )

        ensureBrushPkgManagerGetListHooked()
    }
}
