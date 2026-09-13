package tachiyomi.core.common.util.system

import androidx.exifinterface.media.ExifInterface
import logcat.LogPriority
import java.io.File
import java.security.SecureRandom

/** Random EXIF padding so files inside CBZ archives get unique sizes. */
internal object ImageExifPadding {
    /**
     * Creates random exif metadata used as padding to make
     * the size of files inside  CBZ archives unique
     */
    fun addPaddingToImageExif(imageFile: File) {
        try {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val padding = List(SecureRandom().nextInt(16384) + 16384) { charPool.random() }.joinToString("")
            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute("UserComment", padding)
            exif.saveAttributes()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
    // SY <--
}
