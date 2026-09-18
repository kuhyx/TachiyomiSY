package eu.kanade.tachiyomi.util.storage

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.core.common.archive.ArchiveReader
import tachiyomi.core.common.util.system.ImageUtil
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.SecureRandom

/** The archive-facing half of [CbzCrypto]: encryption settings, ComicInfo padding, cover detection. */
public abstract class CbzCryptoArchives : CbzCryptoKeys() {
    /** True when downloads must be password protected. */
    public fun getPasswordProtectDlPref(): Boolean = securityPreferences.passwordProtectDownloads.get()

    /** Random padding for ComicInfo.xml when downloads are protected, else null. */
    public fun createComicInfoPadding(): String? = if (getPasswordProtectDlPref()) {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        List(SecureRandom().nextInt(PADDING_RANGE) + PADDING_MIN) { charPool.random() }.joinToString("")
    } else {
        null
    }

    /** The configured cipher as the archive library expects it. */
    public fun getPreferredEncryptionAlgo(): ByteArray =
        when (securityPreferences.encryptionType.get()) {
            SecurityPreferences.EncryptionType.AES_256 -> "zip:encryption=aes256".toByteArray()
            SecurityPreferences.EncryptionType.AES_128 -> "zip:encryption=aes128".toByteArray()
            SecurityPreferences.EncryptionType.ZIP_STANDARD -> "zip:encryption=zipcrypt".toByteArray()
        }

    /** True when [stream] is a protected archive holding a cover image. */
    public fun detectCoverImageArchive(stream: InputStream): Boolean {
        val bytes = ByteArray(HEADER_BYTES)
        if (stream.markSupported()) {
            stream.mark(bytes.size)
            stream.read(bytes, 0, bytes.size).also { stream.reset() }
        } else {
            stream.read(bytes, 0, bytes.size)
        }
        return String(bytes).contains(defaultCoverName, ignoreCase = true)
    }

    /** The first image entry of the archive, or null. */
    public fun ArchiveReader.getCoverStream(): BufferedInputStream? {
        this.getInputStream(defaultCoverName)?.let { stream ->
            if (ImageUtil.isImage(defaultCoverName) { stream }) {
                return this.getInputStream(defaultCoverName)?.buffered()
            }
        }
        return null
    }
}
