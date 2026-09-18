package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import tachiyomi.decoder.ImageDecoder
import tachiyomi.decoder.ImageType
import java.io.InputStream

/** Package prefix Robolectric must instrument so [ShadowImageDecoder] can replace the JNI decoder. */
internal const val DECODER_PACKAGE: String = "tachiyomi.decoder"

/**
 * Stands in for the JNI-backed [ImageDecoder], whose static initialiser calls `System.loadLibrary`
 * and cannot load on the JVM: the initialiser only wires the companion, and decoding goes through
 * [BitmapFactory]. Requires `@Config(instrumentedPackages = [DECODER_PACKAGE], shadows = [...])`.
 */
@Implements(ImageDecoder::class)
internal class ShadowImageDecoder {
    /** What [decode] hands back; null makes the decoder report failure like the native one does. */
    var bitmap: Bitmap? = null

    @Implementation
    fun decode(region: Rect?, sampleSize: Int): Bitmap? {
        require(sampleSize == 1) { "the shadow only decodes at full size" }
        val source = bitmap ?: return null
        return region?.let { Bitmap.createBitmap(source, it.left, it.top, it.width(), it.height()) } ?: source
    }

    @Implementation
    fun recycle() {
        bitmap = null
    }

    companion object {
        /** Replaces the real static initialiser: creates the companion, skips the native library. */
        @JvmStatic
        @Implementation
        fun __staticInitializer__() {
            val constructor = ImageDecoder.Companion::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            ReflectionHelpers.setStaticField(ImageDecoder::class.java, "Companion", constructor.newInstance())
        }
    }
}

/** The companion half of [ShadowImageDecoder]: type sniffing and instance creation. */
@Implements(ImageDecoder.Companion::class)
internal class ShadowImageDecoderCompanion {
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
        Shadow.extract<ShadowImageDecoder>(decoder).bitmap = bitmap
        return decoder
    }

    @Implementation
    fun findType(bytes: ByteArray): ImageType? = ImageMagic.sniff(bytes)
}
