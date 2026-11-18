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
            raf.seek(0x1021CL)
            raf.write(byteArrayOf(0x20, 0x00, 0x80.toByte(), 0x52, 0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()))
            raf.seek(0x10808L)
            raf.write(byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte()))
            raf.seek(0x131FCL)
            raf.write(byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte()))
        }

        val armv7Lib = get("lib/armeabi-v7a/libPaintLib.so")
        check(armv7Lib.exists() && armv7Lib.isFile) {
            "Expected native library not found: lib/armeabi-v7a/libPaintLib.so"
        }
        RandomAccessFile(armv7Lib, "rw").use { raf ->
            // Thumb-2 movs r0, #1; bx lr followed by Thumb NOP pairs at crash checks.
            raf.seek(0x9490L)
            raf.write(byteArrayOf(0x01, 0x20, 0x70, 0x47))
            raf.seek(0xB424L)
            raf.write(byteArrayOf(0x00, 0xBF.toByte(), 0x00, 0xBF.toByte()))
        }
    }
}
