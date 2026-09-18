package mihon.core.common.archive

import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import mihon.core.common.NativeBinding
import java.io.FileDescriptor
import java.nio.ByteBuffer

/** [ArchiveNative] over the real libarchive JNI binding and `android.system.Os`. */
@NativeBinding
internal object LibArchive :
    ArchiveNative,
    ArchiveReadNative by LibArchiveRead,
    ArchiveEntryReadNative by LibArchiveEntryRead,
    ArchiveEntryWriteNative by LibArchiveEntryWrite,
    ArchiveWriteNative by LibArchiveWrite,
    ArchiveOsNative by LibOs

@NativeBinding
private object LibArchiveRead : ArchiveReadNative {
    override fun readNew(): Long = Archive.readNew()
    override fun readAddPassphrase(archive: Long, passphrase: ByteArray) =
        Archive.readAddPassphrase(archive, passphrase)
    override fun setCharset(archive: Long, charset: ByteArray) = Archive.setCharset(archive, charset)
    override fun readSupportFilterAll(archive: Long) = Archive.readSupportFilterAll(archive)
    override fun readSupportFormatAll(archive: Long) = Archive.readSupportFormatAll(archive)
    override fun readOpenMemoryUnsafe(archive: Long, buffer: Long, size: Long) =
        Archive.readOpenMemoryUnsafe(archive, buffer, size)
    override fun readData(archive: Long, buffer: ByteBuffer) = Archive.readData(archive, buffer)
    override fun readNextHeader(archive: Long): Long = Archive.readNextHeader(archive)
    override fun readFree(archive: Long) = Archive.readFree(archive)
}

@NativeBinding
private object LibArchiveEntryRead : ArchiveEntryReadNative {
    override fun entryPathnameUtf8(entry: Long): String? = ArchiveEntry.pathnameUtf8(entry)
    override fun entryPathname(entry: Long): ByteArray? = ArchiveEntry.pathname(entry)
    override fun entryFiletype(entry: Long): Int = ArchiveEntry.filetype(entry)
    override fun entryIsEncrypted(entry: Long): Boolean = ArchiveEntry.isEncrypted(entry)
}

@NativeBinding
private object LibArchiveEntryWrite : ArchiveEntryWriteNative {
    override fun entryNew2(archive: Long): Long = ArchiveEntry.new2(archive)
    override fun entryClear(entry: Long) = ArchiveEntry.clear(entry)
    override fun entrySetPathnameUtf8(entry: Long, name: String?) = ArchiveEntry.setPathnameUtf8(entry, name)
    override fun entrySetStat(entry: Long, stat: ArchiveEntry.StructStat) = ArchiveEntry.setStat(entry, stat)
    override fun entrySetSize(entry: Long, size: Long) = ArchiveEntry.setSize(entry, size)
    override fun entrySetFiletype(entry: Long, type: Int) = ArchiveEntry.setFiletype(entry, type)
    override fun entryFree(entry: Long) = ArchiveEntry.free(entry)
}

@NativeBinding
private object LibArchiveWrite : ArchiveWriteNative {
    override fun writeNew(): Long = Archive.writeNew()
    override fun writeSetFormatZip(archive: Long) = Archive.writeSetFormatZip(archive)
    override fun writeZipSetCompressionStore(archive: Long) = Archive.writeZipSetCompressionStore(archive)
    override fun writeSetOptions(archive: Long, options: ByteArray) = Archive.writeSetOptions(archive, options)
    override fun writeSetPassphrase(archive: Long, passphrase: ByteArray) =
        Archive.writeSetPassphrase(archive, passphrase)
    override fun writeOpenFd(archive: Long, fd: Int) = Archive.writeOpenFd(archive, fd)
    override fun writeHeader(archive: Long, entry: Long) = Archive.writeHeader(archive, entry)
    override fun writeData(archive: Long, buffer: ByteBuffer) = Archive.writeData(archive, buffer)
    override fun writeFinishEntry(archive: Long) = Archive.writeFinishEntry(archive)
    override fun writeFree(archive: Long) = Archive.writeFree(archive)
}

@NativeBinding
private object LibOs : ArchiveOsNative {
    override fun mmap(size: Long, fd: FileDescriptor): Long =
        Os.mmap(0, size, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, fd, 0)
    override fun munmap(address: Long, size: Long) = Os.munmap(address, size)
    override fun fstat(fd: FileDescriptor): StructStat = Os.fstat(fd)
    override fun read(fd: FileDescriptor, buffer: ByteBuffer): Int = Os.read(fd, buffer)
}
