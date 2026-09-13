package tachiyomi.core.common.util.system

import android.os.Build
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil.ImageType
import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageDecoder
import java.io.File
import java.io.InputStream

/** Recognises image formats from file names and magic bytes. */
internal object ImageTypeDetection {
    fun isImage(name: String?, openStream: (() -> InputStream)? = null): Boolean {
        if (name == null) return false
        // SY -->
        if (File(name).extension.equals("cbi", ignoreCase = true)) return true
        // SY <--

        val extension = name.substringAfterLast('.')
        return ImageType.entries.any { it.extension == extension } || openStream?.let { findImageType(it) } != null
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    fun findImageType(stream: InputStream): ImageType? {
        return try {
            when (getImageType(stream)?.format) {
                Format.Avif -> ImageType.AVIF
                Format.Gif -> ImageType.GIF
                Format.Heif -> ImageType.HEIF
                Format.Jpeg -> ImageType.JPEG
                Format.Jxl -> ImageType.JXL
                Format.Png -> ImageType.PNG
                Format.Webp -> ImageType.WEBP
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getExtensionFromMimeType(mime: String?, openStream: () -> InputStream): String {
        val type = mime?.let { ImageType.entries.find { it.mime == mime } } ?: findImageType(openStream)
        return type?.extension ?: "jpg"
    }

    fun isAnimatedAndSupported(source: BufferedSource): Boolean {
        return try {
            val type = getImageType(source.peek().inputStream()) ?: return false
            // https://coil-kt.github.io/coil/getting_started/#supported-image-formats
            when (type.format) {
                Format.Gif -> true
                // Animated WebP on Android 9+
                Format.Webp -> type.isAnimated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                // Animated Heif on Android 11+
                Format.Heif -> type.isAnimated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getImageType(stream: InputStream): tachiyomi.decoder.ImageType? {
        val bytes = ByteArray(32)

        val length = if (stream.markSupported()) {
            stream.mark(bytes.size)
            stream.read(bytes, 0, bytes.size).also { stream.reset() }
        } else {
            stream.read(bytes, 0, bytes.size)
        }

        if (length == -1) {
            return null
        }

        return ImageDecoder.findType(bytes)
    }
}
