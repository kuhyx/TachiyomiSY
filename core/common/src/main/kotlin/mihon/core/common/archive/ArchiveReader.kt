package mihon.core.common.archive

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import com.hippo.unifile.UniFile
import me.zhanghai.android.libarchive.ArchiveException
import tachiyomi.core.common.storage.openFileDescriptor
import java.io.Closeable
import java.io.InputStream

/** An archive mapped into memory for repeated entry lookups. */
public class ArchiveReader(pfd: ParcelFileDescriptor) : Closeable {
    /** Archive size in bytes. */
    public val size: Long = pfd.statSize

    /** Address of the memory mapping. */
    public val address: Long = Os.mmap(0, size, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, pfd.fileDescriptor, 0)

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

    /** Runs [block] over the entries and closes the stream afterwards. */
    public inline fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T = ArchiveInputStream(
        address,
        size,
        // SY -->
        encrypted,
        // SY <--
    ).use { block(generateSequence { it.getNextEntry() }) }

    /** A stream over the entry named [entryName], or null when absent. */
    public fun getInputStream(entryName: String): InputStream? {
        val archive = ArchiveInputStream(address, size, /* SY --> */ encrypted /* SY <-- */)
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

    // SY -->
    private fun checkEncryptionStatus() {
        val archive = ArchiveInputStream(address, size, false)
        try {
            while (true) {
                val entry = archive.getNextEntry() ?: break
                if (entry.isEncrypted) {
                    encrypted = true
                    isPasswordIncorrect(entry.name)
                    break
                }
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
        Os.munmap(address, size)
    }
}

/** An [ArchiveReader] over this file. */
public fun UniFile.archiveReader(context: Context): ArchiveReader =
    openFileDescriptor(context, "r").use { ArchiveReader(it) }
