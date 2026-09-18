package mihon.core.common.archive

import android.content.Context
import android.system.StructStat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveEntry.AE_IFREG
import me.zhanghai.android.libarchive.ArchiveException
import mihon.core.common.NativeBinding
import tachiyomi.core.common.storage.openFileDescriptor
import java.io.Closeable
import java.nio.ByteBuffer

/** Writes a ZIP archive, optionally encrypted (SY), through libarchive. */
public class ZipWriter internal constructor(
    /** Used to open file descriptors. */
    public val context: Context,
    file: UniFile,
    // SY -->
    encrypt: Boolean,
    // SY <--
    private val native: ArchiveNative,
) : Closeable {
    private val pfd = file.openFileDescriptor(context, "wt")
    private val archive = native.writeNew()
    private val entry = native.entryNew2(archive)
    private val buffer = ByteBuffer.allocateDirect(
        // SY -->
        BUFFER_SIZE,
        // SY <--
    )

    init {
        try {
            native.setCharset(archive, Charsets.UTF_8.name().toByteArray())
            native.writeSetFormatZip(archive)
            native.writeZipSetCompressionStore(archive)
            // SY -->
            if (encrypt) {
                native.writeSetOptions(archive, CbzCrypto.getPreferredEncryptionAlgo())
                native.writeSetPassphrase(archive, CbzCrypto.getDecryptedPasswordCbz())
            }
            // SY <--
            native.writeOpenFd(archive, pfd.fd)
        } catch (e: ArchiveException) {
            close()
            throw e
        }
    }

    @NativeBinding
    public constructor(
        context: Context,
        file: UniFile,
        // SY -->
        encrypt: Boolean,
        // SY <--
    ) : this(context, file, encrypt, LibArchive)

    /** Adds [file] under its own name. */
    public fun write(file: UniFile) {
        file.openFileDescriptor(context, "r").use {
            val fd = it.fileDescriptor
            native.entryClear(entry)
            native.entrySetPathnameUtf8(entry, file.name)
            val stat = native.fstat(fd)
            native.entrySetStat(entry, stat.toArchiveStat())
            native.writeHeader(archive, entry)
            while (true) {
                buffer.clear()
                native.read(fd, buffer)
                if (buffer.position() == 0) break
                buffer.flip()
                native.writeData(archive, buffer)
            }
            native.writeFinishEntry(archive)
        }
    }

    // SY -->

    /** Adds [fileData] as [fileName]. */
    public fun write(fileData: ByteArray, fileName: String) {
        native.entryClear(entry)
        native.entrySetPathnameUtf8(entry, fileName)
        native.entrySetSize(entry, fileData.size.toLong())
        native.entrySetFiletype(entry, AE_IFREG)
        native.writeHeader(archive, entry)

        var position = 0
        while (position < fileData.size) {
            val lengthToRead = minOf(BUFFER_SIZE, fileData.size - position)
            buffer.clear()
            buffer.put(fileData, position, lengthToRead)
            buffer.flip()
            native.writeData(archive, buffer)
            position += lengthToRead
        }
        native.writeFinishEntry(archive)
    }
    // SY <--

    override fun close() {
        native.entryFree(entry)
        native.writeFree(archive)
        pfd.close()
    }

    // SY -->

    /** Libarchive format and filter constants. */
    public companion object {
        private const val BUFFER_SIZE = 8192
    }
    // SY <--
}

internal fun StructStat.toArchiveStat(): ArchiveEntry.StructStat = ArchiveEntry.StructStat().apply {
    stDev = st_dev
    stMode = st_mode
    stNlink = st_nlink.toInt()
    stUid = st_uid
    stGid = st_gid
    stRdev = st_rdev
    stSize = st_size
    stBlksize = st_blksize
    stBlocks = st_blocks
    stAtim = timespec(st_atime)
    stMtim = timespec(st_mtime)
    stCtim = timespec(st_ctime)
    stIno = st_ino
}

private fun timespec(tvSec: Long) = ArchiveEntry.StructTimespec().also { it.tvSec = tvSec }
