package eu.kanade.tachiyomi.data.saver

import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.robolectric.util.ReflectionHelpers
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** The first bytes of a JPEG, padded to the header length the type sniffer reads. */
internal val jpegBytes: ByteArray =
    byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(28)

internal fun jpegPage(name: String, location: Location): Image.Page =
    Image.Page(inputStream = { ByteArrayInputStream(jpegBytes) }, name = name, location = location)

/** Runs [block] with `Build.VERSION.SDK_INT` pretending to be [sdk]. */
internal fun <T> withSdk(sdk: Int, block: () -> T): T {
    val real = Build.VERSION.SDK_INT
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    return try {
        block()
    } finally {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", real)
    }
}

/** The type sniffer is JNI-backed; the saver's tests decide what it answers. */
internal fun sniffAs(type: ImageUtil.ImageType?) {
    mockkObject(ImageUtil)
    every { ImageUtil.findImageType(any<() -> InputStream>()) } returns type
}

/**
 * FileProvider caches its roots per authority for the whole JVM, so a root from an earlier test's
 * temporary directory would reject this one's files; the file's own `file://` uri stands in.
 */
internal fun plainFileUris() {
    mockkStatic(FileProvider::class)
    every { FileProvider.getUriForFile(any(), any(), any()) } answers { Uri.fromFile(thirdArg<File>()) }
}

/** Whether MediaStore should treat the image's mime type as a known image type. */
internal fun mimeKnown(known: Boolean) {
    val mimeTypes = mockk<MimeTypeMap>()
    every { mimeTypes.hasMimeType(any()) } returns known
    mockkStatic(MimeTypeMap::class)
    every { MimeTypeMap.getSingleton() } returns mimeTypes
}
