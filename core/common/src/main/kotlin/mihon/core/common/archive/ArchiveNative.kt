package mihon.core.common.archive

import android.system.StructStat
import me.zhanghai.android.libarchive.ArchiveEntry
import java.io.FileDescriptor
import java.nio.ByteBuffer

/**
 * The libarchive and `Os` calls the archive classes make, one method per native entry point.
 *
 * Every class here keeps its bookkeeping (argument handling, close ordering, exception mapping,
 * entry decoding) behind this seam so it runs on the JVM against a fake; [LibArchive] is the only
 * production implementation and the only code that touches the native library.
 */
internal interface ArchiveNative :
    ArchiveReadNative,
    ArchiveEntryReadNative,
    ArchiveEntryWriteNative,
    ArchiveWriteNative,
    ArchiveOsNative

/** `Archive.read*` and the charset shared with the writer. */
internal interface ArchiveReadNative {
    fun readNew(): Long
    fun readAddPassphrase(archive: Long, passphrase: ByteArray)
    fun setCharset(archive: Long, charset: ByteArray)
    fun readSupportFilterAll(archive: Long)
    fun readSupportFormatAll(archive: Long)
    fun readOpenMemoryUnsafe(archive: Long, buffer: Long, size: Long)
    fun readData(archive: Long, buffer: ByteBuffer)
    fun readNextHeader(archive: Long): Long
    fun readFree(archive: Long)
}

/** `ArchiveEntry.*` getters over an entry handle the reader was given. */
internal interface ArchiveEntryReadNative {
    fun entryPathnameUtf8(entry: Long): String?
    fun entryPathname(entry: Long): ByteArray?
    fun entryFiletype(entry: Long): Int
    fun entryIsEncrypted(entry: Long): Boolean
}

/** `ArchiveEntry.*` over the entry handle the writer owns. */
internal interface ArchiveEntryWriteNative {
    fun entryNew2(archive: Long): Long
    fun entryClear(entry: Long)
    fun entrySetPathnameUtf8(entry: Long, name: String?)
    fun entrySetStat(entry: Long, stat: ArchiveEntry.StructStat)
    fun entrySetSize(entry: Long, size: Long)
    fun entrySetFiletype(entry: Long, type: Int)
    fun entryFree(entry: Long)
}

/** `Archive.write*`. */
internal interface ArchiveWriteNative {
    fun writeNew(): Long
    fun writeSetFormatZip(archive: Long)
    fun writeZipSetCompressionStore(archive: Long)
    fun writeSetOptions(archive: Long, options: ByteArray)
    fun writeSetPassphrase(archive: Long, passphrase: ByteArray)
    fun writeOpenFd(archive: Long, fd: Int)
    fun writeHeader(archive: Long, entry: Long)
    fun writeData(archive: Long, buffer: ByteBuffer)
    fun writeFinishEntry(archive: Long)
    fun writeFree(archive: Long)
}

/** The `android.system.Os` calls: the read-only mapping and the file reads the writer copies from. */
internal interface ArchiveOsNative {
    fun mmap(size: Long, fd: FileDescriptor): Long
    fun munmap(address: Long, size: Long)
    fun fstat(fd: FileDescriptor): StructStat
    fun read(fd: FileDescriptor, buffer: ByteBuffer): Int
}
