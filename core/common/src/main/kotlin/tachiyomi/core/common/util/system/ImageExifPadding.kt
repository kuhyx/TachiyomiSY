package tachiyomi.core.common.util.system

import androidx.exifinterface.media.ExifInterface
import logcat.LogPriority
import java.io.File
import java.io.IOException
import java.security.SecureRandom

/** Random EXIF padding so files inside CBZ archives get unique sizes. */
internal object ImageExifPadding {
    private const val PADDING_BYTES = 16_384

    /**
     * Creates random exif metadata used as padding to make
     * the size of files inside CBZ archives unique.
     */
    fun addPaddingToImageExif(imageFile: File) {
        try {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val length = SecureRandom().nextInt(PADDING_BYTES) + PADDING_BYTES
            val padding = List(length) { charPool.random() }.joinToString("")
            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute("UserComment", padding)
            exif.saveAttributes()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e)
        } catch (e: UnsupportedOperationException) {
            logcat(LogPriority.ERROR, e)
        }
    }
    // SY <--
}
