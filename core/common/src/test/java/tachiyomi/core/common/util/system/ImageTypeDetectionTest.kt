package tachiyomi.core.common.util.system

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.util.system.ImageUtil.ImageType
import tachiyomi.decoder.Format
import java.io.ByteArrayInputStream
import java.io.InputStream

/** An [InputStream] without mark support, so [ImageTypeDetection.getImageType] takes its plain read path. */
internal class PlainStream(bytes: ByteArray) : InputStream() {
    private val delegate = ByteArrayInputStream(bytes)

    override fun read(): Int = delegate.read()

    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

    override fun markSupported(): Boolean = false
}

@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class ImageTypeDetectionTest {
    private fun stream(bytes: ByteArray): () -> InputStream = { ByteArrayInputStream(bytes) }

    @Test
    fun everyFormatMapsToItsImageType() {
        val expected = mapOf(
            ImageHeaders.avif to ImageType.AVIF,
            ImageHeaders.gif to ImageType.GIF,
            ImageHeaders.heif to ImageType.HEIF,
            ImageHeaders.jpeg to ImageType.JPEG,
            ImageHeaders.jxl to ImageType.JXL,
            ImageHeaders.png to ImageType.PNG,
            ImageHeaders.webp to ImageType.WEBP,
        )
        expected.forEach { (header, type) -> ImageTypeDetection.findImageType(stream(header)) shouldBe type }
    }

    @Test
    fun unknownOrEmptyHasNoType() {
        ImageTypeDetection.findImageType(stream("plain text".toByteArray())).shouldBeNull()
        ImageTypeDetection.findImageType(ByteArrayInputStream(ByteArray(0))).shouldBeNull()
    }

    @Test
    fun readFailuresAreSwallowed() {
        val broken = object : InputStream() {
            override fun read(): Int = throw java.io.IOException("closed")
        }
        ImageTypeDetection.findImageType(broken).shouldBeNull()
    }

    @Test
    fun headerIsReadWithAndWithoutMark() {
        val marked = ByteArrayInputStream(ImageHeaders.png)
        ImageTypeDetection.getImageType(marked)?.format shouldBe Format.Png
        marked.available() shouldBe ImageHeaders.png.size
        ImageTypeDetection.getImageType(PlainStream(ImageHeaders.gif))?.format shouldBe Format.Gif
        ImageTypeDetection.getImageType(PlainStream(ByteArray(0))).shouldBeNull()
    }

    @Test
    fun isImageChecksTheExtensionFirst() {
        ImageTypeDetection.isImage(null) shouldBe false
        ImageTypeDetection.isImage("page.PNG") shouldBe false
        ImageTypeDetection.isImage("page.png") shouldBe true
        ImageTypeDetection.isImage("archive.CBI") shouldBe true
        ImageTypeDetection.isImage("notes.txt") shouldBe false
    }

    @Test
    fun isImageSniffsUnknownExtension() {
        ImageTypeDetection.isImage("blob", stream(ImageHeaders.jpeg)) shouldBe true
        ImageTypeDetection.isImage("blob", stream("nope".toByteArray())) shouldBe false
        ImageTypeDetection.isImage("blob.jpg", stream("nope".toByteArray())) shouldBe true
    }

    @Test
    fun extensionPrefersTheMimeType() {
        ImageTypeDetection.getExtensionFromMimeType("image/webp", stream(ImageHeaders.png)) shouldBe "webp"
        ImageTypeDetection.getExtensionFromMimeType("image/unknown", stream(ImageHeaders.png)) shouldBe "png"
        ImageTypeDetection.getExtensionFromMimeType(null, stream(ImageHeaders.gif)) shouldBe "gif"
        ImageTypeDetection.getExtensionFromMimeType(null, stream("nope".toByteArray())) shouldBe "jpg"
    }

    @Test
    fun imageTypeCarriesMimeAndExt() {
        ImageType.entries.map { it.mime } shouldBe listOf(
            "image/avif", "image/gif", "image/heif", "image/jpeg", "image/jxl", "image/png", "image/webp",
        )
        ImageType.valueOf("JXL").extension shouldBe "jxl"
    }
}
