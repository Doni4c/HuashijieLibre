package app.morphe.patches.huashijie.core

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.rawResourcePatch
import java.io.File
import java.io.RandomAccessFile

internal val HUASHIJIE_COMPATIBILITY = Compatibility(
    packageName = "net.huanci.hsjpro",
    name = "HuashijiePro",
    appIconColor = null as Int?,
    targets = listOf(AppTarget(version = "3.4.6", versionCodes = null)),
)

internal const val ARM64_LIB_ENTRY = "lib/arm64-v8a/libPaintLib.so"
internal const val ARMV7_LIB_ENTRY = "lib/armeabi-v7a/libPaintLib.so"

internal data class NativePatchSiteMatch(
    val label: String,
    val fileOffset: Long,
    val matchCount: Int,
    val alreadyPatched: Boolean,
    val decodedCallTargetVaddr: Long? = null,
)

internal data class Arm64NativeTargets(
    val dder0Prologue: NativePatchSiteMatch,
    val dumbiiCheck: NativePatchSiteMatch,
    val jniOnLoadCheck: NativePatchSiteMatch,
    val resolvedDder0FromBlOffset: Long?,
)

internal data class Armv7NativeTargets(
    val dder0Prologue: NativePatchSiteMatch,
    val jniOnLoadCheck: NativePatchSiteMatch,
    val resolvedDder0FromBlOffset: Long?,
)

private const val ELF_MAGIC = 0x464C457FL // "\x7FELF" in little-endian u32
private const val ELFCLASS32 = 1
private const val ELFCLASS64 = 2
private const val ELFDATA2LSB = 1
private const val EM_ARM = 40
private const val EM_AARCH64 = 183

private const val PT_LOAD = 1L
private const val PT_DYNAMIC = 2L
private const val PF_X = 1L

private const val SHT_DYNSYM = 11L

private const val DT_NULL = 0L
private const val DT_PLTRELSZ = 2L
private const val DT_HASH = 4L
private const val DT_STRTAB = 5L
private const val DT_SYMTAB = 6L
private const val DT_STRSZ = 10L
private const val DT_SYMENT = 11L
private const val DT_PLTREL = 20L
private const val DT_JMPREL = 23L
private const val DT_GNU_HASH = 0x6FFFFEF5L
private const val DT_RELA = 7L

private const val ARM64_NOP = 0xD503201FL
private const val ARM64_TBZ_W0_BIT0_VAL = 0x36000000L
private const val ARM64_TBZ_W0_BIT0_MASK = 0xFFF8001FL

private val ARM64_DDER0_ORIG_WORDS = longArrayOf(0xA9BA6FFCL, 0xA90167FAL)
private val ARM64_DDER0_PATCH_WORDS = longArrayOf(0x52800020L, 0xD65F03C0L)
private val ARM64_DDER0_ORIG_BYTES = byteArrayOf(
    0xFC.toByte(), 0x6F, 0xBA.toByte(), 0xA9.toByte(),
    0xFA.toByte(), 0x67, 0x01, 0xA9.toByte(),
)
private val ARM64_DDER0_PATCH_BYTES = byteArrayOf(
    0x20, 0x00, 0x80.toByte(), 0x52,
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)
private val ARM64_NOP_BYTES = byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte())

private val ARMV7_DDER0_ORIG_BYTES = byteArrayOf(0xF0.toByte(), 0xB5.toByte(), 0x03, 0xAF.toByte())
private val ARMV7_DDER0_PATCH_BYTES = byteArrayOf(0x01, 0x20, 0x70, 0x47)
private val ARMV7_NOP_PAIR_BYTES = byteArrayOf(0x00, 0xBF.toByte(), 0x00, 0xBF.toByte())

private data class WordMask(val value: Long, val mask: Long)
private data class ByteMask(val value: Int, val mask: Int)

private data class ElfLoadSegment(
    val vaddr: Long,
    val offset: Long,
    val fileSize: Long,
    val flags: Long,
) {
    val isExecutable: Boolean get() = (flags and PF_X) != 0L
}

private data class ElfSymbol(
    val name: String,
    val value: Long,
    val size: Long,
)

private data class FileRange(val start: Long, val end: Long)

/**
 * Minimal pure-Kotlin ELF64/ELF32 reader using PT_LOAD and PT_DYNAMIC without relying on section headers.
 */
private class ElfImage private constructor(
    val bytes: ByteArray,
    val is64Bit: Boolean,
    val machine: Int,
    val loadSegments: List<ElfLoadSegment>,
    val dynamicSymbols: List<ElfSymbol>,
    val jmprelGotToSymIndex: Map<Long, Int>,
) {
    val executableSegments: List<ElfLoadSegment>
        get() = loadSegments.filter { it.isExecutable }

    fun vaddrToFileOffset(vaddr: Long): Long? {
        for (seg in loadSegments) {
            if (vaddr >= seg.vaddr && vaddr < seg.vaddr + seg.fileSize) {
                val off = seg.offset + (vaddr - seg.vaddr)
                if (off in 0L..bytes.size.toLong()) return off
            }
        }
        return null
    }

    fun fileOffsetToVaddr(fileOffset: Long): Long? {
        for (seg in loadSegments) {
            if (fileOffset >= seg.offset && fileOffset < seg.offset + seg.fileSize) {
                return seg.vaddr + (fileOffset - seg.offset)
            }
        }
        return null
    }

    fun findDynamicSymbol(name: String): ElfSymbol? =
        dynamicSymbols.firstOrNull { it.name == name }

    fun symbolFileRange(symbol: ElfSymbol): FileRange? {
        val codeVaddr = if (!is64Bit && machine == EM_ARM) symbol.value and -2L else symbol.value
        if (codeVaddr == 0L || symbol.size <= 0L) return null
        val start = vaddrToFileOffset(codeVaddr) ?: return null
        val end = minOf(start + symbol.size, bytes.size.toLong())
        return if (end > start) FileRange(start, end) else null
    }

    companion object {
        fun parse(bytes: ByteArray, expectedMachine: Int, expected64Bit: Boolean): ElfImage {
            require(bytes.size >= 64) { "ELF image too small: ${bytes.size} bytes" }
            require(readU32(bytes, 0) == ELF_MAGIC) { "Invalid ELF magic header" }
            val eiClass = bytes[4].toInt() and 0xFF
            val eiData = bytes[5].toInt() and 0xFF
            val is64 = when (eiClass) {
                ELFCLASS32 -> false
                ELFCLASS64 -> true
                else -> error("Unsupported ELF class: $eiClass")
            }
            require(is64 == expected64Bit) {
                "Unexpected ELF class: expected64=$expected64Bit, actual64=$is64"
            }
            require(eiData == ELFDATA2LSB) { "Unsupported ELF endianness: $eiData" }

            val machine = readU16(bytes, 18)
            require(machine == expectedMachine) {
                "Unexpected ELF e_machine: expected=$expectedMachine, actual=$machine"
            }

            val phoff: Long
            val phentsize: Int
            val phnum: Int
            val shoff: Long
            val shentsize: Int
            val shnum: Int
            if (is64) {
                phoff = readU64(bytes, 32)
                shoff = readU64(bytes, 40)
                phentsize = readU16(bytes, 54)
                phnum = readU16(bytes, 56)
                shentsize = readU16(bytes, 58)
                shnum = readU16(bytes, 60)
            } else {
                phoff = readU32(bytes, 28)
                shoff = readU32(bytes, 32)
                phentsize = readU16(bytes, 42)
                phnum = readU16(bytes, 44)
                shentsize = readU16(bytes, 46)
                shnum = readU16(bytes, 48)
            }

            val loadSegments = mutableListOf<ElfLoadSegment>()
            var dynOffset = -1L
            var dynSize = 0L

            for (i in 0 until phnum) {
                val entryOff = (phoff + i.toLong() * phentsize).toInt()
                if (entryOff < 0 || entryOff + phentsize > bytes.size) break
                val pType = readU32(bytes, entryOff)
                if (is64) {
                    val pFlags = readU32(bytes, entryOff + 4)
                    val pOffset = readU64(bytes, entryOff + 8)
                    val pVaddr = readU64(bytes, entryOff + 16)
                    val pFilesz = readU64(bytes, entryOff + 32)
                    if (pType == PT_LOAD) {
                        loadSegments += ElfLoadSegment(pVaddr, pOffset, pFilesz, pFlags)
                    } else if (pType == PT_DYNAMIC) {
                        dynOffset = pOffset
                        dynSize = pFilesz
                    }
                } else {
                    val pOffset = readU32(bytes, entryOff + 4)
                    val pVaddr = readU32(bytes, entryOff + 8)
                    val pFilesz = readU32(bytes, entryOff + 16)
                    val pFlags = readU32(bytes, entryOff + 24)
                    if (pType == PT_LOAD) {
                        loadSegments += ElfLoadSegment(pVaddr, pOffset, pFilesz, pFlags)
                    } else if (pType == PT_DYNAMIC) {
                        dynOffset = pOffset
                        dynSize = pFilesz
                    }
                }
            }

            require(loadSegments.any { it.isExecutable }) {
                "No executable PT_LOAD (PF_X) segment found in ELF"
            }

            fun vaddrToOff(vaddr: Long): Long? {
                for (seg in loadSegments) {
                    if (vaddr >= seg.vaddr && vaddr < seg.vaddr + seg.fileSize) {
                        val off = seg.offset + (vaddr - seg.vaddr)
                        if (off in 0L..bytes.size.toLong()) return off
                    }
                }
                return null
            }

            val dynTags = HashMap<Long, Long>()
            if (dynOffset >= 0 && dynSize > 0) {
                val step = if (is64) 16 else 8
                var off = dynOffset.toInt()
                val end = minOf((dynOffset + dynSize).toInt(), bytes.size)
                while (off + step <= end) {
                    val tag = if (is64) readI64(bytes, off) else readI32(bytes, off).toLong()
                    val value = if (is64) readU64(bytes, off + 8) else readU32(bytes, off + 4)
                    if (tag == DT_NULL) break
                    dynTags[tag] = value
                    off += step
                }
            }

            val symtabOff = dynTags[DT_SYMTAB]?.let { vaddrToOff(it) }
            val strtabOff = dynTags[DT_STRTAB]?.let { vaddrToOff(it) }
            val strtabSize = dynTags[DT_STRSZ] ?: 0L
            val syment = (dynTags[DT_SYMENT] ?: if (is64) 24L else 16L).toInt()

            var symCount = 0
            val hashOff = dynTags[DT_HASH]?.let { vaddrToOff(it) }
            val gnuHashOff = dynTags[DT_GNU_HASH]?.let { vaddrToOff(it) }
            if (hashOff != null && hashOff + 8 <= bytes.size) {
                symCount = readU32(bytes, hashOff.toInt() + 4).toInt()
            } else if (gnuHashOff != null && gnuHashOff + 16 <= bytes.size) {
                symCount = computeGnuHashSymCount(bytes, gnuHashOff.toInt(), is64)
            } else if (shoff > 0 && shnum > 0 && shentsize > 0) {
                // Optional fallback only when hash tables are absent and section headers remain intact.
                for (i in 0 until shnum) {
                    val shOff = (shoff + i.toLong() * shentsize).toInt()
                    if (shOff < 0 || shOff + shentsize > bytes.size) break
                    if (readU32(bytes, shOff) == SHT_DYNSYM) {
                        val size = if (is64) readU64(bytes, shOff + 32) else readU32(bytes, shOff + 20)
                        val ent = if (is64) readU64(bytes, shOff + 56) else readU32(bytes, shOff + 36)
                        if (ent > 0) symCount = (size / ent).toInt()
                        break
                    }
                }
            }

            val symbols = ArrayList<ElfSymbol>(symCount.coerceAtLeast(0))
            if (symtabOff != null && strtabOff != null && symCount > 0 && syment > 0) {
                val strLimit = if (strtabSize > 0L) {
                    minOf(strtabOff + strtabSize, bytes.size.toLong()).toInt()
                } else {
                    bytes.size
                }
                for (idx in 0 until symCount) {
                    val sOff = symtabOff.toInt() + idx * syment
                    if (sOff < 0 || sOff + syment > bytes.size) break
                    val stName: Int
                    val stValue: Long
                    val stSize: Long
                    if (is64) {
                        stName = readU32(bytes, sOff).toInt()
                        stValue = readU64(bytes, sOff + 8)
                        stSize = readU64(bytes, sOff + 16)
                    } else {
                        stName = readU32(bytes, sOff).toInt()
                        stValue = readU32(bytes, sOff + 4)
                        stSize = readU32(bytes, sOff + 8)
                    }
                    val nameStart = strtabOff.toInt() + stName
                    val symName = if (nameStart in strtabOff.toInt() until strLimit) {
                        readCString(bytes, nameStart, strLimit)
                    } else {
                        ""
                    }
                    symbols += ElfSymbol(symName, stValue, stSize)
                }
            }

            val jmprelMap = HashMap<Long, Int>()
            val jmprelOff = dynTags[DT_JMPREL]?.let { vaddrToOff(it) }
            val pltRelSize = dynTags[DT_PLTRELSZ] ?: 0L
            val pltRelType = dynTags[DT_PLTREL] ?: if (is64) DT_RELA else 17L
            if (jmprelOff != null && pltRelSize > 0L) {
                val relEntSize = when {
                    is64 && pltRelType == DT_RELA -> 24
                    is64 -> 16
                    pltRelType == DT_RELA -> 12
                    else -> 8
                }
                var rOff = jmprelOff.toInt()
                val rEnd = minOf((jmprelOff + pltRelSize).toInt(), bytes.size)
                while (rOff + relEntSize <= rEnd) {
                    val gotVaddr = if (is64) readU64(bytes, rOff) else readU32(bytes, rOff)
                    val symIdx = if (is64) {
                        (readU64(bytes, rOff + 8) ushr 32).toInt()
                    } else {
                        (readU32(bytes, rOff + 4) ushr 8).toInt()
                    }
                    jmprelMap[gotVaddr] = symIdx
                    rOff += relEntSize
                }
            }

            return ElfImage(
                bytes = bytes,
                is64Bit = is64,
                machine = machine,
                loadSegments = loadSegments,
                dynamicSymbols = symbols,
                jmprelGotToSymIndex = jmprelMap,
            )
        }

        private fun computeGnuHashSymCount(bytes: ByteArray, ghOff: Int, is64: Boolean): Int {
            val nbuckets = readU32(bytes, ghOff).toInt()
            val symoffset = readU32(bytes, ghOff + 4).toInt()
            val bloomSize = readU32(bytes, ghOff + 8).toInt()
            val bloomBytes = bloomSize * if (is64) 8 else 4
            val bucketsOff = ghOff + 16 + bloomBytes
            val chainsOff = bucketsOff + nbuckets * 4
            if (bucketsOff < 0 || chainsOff > bytes.size) return 0

            var maxSym = 0
            for (b in 0 until nbuckets) {
                val idx = readU32(bytes, bucketsOff + b * 4).toInt()
                if (idx > maxSym) maxSym = idx
            }
            if (maxSym < symoffset) return symoffset

            var cur = maxSym
            while (true) {
                val cOff = chainsOff + (cur - symoffset) * 4
                if (cOff < 0 || cOff + 4 > bytes.size) return cur
                val chainVal = readU32(bytes, cOff)
                if ((chainVal and 1L) != 0L) {
                    return cur + 1
                }
                cur++
            }
        }
    }
}

private fun readU16(bytes: ByteArray, off: Int): Int =
    (bytes[off].toInt() and 0xFF) or ((bytes[off + 1].toInt() and 0xFF) shl 8)

private fun readU32(bytes: ByteArray, off: Int): Long =
    (bytes[off].toLong() and 0xFFL) or
        ((bytes[off + 1].toLong() and 0xFFL) shl 8) or
        ((bytes[off + 2].toLong() and 0xFFL) shl 16) or
        ((bytes[off + 3].toLong() and 0xFFL) shl 24)

private fun readI32(bytes: ByteArray, off: Int): Int =
    (bytes[off].toInt() and 0xFF) or
        ((bytes[off + 1].toInt() and 0xFF) shl 8) or
        ((bytes[off + 2].toInt() and 0xFF) shl 16) or
        ((bytes[off + 3].toInt() and 0xFF) shl 24)

private fun readI64(bytes: ByteArray, off: Int): Long =
    (bytes[off].toLong() and 0xFFL) or
        ((bytes[off + 1].toLong() and 0xFFL) shl 8) or
        ((bytes[off + 2].toLong() and 0xFFL) shl 16) or
        ((bytes[off + 3].toLong() and 0xFFL) shl 24) or
        ((bytes[off + 4].toLong() and 0xFFL) shl 32) or
        ((bytes[off + 5].toLong() and 0xFFL) shl 40) or
        ((bytes[off + 6].toLong() and 0xFFL) shl 48) or
        ((bytes[off + 7].toLong() and 0xFFL) shl 56)

private fun readU64(bytes: ByteArray, off: Int): Long = readI64(bytes, off)

private fun readCString(bytes: ByteArray, start: Int, limit: Int): String {
    var end = start
    while (end < limit && bytes[end].toInt() != 0) end++
    return String(bytes, start, end - start, Charsets.UTF_8)
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { byte -> "%02X".format(byte.toInt() and 0xFF) }

private fun decodeArm64BlTargetVaddr(elf: ElfImage, insFileOffset: Long): Long? {
    val off = insFileOffset.toInt()
    if (off < 0 || off + 4 > elf.bytes.size) return null
    val w = readU32(elf.bytes, off)
    if ((w and 0xFC000000L) != 0x94000000L) return null
    val pcVaddr = elf.fileOffsetToVaddr(insFileOffset) ?: return null
    val rawImm26 = w and 0x03FFFFFFL
    val signedImm26 = if ((rawImm26 and 0x02000000L) != 0L) rawImm26 - 0x04000000L else rawImm26
    return pcVaddr + (signedImm26 shl 2)
}

private fun decodeArm64AdrpImm(w: Long): Long {
    val immlo = (w ushr 29) and 0x3L
    val immhi = (w ushr 5) and 0x7FFFFL
    val raw = ((immhi shl 2) or immlo) shl 12
    return if ((raw and (1L shl 32)) != 0L) raw - (1L shl 33) else raw
}

private fun resolveArm64CallTargetToFuncOffset(elf: ElfImage, targetVaddr: Long): Long? {
    val targetOff = elf.vaddrToFileOffset(targetVaddr)?.toInt() ?: return null
    if (targetOff + 16 <= elf.bytes.size) {
        var stubIdx = targetOff
        var stubVaddr = targetVaddr
        val maybeBti = readU32(elf.bytes, stubIdx)
        if ((maybeBti and 0xFFFFFF3FL) == 0xD503241FL && stubIdx + 16 <= elf.bytes.size) {
            stubIdx += 4
            stubVaddr += 4
        }
        val w0 = readU32(elf.bytes, stubIdx)
        val w1 = readU32(elf.bytes, stubIdx + 4)
        if ((w0 and 0x9F00001FL) == 0x90000010L && (w1 and 0xFFC003FFL) == 0xF9400211L) {
            val gotPage = (stubVaddr and -4096L) + decodeArm64AdrpImm(w0)
            val gotOff = ((w1 ushr 10) and 0xFFFL) shl 3
            val gotVaddr = gotPage + gotOff
            val symIdx = elf.jmprelGotToSymIndex[gotVaddr]
            if (symIdx != null && symIdx in elf.dynamicSymbols.indices) {
                val symVal = elf.dynamicSymbols[symIdx].value
                if (symVal != 0L) return elf.vaddrToFileOffset(symVal)
            }
        }
    }
    return if (elf.executableSegments.any { targetVaddr >= it.vaddr && targetVaddr < it.vaddr + it.fileSize }) {
        targetOff.toLong()
    } else {
        null
    }
}

private fun decodeThumbBlOrBlxTargetVaddr(elf: ElfImage, insFileOffset: Long): Long? {
    val off = insFileOffset.toInt()
    if (off < 0 || off + 4 > elf.bytes.size) return null
    val hw1 = readU16(elf.bytes, off)
    val hw2 = readU16(elf.bytes, off + 2)
    if ((hw1 and 0xF800) != 0xF000 || (hw2 and 0xC000) != 0xC000) return null
    val isBl = (hw2 and 0x1000) != 0
    if (!isBl && (hw2 and 1) != 0) return null

    val pcVaddr = elf.fileOffsetToVaddr(insFileOffset) ?: return null
    val s = (hw1 ushr 10) and 1
    val imm10 = hw1 and 0x3FF
    val j1 = (hw2 ushr 13) and 1
    val j2 = (hw2 ushr 11) and 1
    val i1 = (j1 xor s) xor 1
    val i2 = (j2 xor s) xor 1
    return if (isBl) {
        val imm11 = hw2 and 0x7FF
        val raw = (s shl 24) or (i1 shl 23) or (i2 shl 22) or (imm10 shl 12) or (imm11 shl 1)
        val signed = if (s != 0) raw - (1 shl 25) else raw
        pcVaddr + 4L + signed.toLong()
    } else {
        val imm10L = (hw2 ushr 1) and 0x3FF
        val raw = (s shl 24) or (i1 shl 23) or (i2 shl 22) or (imm10 shl 12) or (imm10L shl 2)
        val signed = if (s != 0) raw - (1 shl 25) else raw
        ((pcVaddr + 4L) and -4L) + signed.toLong()
    }
}

private fun decodeArmImmediate(ins: Long): Long {
    val rot = ((ins ushr 8) and 0xFL).toInt() * 2
    val imm8 = ins and 0xFFL
    return if (rot == 0) imm8 else ((imm8 ushr rot) or (imm8 shl (32 - rot))) and 0xFFFFFFFFL
}

private fun resolveArmv7CallTargetToFuncOffset(elf: ElfImage, targetVaddr: Long): Long? {
    val codeVaddr = targetVaddr and -2L
    val targetOff = elf.vaddrToFileOffset(codeVaddr)?.toInt() ?: return null
    if (targetOff + 12 <= elf.bytes.size && (targetVaddr and 1L) == 0L) {
        val w0 = readU32(elf.bytes, targetOff)
        val w1 = readU32(elf.bytes, targetOff + 4)
        val w2 = readU32(elf.bytes, targetOff + 8)
        if ((w0 and 0xFFFFF000L) == 0xE28FC000L &&
            (w1 and 0xFFFFF000L) == 0xE28CC000L &&
            (w2 and 0xFFFFF000L) == 0xE5BCF000L
        ) {
            val gotVaddr = (codeVaddr + 8L) + decodeArmImmediate(w0) + decodeArmImmediate(w1) + (w2 and 0xFFFL)
            val symIdx = elf.jmprelGotToSymIndex[gotVaddr]
            if (symIdx != null && symIdx in elf.dynamicSymbols.indices) {
                val symVal = elf.dynamicSymbols[symIdx].value and -2L
                if (symVal != 0L) return elf.vaddrToFileOffset(symVal)
            }
        }
    }
    return if (elf.executableSegments.any { codeVaddr >= it.vaddr && codeVaddr < it.vaddr + it.fileSize }) {
        targetOff.toLong()
    } else {
        null
    }
}

private fun requireSingleMatch(label: String, matches: List<NativePatchSiteMatch>): NativePatchSiteMatch {
    if (matches.size == 1) return matches.single()
    val summary = if (matches.isEmpty()) {
        "0 matches found"
    } else {
        val offsets = matches.joinToString(", ") { "0x${it.fileOffset.toString(16).uppercase()}" }
        "${matches.size} ambiguous matches found at [$offsets]"
    }
    error("Native AOB locator failed for $label: expected matchCount == 1, got $summary")
}

private fun scanArm64MaskedWindow(
    bytes: ByteArray,
    ranges: List<FileRange>,
    label: String,
    window: List<WordMask>,
    targetWordIndex: Int,
    acceptPatchedAtTarget: (Long) -> Boolean,
    headAlternatives: List<LongArray>? = null,
    headTargetCheck: ((Int) -> Boolean)? = null,
    extraFilter: ((Int) -> Boolean)? = null,
): NativePatchSiteMatch {
    val hits = mutableListOf<NativePatchSiteMatch>()
    val winBytes = window.size * 4
    val headLen = headAlternatives?.firstOrNull()?.size ?: 0
    for (range in ranges) {
        val start = ((range.start + 3L) and -4L).toInt()
        val end = minOf(range.end.toInt(), bytes.size) - winBytes
        var off = start
        while (off <= end) {
            var matched = true
            var alreadyPatched = false
            if (headAlternatives != null) {
                var headOk = false
                for (altIdx in headAlternatives.indices) {
                    val alt = headAlternatives[altIdx]
                    var altMatch = true
                    for (j in alt.indices) {
                        if (readU32(bytes, off + j * 4) != alt[j]) {
                            altMatch = false
                            break
                        }
                    }
                    if (altMatch) {
                        headOk = true
                        alreadyPatched = headTargetCheck?.invoke(altIdx) ?: false
                        break
                    }
                }
                if (!headOk) matched = false
            }

            if (matched) {
                for (i in headLen until window.size) {
                    val w = readU32(bytes, off + i * 4)
                    val wm = window[i]
                    if (i == targetWordIndex) {
                        if (w == ARM64_NOP) {
                            alreadyPatched = true
                        } else if ((w and wm.mask) != (wm.value and wm.mask)) {
                            matched = false
                            break
                        } else if (acceptPatchedAtTarget(w)) {
                            alreadyPatched = true
                        }
                    } else if ((w and wm.mask) != (wm.value and wm.mask)) {
                        matched = false
                        break
                    }
                }
            }

            if (matched && (extraFilter == null || extraFilter(off))) {
                hits += NativePatchSiteMatch(
                    label = label,
                    fileOffset = (off + targetWordIndex * 4).toLong(),
                    matchCount = 1,
                    alreadyPatched = alreadyPatched,
                )
            }
            off += 4
        }
    }
    return requireSingleMatch(label, hits)
}

internal fun resolveArm64Targets(bytes: ByteArray): Arm64NativeTargets {
    val elf = ElfImage.parse(bytes, expectedMachine = EM_AARCH64, expected64Bit = true)
    val execRanges = elf.executableSegments.map { seg ->
        FileRange(seg.offset, minOf(seg.offset + seg.fileSize, bytes.size.toLong()))
    }
    val jniRange = elf.findDynamicSymbol("JNI_OnLoad")?.let { elf.symbolFileRange(it) }
    val jniSearchRanges = if (jniRange != null) listOf(jniRange) else execRanges

    // Site (c): JNI_OnLoad check (tbz w0, #0, #imm14 immediately after bl dder0; masked for 14-bit imm).
    val jniWindow = listOf(
        WordMask(0xF94003E0L, 0xFFC003FFL), // ldr x0, [sp, #imm]
        WordMask(0x94000000L, 0xFC000000L), // bl <dder0>
        WordMask(ARM64_TBZ_W0_BIT0_VAL, ARM64_TBZ_W0_BIT0_MASK), // tbz w0, #0, #imm14 (target)
        WordMask(0x90000008L, 0x9F00001FL), // adrp x8, #page
        WordMask(0xF9400108L, 0xFFC003FFL), // ldr x8, [x8, #imm]
        WordMask(0xF9400100L, 0xFFFFFFE0L), // ldr xRt, [x8]
        WordMask(0xAA0003E0L, 0xFFE0FFFFL), // mov x0, xRm
        WordMask(0x94000000L, 0xFC000000L), // bl <strlen>
    )
    val jniMatchRaw = scanArm64MaskedWindow(
        bytes = bytes,
        ranges = jniSearchRanges,
        label = "arm64 JNI_OnLoad check",
        window = jniWindow,
        targetWordIndex = 2,
        acceptPatchedAtTarget = { it == ARM64_NOP },
    )
    val jniBlTargetVaddr = decodeArm64BlTargetVaddr(elf, jniMatchRaw.fileOffset - 4L)
        ?: error("Failed to decode BL <dder0> preceding arm64 JNI_OnLoad check at 0x${jniMatchRaw.fileOffset.toString(16)}")
    val jniMatch = jniMatchRaw.copy(decodedCallTargetVaddr = jniBlTargetVaddr)

    // Site (b): dumbii check (tbz w0, #0, #imm14 immediately after bl dder0).
    val dumbiiWindow = listOf(
        WordMask(0xD53BD040L, 0xFFFFFFE0L), // mrs xRt, TPIDR_EL0
        WordMask(0xF9401408L, 0xFFFFFC1FL), // ldr x8, [xRn, #0x28]
        WordMask(0xAA0003E0L, 0xFFFFFFE0L), // mov xRd, x0
        WordMask(0xF80003A8L, 0xFFE00FFFL), // stur x8, [x29, #imm9]
        WordMask(0x94000000L, 0xFC000000L), // bl <dder0>
        WordMask(ARM64_TBZ_W0_BIT0_VAL, ARM64_TBZ_W0_BIT0_MASK), // tbz w0, #0, #imm14 (target)
        WordMask(0x90000000L, 0x9F000000L), // adrp xRd, #page
        WordMask(0xF9400000L, 0xFFC00000L), // ldr xRt, [xRn, #imm]
        WordMask(0xF9400000L, 0xFFFFFC1FL), // ldr x0, [xRn]
        WordMask(0x94000000L, 0xFC000000L), // bl <strlen>
    )
    val dumbiiMatchRaw = scanArm64MaskedWindow(
        bytes = bytes,
        ranges = execRanges,
        label = "arm64 dumbii check",
        window = dumbiiWindow,
        targetWordIndex = 5,
        acceptPatchedAtTarget = { it == ARM64_NOP },
        extraFilter = { winStart ->
            decodeArm64BlTargetVaddr(elf, (winStart + 16).toLong()) == jniBlTargetVaddr
        },
    )
    val dumbiiBlTargetVaddr = decodeArm64BlTargetVaddr(elf, dumbiiMatchRaw.fileOffset - 4L)
    val dumbiiMatch = dumbiiMatchRaw.copy(decodedCallTargetVaddr = dumbiiBlTargetVaddr)

    // Site (a): dder0 prologue (orig or patched 8-byte stub + invariant body).
    val dder0Window = listOf(
        WordMask(0L, 0L), // word 0 checked via headAlternatives
        WordMask(0L, 0L), // word 1 checked via headAlternatives
        WordMask(0xA9025FF8L, 0xFFFFFFFFL), // stp x24, x23, [sp, #0x20]
        WordMask(0xA90357F6L, 0xFFFFFFFFL), // stp x22, x21, [sp, #0x30]
        WordMask(0xA9044FF4L, 0xFFFFFFFFL), // stp x20, x19, [sp, #0x40]
        WordMask(0xA9057BFDL, 0xFFFFFFFFL), // stp x29, x30, [sp, #0x50]
        WordMask(0x910143FDL, 0xFFFFFFFFL), // add x29, sp, #0x50
        WordMask(0x90000008L, 0x9F00001FL), // adrp x8, #page
        WordMask(0xF9400108L, 0xFFC003FFL), // ldr x8, [x8, #imm]
        WordMask(0xAA0003F3L, 0xFFFFFFFFL), // mov x19, x0
        WordMask(0xF9400114L, 0xFFFFFFFFL), // ldr x20, [x8]
        WordMask(0xAA1403E0L, 0xFFFFFFFFL), // mov x0, x20
        WordMask(0x94000000L, 0xFC000000L), // bl <strlen>
        WordMask(0xD3607C16L, 0xFFFFFFFFL), // ubfx x22, x0, #0, #32
        WordMask(0xD2C00028L, 0xFFFFFFFFL), // mov x8, #0x100000000
        WordMask(0x8B0802C8L, 0xFFFFFFFFL), // add x8, x22, x8
    )
    val dder0Match = scanArm64MaskedWindow(
        bytes = bytes,
        ranges = execRanges,
        label = "arm64 dder0 prologue",
        window = dder0Window,
        targetWordIndex = 0,
        acceptPatchedAtTarget = { false },
        headAlternatives = listOf(ARM64_DDER0_ORIG_WORDS, ARM64_DDER0_PATCH_WORDS),
        headTargetCheck = { altIdx -> altIdx == 1 },
    )

    val resolvedFromBl = resolveArm64CallTargetToFuncOffset(elf, jniBlTargetVaddr)
    if (resolvedFromBl != null) {
        check(resolvedFromBl == dder0Match.fileOffset) {
            "Cross-check mismatch for arm64 dder0: AOB=0x${dder0Match.fileOffset.toString(16)}, " +
                "BL(0x${jniBlTargetVaddr.toString(16)})=0x${resolvedFromBl.toString(16)}"
        }
    }

    return Arm64NativeTargets(
        dder0Prologue = dder0Match,
        dumbiiCheck = dumbiiMatch,
        jniOnLoadCheck = jniMatch,
        resolvedDder0FromBlOffset = resolvedFromBl,
    )
}

private fun scanArmv7ThumbBeqSlot(bytes: ByteArray, off: Int): Pair<Boolean, Boolean> {
    val hw1 = readU16(bytes, off)
    val hw2 = readU16(bytes, off + 2)
    if (hw1 == 0xBF00 && hw2 == 0xBF00) return true to true
    val isBeqW = (hw1 and 0xFBC0) == 0xF000 && (hw2 and 0xD000) == 0x8000
    return isBeqW to false
}

internal fun resolveArmv7Targets(bytes: ByteArray): Armv7NativeTargets {
    val elf = ElfImage.parse(bytes, expectedMachine = EM_ARM, expected64Bit = false)
    val execRanges = elf.executableSegments.map { seg ->
        FileRange(seg.offset, minOf(seg.offset + seg.fileSize, bytes.size.toLong()))
    }
    val jniRange = elf.findDynamicSymbol("JNI_OnLoad")?.let { elf.symbolFileRange(it) }
    val jniSearchRanges = if (jniRange != null) listOf(jniRange) else execRanges

    // Site (e): JNI_OnLoad Thumb check (beq.w #imm after blx dder0; cmp r0, #0).
    // Bytes relative to matchStart:
    //  +0..+1: ldr r0, [sp, #imm]  (?? 98)
    //  +2..+5: Thumb-2 bl/blx      (hw1 & F800 == F000, hw2 & C000 == C000)
    //  +6..+7: cmp r0, #0          (00 28)
    //  +8..+11: beq.w #imm OR nop;nop (target)
    // +12..+13: str rRt, [sp, #imm] (?? [90..97])
    // +14..+17: str.w rRt, [sp, #imm] (CD F8 ?? ??)
    // +18..+19: ldr r0, [pc, #imm] (?? 48)
    // +20..+21: add r0, pc         (78 44)
    val jniPrefix = listOf(
        ByteMask(0x00, 0x00), ByteMask(0x98, 0xFF),
        ByteMask(0x00, 0x00), ByteMask(0xF0, 0xF8), ByteMask(0x00, 0x00), ByteMask(0xC0, 0xC0),
        ByteMask(0x00, 0xFF), ByteMask(0x28, 0xFF),
    )
    val jniSuffix = listOf(
        ByteMask(0x00, 0x00), ByteMask(0x90, 0xF8),
        ByteMask(0xCD, 0xFF), ByteMask(0xF8, 0xFF), ByteMask(0x00, 0x00), ByteMask(0x00, 0x00),
        ByteMask(0x00, 0x00), ByteMask(0x48, 0xFF),
        ByteMask(0x78, 0xFF), ByteMask(0x44, 0xFF),
    )
    val jniWindowSize = jniPrefix.size + 4 + jniSuffix.size
    val jniHits = mutableListOf<NativePatchSiteMatch>()
    for (range in jniSearchRanges) {
        val start = ((range.start + 1L) and -2L).toInt()
        val end = minOf(range.end.toInt(), bytes.size) - jniWindowSize
        var off = start
        while (off <= end) {
            var ok = true
            for (i in jniPrefix.indices) {
                val b = bytes[off + i].toInt() and 0xFF
                if ((b and jniPrefix[i].mask) != (jniPrefix[i].value and jniPrefix[i].mask)) {
                    ok = false
                    break
                }
            }
            var alreadyPatched = false
            if (ok) {
                val (slotOk, patched) = scanArmv7ThumbBeqSlot(bytes, off + 8)
                ok = slotOk
                alreadyPatched = patched
            }
            if (ok) {
                val sufOff = off + 12
                for (i in jniSuffix.indices) {
                    val b = bytes[sufOff + i].toInt() and 0xFF
                    if ((b and jniSuffix[i].mask) != (jniSuffix[i].value and jniSuffix[i].mask)) {
                        ok = false
                        break
                    }
                }
            }
            if (ok) {
                val targetOff = (off + 8).toLong()
                val blTarget = decodeThumbBlOrBlxTargetVaddr(elf, (off + 2).toLong())
                if (blTarget != null) {
                    jniHits += NativePatchSiteMatch(
                        label = "armv7 JNI_OnLoad Thumb check",
                        fileOffset = targetOff,
                        matchCount = 1,
                        alreadyPatched = alreadyPatched,
                        decodedCallTargetVaddr = blTarget,
                    )
                }
            }
            off += 2
        }
    }
    val jniMatch = requireSingleMatch("armv7 JNI_OnLoad Thumb check", jniHits)
    val jniBlTargetVaddr = jniMatch.decodedCallTargetVaddr
        ?: error("Failed to decode BL/BLX <dder0> preceding armv7 JNI_OnLoad check")

    // Site (d): dder0 Thumb prologue (orig F0 B5 03 AF or patched 01 20 70 47 + invariant Thumb body).
    val dder0Body = listOf(
        ByteMask(0x2D, 0xFF), ByteMask(0xE9, 0xFF), ByteMask(0x00, 0xFF), ByteMask(0x0F, 0xFF), // push.w {r8-r11}
        ByteMask(0x83, 0xFF), ByteMask(0xB0, 0xFF), // sub sp, #12
        ByteMask(0x82, 0xFF), ByteMask(0x46, 0xFF), // mov r10, r0
        ByteMask(0x00, 0x00), ByteMask(0x48, 0xFF), // ldr r0, [pc, #imm]
        ByteMask(0x78, 0xFF), ByteMask(0x44, 0xFF), // add r0, pc
        ByteMask(0x00, 0xFF), ByteMask(0x68, 0xFF), // ldr r0, [r0]
        ByteMask(0x04, 0xFF), ByteMask(0x68, 0xFF), // ldr r4, [r0]
        ByteMask(0x20, 0xFF), ByteMask(0x46, 0xFF), // mov r0, r4
        ByteMask(0x00, 0x00), ByteMask(0xF0, 0xF8), ByteMask(0x00, 0x00), ByteMask(0xC0, 0xC0), // bl/blx <strlen>
    )
    val dder0WindowSize = 4 + dder0Body.size
    val dder0Hits = mutableListOf<NativePatchSiteMatch>()
    for (range in execRanges) {
        val start = ((range.start + 1L) and -2L).toInt()
        val end = minOf(range.end.toInt(), bytes.size) - dder0WindowSize
        var off = start
        while (off <= end) {
            val headMatchOrig =
                bytes[off] == ARMV7_DDER0_ORIG_BYTES[0] &&
                    bytes[off + 1] == ARMV7_DDER0_ORIG_BYTES[1] &&
                    bytes[off + 2] == ARMV7_DDER0_ORIG_BYTES[2] &&
                    bytes[off + 3] == ARMV7_DDER0_ORIG_BYTES[3]
            val headMatchPatched =
                bytes[off] == ARMV7_DDER0_PATCH_BYTES[0] &&
                    bytes[off + 1] == ARMV7_DDER0_PATCH_BYTES[1] &&
                    bytes[off + 2] == ARMV7_DDER0_PATCH_BYTES[2] &&
                    bytes[off + 3] == ARMV7_DDER0_PATCH_BYTES[3]
            if (headMatchOrig || headMatchPatched) {
                var ok = true
                for (i in dder0Body.indices) {
                    val b = bytes[off + 4 + i].toInt() and 0xFF
                    if ((b and dder0Body[i].mask) != (dder0Body[i].value and dder0Body[i].mask)) {
                        ok = false
                        break
                    }
                }
                if (ok) {
                    dder0Hits += NativePatchSiteMatch(
                        label = "armv7 dder0 Thumb prologue",
                        fileOffset = off.toLong(),
                        matchCount = 1,
                        alreadyPatched = headMatchPatched,
                    )
                }
            }
            off += 2
        }
    }
    val dder0Match = requireSingleMatch("armv7 dder0 Thumb prologue", dder0Hits)

    val resolvedFromBl = resolveArmv7CallTargetToFuncOffset(elf, jniBlTargetVaddr)
    if (resolvedFromBl != null) {
        check(resolvedFromBl == dder0Match.fileOffset) {
            "Cross-check mismatch for armv7 dder0: AOB=0x${dder0Match.fileOffset.toString(16)}, " +
                "BLX(0x${jniBlTargetVaddr.toString(16)})=0x${resolvedFromBl.toString(16)}"
        }
    }

    return Armv7NativeTargets(
        dder0Prologue = dder0Match,
        jniOnLoadCheck = jniMatch,
        resolvedDder0FromBlOffset = resolvedFromBl,
    )
}

private fun verifyAndPatchExactBytes(
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

private fun verifyAndPatchMaskedArm64Tbz(
    raf: RandomAccessFile,
    label: String,
    offset: Long,
    patchBytes: ByteArray,
) {
    check(patchBytes.size == 4) { "Invalid ARM64 instruction size for $label" }
    val requiredLength = offset + 4L
    check(raf.length() >= requiredLength) {
        "File too short for $label at 0x${offset.toString(16)}: length=${raf.length()}, required=$requiredLength"
    }
    val actual = ByteArray(4)
    raf.seek(offset)
    raf.readFully(actual)
    if (actual.contentEquals(patchBytes)) {
        return
    }
    val word = readU32(actual, 0)
    if ((word and ARM64_TBZ_W0_BIT0_MASK) != ARM64_TBZ_W0_BIT0_VAL) {
        error(
            "Unexpected instruction for $label at 0x${offset.toString(16)}: " +
                "expected TBZ w0, #0, #imm (mask=0x${ARM64_TBZ_W0_BIT0_MASK.toString(16)}), " +
                "actual=[${actual.toHexString()}]"
        )
    }
    raf.seek(offset)
    raf.write(patchBytes)
}

private fun verifyAndPatchMaskedArmv7Beq(
    raf: RandomAccessFile,
    label: String,
    offset: Long,
    patchBytes: ByteArray,
) {
    check(patchBytes.size == 4) { "Invalid Thumb-2 BEQ.W size for $label" }
    val requiredLength = offset + 4L
    check(raf.length() >= requiredLength) {
        "File too short for $label at 0x${offset.toString(16)}: length=${raf.length()}, required=$requiredLength"
    }
    val actual = ByteArray(4)
    raf.seek(offset)
    raf.readFully(actual)
    if (actual.contentEquals(patchBytes)) {
        return
    }
    val hw1 = readU16(actual, 0)
    val hw2 = readU16(actual, 2)
    if ((hw1 and 0xFBC0) != 0xF000 || (hw2 and 0xD000) != 0x8000) {
        error(
            "Unexpected instruction for $label at 0x${offset.toString(16)}: " +
                "expected Thumb-2 BEQ.W, actual=[${actual.toHexString()}]"
        )
    }
    raf.seek(offset)
    raf.write(patchBytes)
}

internal fun patchNativeLibraryFile(libFile: File, patchArm64: Boolean) {
    val rawBytes = libFile.readBytes()
    if (patchArm64) {
        val targets = resolveArm64Targets(rawBytes)
        RandomAccessFile(libFile, "rw").use { raf ->
            verifyAndPatchExactBytes(
                raf = raf,
                label = targets.dder0Prologue.label,
                offset = targets.dder0Prologue.fileOffset,
                expectedOrig = ARM64_DDER0_ORIG_BYTES,
                patchBytes = ARM64_DDER0_PATCH_BYTES,
            )
            verifyAndPatchMaskedArm64Tbz(
                raf = raf,
                label = targets.dumbiiCheck.label,
                offset = targets.dumbiiCheck.fileOffset,
                patchBytes = ARM64_NOP_BYTES,
            )
            verifyAndPatchMaskedArm64Tbz(
                raf = raf,
                label = targets.jniOnLoadCheck.label,
                offset = targets.jniOnLoadCheck.fileOffset,
                patchBytes = ARM64_NOP_BYTES,
            )
        }
    } else {
        val targets = resolveArmv7Targets(rawBytes)
        RandomAccessFile(libFile, "rw").use { raf ->
            verifyAndPatchExactBytes(
                raf = raf,
                label = targets.dder0Prologue.label,
                offset = targets.dder0Prologue.fileOffset,
                expectedOrig = ARMV7_DDER0_ORIG_BYTES,
                patchBytes = ARMV7_DDER0_PATCH_BYTES,
            )
            verifyAndPatchMaskedArmv7Beq(
                raf = raf,
                label = targets.jniOnLoadCheck.label,
                offset = targets.jniOnLoadCheck.fileOffset,
                patchBytes = ARMV7_NOP_PAIR_BYTES,
            )
        }
    }
}

@Suppress("unused")
val nativeSignatureBypassPatch = rawResourcePatch(
    name = "Native Signature Bypass",
    description = "Preserves original native byte signatures when modifying libPaintLib.so so patched builds pass startup checks.",
    default = true,
) {
    compatibleWith(HUASHIJIE_COMPATIBILITY)

    execute {
        val arm64Lib = get(ARM64_LIB_ENTRY)
        check(arm64Lib.exists() && arm64Lib.isFile) {
            "Expected native library not found: $ARM64_LIB_ENTRY"
        }
        patchNativeLibraryFile(arm64Lib, patchArm64 = true)

        val armv7Lib = get(ARMV7_LIB_ENTRY)
        check(armv7Lib.exists() && armv7Lib.isFile) {
            "Expected native library not found: $ARMV7_LIB_ENTRY"
        }
        patchNativeLibraryFile(armv7Lib, patchArm64 = false)
    }
}
