package mihon.core.common.archive

import android.system.StructStat
import me.zhanghai.android.libarchive.ArchiveEntry.AE_IFDIR
import me.zhanghai.android.libarchive.ArchiveEntry.AE_IFREG
import me.zhanghai.android.libarchive.ArchiveException
import java.io.FileDescriptor
import java.nio.ByteBuffer
import me.zhanghai.android.libarchive.ArchiveEntry as LibEntry

/** One entry of a [FakeArchiveNative] archive; `utf8Name == null` falls back to [rawName]. */
internal data class FakeEntry(
    val utf8Name: String?,
    val rawName: ByteArray? = null,
    val isFile: Boolean = true,
    val isEncrypted: Boolean = false,
    val data: ByteArray = ByteArray(0),
)

/**
 * An in-memory [ArchiveNative]. Read handles are opened archives over [entries]; entry handles
 * are `index + 1` so that 0 keeps meaning "no more entries". Every call is appended to [calls]
 * so tests can check ordering and that each handle is freed exactly once.
 */
internal class FakeArchiveNative(private val entries: List<FakeEntry> = emptyList()) : ArchiveNative {
    val calls: MutableList<String> = mutableListOf()
    val passphrases: MutableList<ByteArray> = mutableListOf()
    val written: MutableList<Pair<String, ByteArray>> = mutableListOf()
    var openError: ArchiveException? = null
    var headerError: ArchiveException? = null
    var dataError: ArchiveException? = null
    var writeOpenError: ArchiveException? = null
    var fileData: ByteArray = ByteArray(0)
    var stat: StructStat? = null
    private val cursors = mutableMapOf<Long, Int>()
    private val offsets = mutableMapOf<Long, Int>()
    private var nextHandle = 100L
    private var pendingName = ""
    private var pendingData = ByteBuffer.allocate(0)
    private var fileOffset = 0

    private fun log(call: String) {
        calls += call
    }

    override fun readNew(): Long = nextHandle++.also {
        cursors[it] = -1
        log("readNew=$it")
    }

    override fun readAddPassphrase(archive: Long, passphrase: ByteArray) {
        passphrases += passphrase
        log("readAddPassphrase($archive)")
    }

    override fun setCharset(archive: Long, charset: ByteArray) = log("setCharset(${charset.decodeToString()})")

    override fun readSupportFilterAll(archive: Long) = log("readSupportFilterAll")

    override fun readSupportFormatAll(archive: Long) = log("readSupportFormatAll")

    override fun readOpenMemoryUnsafe(archive: Long, buffer: Long, size: Long) {
        log("readOpenMemoryUnsafe($archive,$buffer,$size)")
        openError?.let { throw it }
    }

    override fun readData(archive: Long, buffer: ByteBuffer) {
        dataError?.let { throw it }
        val data = entries[cursors.getValue(archive)].data
        val offset = offsets.getValue(archive)
        val count = minOf(buffer.remaining(), data.size - offset)
        buffer.put(data, offset, count)
        offsets[archive] = offset + count
    }

    override fun readNextHeader(archive: Long): Long {
        headerError?.let { throw it }
        val next = cursors.getValue(archive) + 1
        cursors[archive] = next
        offsets[archive] = 0
        return if (next < entries.size) next + 1L else 0L
    }

    override fun readFree(archive: Long) = log("readFree($archive)")

    override fun entryPathnameUtf8(entry: Long): String? = entries[entry.toInt() - 1].utf8Name

    override fun entryPathname(entry: Long): ByteArray? = entries[entry.toInt() - 1].rawName

    override fun entryFiletype(entry: Long): Int = if (entries[entry.toInt() - 1].isFile) AE_IFREG else AE_IFDIR

    override fun entryIsEncrypted(entry: Long): Boolean = entries[entry.toInt() - 1].isEncrypted

    override fun writeNew(): Long = nextHandle++.also { log("writeNew=$it") }

    override fun entryNew2(archive: Long): Long = nextHandle++.also { log("entryNew2=$it") }

    override fun writeSetFormatZip(archive: Long) = log("writeSetFormatZip")

    override fun writeZipSetCompressionStore(archive: Long) = log("writeZipSetCompressionStore")

    override fun writeSetOptions(archive: Long, options: ByteArray) =
        log("writeSetOptions(${options.decodeToString()})")

    override fun writeSetPassphrase(archive: Long, passphrase: ByteArray) {
        passphrases += passphrase
        log("writeSetPassphrase")
    }

    override fun writeOpenFd(archive: Long, fd: Int) {
        log("writeOpenFd")
        writeOpenError?.let { throw it }
    }

    override fun entryClear(entry: Long) {
        pendingData = ByteBuffer.allocate(fileData.size + 1)
        log("entryClear")
    }

    override fun entrySetPathnameUtf8(entry: Long, name: String?) {
        pendingName = name.orEmpty()
    }

    override fun entrySetStat(entry: Long, stat: LibEntry.StructStat) = log("entrySetStat(${stat.stSize})")

    override fun entrySetSize(entry: Long, size: Long) {
        pendingData = ByteBuffer.allocate(size.toInt())
        log("entrySetSize($size)")
    }

    override fun entrySetFiletype(entry: Long, type: Int) = log("entrySetFiletype($type)")

    override fun writeHeader(archive: Long, entry: Long) = log("writeHeader($pendingName)")

    override fun writeData(archive: Long, buffer: ByteBuffer) {
        log("writeData(${buffer.remaining()})")
        pendingData.put(buffer)
    }

    override fun writeFinishEntry(archive: Long) {
        written += pendingName to pendingData.array().copyOf(pendingData.position())
        log("writeFinishEntry")
    }

    override fun entryFree(entry: Long) = log("entryFree($entry)")

    override fun writeFree(archive: Long) = log("writeFree($archive)")

    override fun mmap(size: Long, fd: FileDescriptor): Long = MAPPING.also { log("mmap($size)") }

    override fun munmap(address: Long, size: Long) = log("munmap($address,$size)")

    override fun fstat(fd: FileDescriptor): StructStat = checkNotNull(stat) { "stat not set by the test" }

    override fun read(fd: FileDescriptor, buffer: ByteBuffer): Int {
        val count = minOf(buffer.remaining(), fileData.size - fileOffset)
        buffer.put(fileData, fileOffset, count)
        fileOffset += count
        return count
    }

    companion object {
        const val MAPPING: Long = 0xABCDL
    }
}
