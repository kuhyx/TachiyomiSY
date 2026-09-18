package mihon.core.common.archive

import android.content.Context
import android.os.ParcelFileDescriptor
import com.hippo.unifile.UniFile
import me.zhanghai.android.libarchive.ArchiveException
import mihon.core.common.NativeBinding
import tachiyomi.core.common.storage.openFileDescriptor
import java.io.Closeable
import java.io.InputStream

/** An archive mapped into memory for repeated entry lookups. */
public class ArchiveReader internal constructor(
    pfd: ParcelFileDescriptor,
    private val native: ArchiveNative,
) : Closeable {
    /** Archive size in bytes. */
    public val size: Long = pfd.statSize

    /** Address of the memory mapping. */
    public val address: Long = native.mmap(size, pfd.fileDescriptor)

    // SY -->

    /** SY: true when the archive holds encrypted entries. */
    public var encrypted: Boolean = false
        private set

    /** SY: true when the stored password failed, null until tried. */
    public var wrongPassword: Boolean? = null
        private set

    /** Identity of the underlying descriptor, for caches. */
    public val archiveHashCode: Int = pfd.hashCode()

    init {
        checkEncryptionStatus()
    }
    // SY <--

    @NativeBinding
    public constructor(pfd: ParcelFileDescriptor) : this(pfd, LibArchive)

    /** Runs [block] over the entries and closes the stream afterwards. */
    public inline fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T =
        openStream(encrypted).use { block(generateSequence { it.getNextEntry() }) }

    /** A stream over the entry named [entryName], or null when absent. */
    public fun getInputStream(entryName: String): InputStream? {
        val archive = openStream(encrypted)
        try {
            while (true) {
                val entry = archive.getNextEntry() ?: break
                if (entry.name == entryName) {
                    return archive
                }
            }
        } catch (e: ArchiveException) {
            archive.close()
            throw e
        }
        archive.close()
        return null
    }

    /** A fresh stream over the whole mapping, for [useEntries] to inline. */
    @PublishedApi
    internal fun openStream(encrypted: Boolean): ArchiveInputStream =
        ArchiveInputStream(address, size, /* SY --> */ encrypted, /* SY <-- */ native)

    // SY -->
    private fun checkEncryptionStatus() {
        val archive = openStream(false)
        try {
            val encryptedEntry = generateSequence { archive.getNextEntry() }.firstOrNull { it.isEncrypted }
            if (encryptedEntry != null) {
                encrypted = true
                isPasswordIncorrect(encryptedEntry.name)
            }
        } catch (e: ArchiveException) {
            archive.close()
            throw e
        }
        archive.close()
    }

    private fun isPasswordIncorrect(entryName: String) {
        try {
            getInputStream(entryName).use { stream ->
                stream!!.read()
            }
        } catch (e: ArchiveException) {
            if (e.message == "Incorrect passphrase") {
                wrongPassword = true
                return
            }
            throw e
        }
        wrongPassword = false
    }
    // SY <--

    override fun close() {
        native.munmap(address, size)
    }
}

/** An [ArchiveReader] over this file. */
@NativeBinding
public fun UniFile.archiveReader(context: Context): ArchiveReader =
    openFileDescriptor(context, "r").use { ArchiveReader(it) }
