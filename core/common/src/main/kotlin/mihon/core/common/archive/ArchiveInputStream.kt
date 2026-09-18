package mihon.core.common.archive

import eu.kanade.tachiyomi.util.storage.CbzCrypto
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveException
import mihon.core.common.NativeBinding
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.concurrent.Volatile
import mihon.core.common.archive.ArchiveEntry as MihonArchiveEntry

/** Reads an archive mapped in memory entry by entry through libarchive. */
public class ArchiveInputStream internal constructor(
    buffer: Long,
    size: Long,
    // SY -->
    encrypted: Boolean,
    // SY <--
    private val native: ArchiveNative,
) : InputStream() {
    private val lock = Any()

    @Volatile
    private var isClosed = false

    private val archive = native.readNew()

    init {
        try {
            // SY -->
            if (encrypted) {
                native.readAddPassphrase(archive, CbzCrypto.getDecryptedPasswordCbz())
            }
            // SY <--
            native.setCharset(archive, Charsets.UTF_8.name().toByteArray())
            native.readSupportFilterAll(archive)
            native.readSupportFormatAll(archive)
            native.readOpenMemoryUnsafe(archive, buffer, size)
        } catch (e: ArchiveException) {
            close()
            throw e
        }
    }

    private val oneByteBuffer = ByteBuffer.allocateDirect(1)

    @NativeBinding
    public constructor(
        buffer: Long,
        size: Long,
        // SY -->
        encrypted: Boolean,
        // SY <--
    ) : this(buffer, size, encrypted, LibArchive)

    override fun read(): Int {
        read(oneByteBuffer)
        return if (oneByteBuffer.hasRemaining()) oneByteBuffer.get().toUByte().toInt() else -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // slice() so that clear() in read() keeps the window at [off, off + len).
        val buffer = ByteBuffer.wrap(b, off, len).slice()
        read(buffer)
        return if (buffer.hasRemaining()) buffer.remaining() else -1
    }

    private fun read(buffer: ByteBuffer) {
        buffer.clear()
        native.readData(archive, buffer)
        buffer.flip()
    }

    override fun close() {
        synchronized(lock) {
            if (isClosed) return
            isClosed = true
        }

        native.readFree(archive)
    }

    /** Advances to the next entry, or null at the end. */
    public fun getNextEntry(): MihonArchiveEntry? =
        native.readNextHeader(archive).takeUnless { it == 0L }?.let { entry ->
            val name = native.entryPathnameUtf8(entry)
                ?: native.entryPathname(entry)?.decodeToString()
                ?: return null
            val isFile = native.entryFiletype(entry) == ArchiveEntry.AE_IFREG
            // SY -->
            val isEncrypted = native.entryIsEncrypted(entry)
            // SY <--
            MihonArchiveEntry(
                name,
                isFile,
                // SY -->
                isEncrypted,
                // SY <--
            )
        }
}
