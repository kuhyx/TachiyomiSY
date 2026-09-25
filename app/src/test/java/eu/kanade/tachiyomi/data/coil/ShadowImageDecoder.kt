package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Rect
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import tachiyomi.decoder.ImageDecoder
import java.io.InputStream

/** Package prefix Robolectric must instrument so [ShadowImageDecoder] can replace the JNI decoder. */
internal const val DECODER_PACKAGE: String = "tachiyomi.decoder"

/** What the next [ImageDecoder.Companion.newInstance] hands back; set by each test. */
internal object DecoderScript {
    /** Null makes `newInstance` fail like the native decoder does on garbage. */
    var size: Pair<Int, Int>? = 100 to 100

    /** What `decode` returns. */
    var bitmap: Bitmap? = null

    /** The stream `newInstance` was last given, read to the end. */
    var lastInput: String = ""

    /** The sample size `decode` was last asked for. */
    var lastSampleSize: Int = 0

    /** The region `decode` was last asked for (the whole image by default). */
    var lastRegion: Rect? = null

    fun reset() {
        size = 100 to 100
        bitmap = null
        lastInput = ""
        lastSampleSize = 0
        lastRegion = null
    }
}

/**
 * Stands in for the JNI-backed [ImageDecoder], whose static initialiser calls `System.loadLibrary`
 * and cannot load on the JVM. Requires `@Config(instrumentedPackages = [DECODER_PACKAGE], shadows = [...])`.
 */
@Implements(ImageDecoder::class)
internal class ShadowImageDecoder {
    @Implementation
    fun decode(region: Rect?, sampleSize: Int): Bitmap? {
        DecoderScript.lastRegion = region
        DecoderScript.lastSampleSize = sampleSize
        return DecoderScript.bitmap
    }

    @Implementation
    fun recycle() {
        DecoderScript.lastSampleSize = -1
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

/** The companion half of [ShadowImageDecoder]: instance creation from [DecoderScript]. */
@Implements(ImageDecoder.Companion::class)
internal class ShadowImageDecoderCompanion {
    @Implementation
    fun newInstance(stream: InputStream, cropBorders: Boolean, displayProfile: ByteArray?): ImageDecoder? {
        require(!cropBorders && displayProfile == null) { "the shadow only supports the default options" }
        DecoderScript.lastInput = stream.readBytes().decodeToString()
        val (width, height) = DecoderScript.size ?: return null
        val decoder = ReflectionHelpers.callConstructor(
            ImageDecoder::class.java,
            ClassParameter.from(java.lang.Long.TYPE, 0L),
            ClassParameter.from(Integer.TYPE, width),
            ClassParameter.from(Integer.TYPE, height),
        )
        Shadow.extract<ShadowImageDecoder>(decoder)
        return decoder
    }
}
