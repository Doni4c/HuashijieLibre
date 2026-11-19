package app.morphe.patches.huashijie.core

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.rawResourcePatch
import java.io.RandomAccessFile

internal val HUASHIJIE_COMPATIBILITY = Compatibility(
    packageName = "net.huanci.hsjpro",
    name = "HuashijiePro",
    appIconColor = null as Int?,
    targets = listOf(AppTarget(version = "3.4.6", versionCodes = null)),
)

private fun ByteArray.toHexString(): String =
    joinToString(" ") { byte -> "%02X".format(byte.toInt() and 0xFF) }

private fun verifyAndPatchBytes(
    raf: RandomAccessFile,
    label: String,
    offset: Long,
    expectedOrig: ByteArray,
    patchBytes: ByteArray,
) {
    check(expectedOrig.size == patchBytes.size) {
        "Patch byte length mismatch for $label at 0x${offset.toString(16)}"
    }
    val requiredLength = offset + expectedOrig.size
    check(raf.length() >= requiredLength) {
        "File too short for $label at 0x${offset.toString(16)}: length=${raf.length()}, required=$requiredLength"
    }
    val actual = ByteArray(expectedOrig.size)
    raf.seek(offset)
    raf.readFully(actual)
    if (actual.contentEquals(patchBytes)) {
        return
    }
    if (!actual.contentEquals(expectedOrig)) {
        error(
            "Unexpected bytes for $label at 0x${offset.toString(16)}: " +
                "expected=[${expectedOrig.toHexString()}], actual=[${actual.toHexString()}]"
        )
    }
    raf.seek(offset)
    raf.write(patchBytes)
}

@Suppress("unused")
val nativeSignatureBypassPatch = rawResourcePatch(
    name = "Native Signature Bypass",
    description = "Allows customized HuashijiePro builds to launch smoothly without failing internal signature checks.",
    default = true,
) {
    compatibleWith(HUASHIJIE_COMPATIBILITY)

    execute {
        val arm64Lib = get("lib/arm64-v8a/libPaintLib.so")
        check(arm64Lib.exists() && arm64Lib.isFile) {
            "Expected native library not found: lib/arm64-v8a/libPaintLib.so"
        }
        RandomAccessFile(arm64Lib, "rw").use { raf ->
            // Force native signature helper to return 1 and NOP anti-tamper aborts.
            verifyAndPatchBytes(
                raf = raf,
                label = "arm64 dder0 prologue",
                offset = 0x1021CL,
                expectedOrig = byteArrayOf(0xFC.toByte(), 0x6F, 0xBA.toByte(), 0xA9.toByte(), 0xFA.toByte(), 0x67, 0x01, 0xA9.toByte()),
                patchBytes = byteArrayOf(0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()),
            )
            verifyAndPatchBytes(
                raf = raf,
                label = "arm64 dumbii check",
                offset = 0x10808L,
                expectedOrig = byteArrayOf(0x80.toByte(), 0x04, 0x00, 0x36),
                patchBytes = byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte()),
            )
            verifyAndPatchBytes(
                raf = raf,
                label = "arm64 JNI_OnLoad check",
                offset = 0x131FCL,
                expectedOrig = byteArrayOf(0x40, 0x10, 0x00, 0x36),
                patchBytes = byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte()),
            )
        }

        val armv7Lib = get("lib/armeabi-v7a/libPaintLib.so")
        check(armv7Lib.exists() && armv7Lib.isFile) {
            "Expected native library not found: lib/armeabi-v7a/libPaintLib.so"
        }
        RandomAccessFile(armv7Lib, "rw").use { raf ->
            // Thumb-2 movs r0, #1; bx lr followed by Thumb NOP pairs at crash checks.
            verifyAndPatchBytes(
                raf = raf,
                label = "armv7 dder0 Thumb prologue",
                offset = 0x9490L,
                expectedOrig = byteArrayOf(0xF0.toByte(), 0xB5.toByte(), 0x03, 0xAF.toByte()),
                patchBytes = byteArrayOf(0x01, 0x20, 0x70, 0x47),
            )
            verifyAndPatchBytes(
                raf = raf,
                label = "armv7 JNI_OnLoad Thumb check",
                offset = 0xB424L,
                expectedOrig = byteArrayOf(0x00, 0xF0.toByte(), 0xBF.toByte(), 0x80.toByte()),
                patchBytes = byteArrayOf(0x00, 0xBF.toByte(), 0x00, 0xBF.toByte()),
            )
        }
    }
}
