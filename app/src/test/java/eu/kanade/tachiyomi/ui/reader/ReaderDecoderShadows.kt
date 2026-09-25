package eu.kanade.tachiyomi.ui.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageDecoder
import tachiyomi.decoder.ImageType
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Package prefix Robolectric must instrument so [ReaderShadowDecoder] can replace the JNI decoder. */
internal const val DECODER_PACKAGE: String = "tachiyomi.decoder"

/**
 * Stands in for the JNI-backed [ImageDecoder] (the core/common tests carry the same shadow): the
 * static initialiser only wires the companion, and decoding goes through [BitmapFactory].
 */
@Implements(ImageDecoder::class)
internal class ReaderShadowDecoder {
    var bitmap: Bitmap? = null

    @Implementation
    fun decode(region: Rect?, sampleSize: Int): Bitmap? {
        require(region == null && sampleSize == 1) { "the shadow only decodes whole images at full size" }
        return bitmap
    }

    @Implementation
    fun recycle() {
        bitmap = null
    }

    companion object {
        @JvmStatic
        @Implementation
        fun __staticInitializer__() {
            val constructor = ImageDecoder.Companion::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            ReflectionHelpers.setStaticField(ImageDecoder::class.java, "Companion", constructor.newInstance())
        }
    }
}

/** The companion half of [ReaderShadowDecoder]: PNG / JPEG sniffing and instance creation. */
@Implements(ImageDecoder.Companion::class)
internal class ReaderShadowDecoderCompanion {
    @Implementation
    fun newInstance(stream: InputStream, cropBorders: Boolean, displayProfile: ByteArray?): ImageDecoder? {
        require(!cropBorders && displayProfile == null) { "the shadow only supports the default options" }
        val bitmap = BitmapFactory.decodeStream(stream) ?: return null
        val decoder = ReflectionHelpers.callConstructor(
            ImageDecoder::class.java,
            ClassParameter.from(java.lang.Long.TYPE, 0L),
            ClassParameter.from(Integer.TYPE, bitmap.width),
            ClassParameter.from(Integer.TYPE, bitmap.height),
        )
        Shadow.extract<ReaderShadowDecoder>(decoder).bitmap = bitmap
        return decoder
    }

    @Implementation
    fun findType(bytes: ByteArray): ImageType? {
        val format = when {
            bytes.size > 3 && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() -> Format.Png
            bytes.size > 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> Format.Jpeg
            else -> return null
        }
        return ImageType::class.java
            .getConstructor(Format::class.java, java.lang.Boolean.TYPE)
            .newInstance(format, false)
    }
}

/** A [width] x [height] PNG, encoded by the sandbox's native graphics. */
internal fun pngBytes(width: Int = 4, height: Int = 4): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}
