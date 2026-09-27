package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import android.graphics.Rect
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import tachiyomi.decoder.ImageDecoder
import java.io.InputStream

/** What the pager's decodes return, in call order; an empty queue (or a queued null) fails the decode. */
internal object PagerDecodes {
    /** The region and sample size of the last decode (the whole image by default). */
    var lastRegion: Pair<Rect?, Int>? = null

    val bitmaps: ArrayDeque<Bitmap?> = ArrayDeque()

    /** How many decoders were recycled. */
    var recycled: Int = 0

    /** Makes the next decode throw, as the native decoder does on a corrupt image. */
    var throwNext: Boolean = false

    fun portrait(): Bitmap = Bitmap.createBitmap(2, 4, Bitmap.Config.ARGB_8888)

    fun landscape(): Bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
}

/** Stands in for the JNI decoder: `decode` hands out [PagerDecodes] in order. */
@Implements(ImageDecoder::class)
internal class PagerShadowDecoder {
    @Implementation
    fun decode(region: Rect?, sampleSize: Int): Bitmap? {
        PagerDecodes.lastRegion = region to sampleSize
        if (PagerDecodes.throwNext) {
            PagerDecodes.throwNext = false
            error("corrupt")
        }
        return PagerDecodes.bitmaps.removeFirstOrNull()
    }

    @Implementation
    fun recycle() {
        PagerDecodes.recycled++
    }

    companion object {
        /** Replaces the native static initialiser: creates the companion, skips the library. */
        @JvmStatic
        @Implementation
        fun __staticInitializer__() {
            val constructor = ImageDecoder.Companion::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            ReflectionHelpers.setStaticField(ImageDecoder::class.java, "Companion", constructor.newInstance())
        }
    }
}

/** The companion half of [PagerShadowDecoder]: every stream opens, whatever it holds. */
@Implements(ImageDecoder.Companion::class)
internal class PagerShadowDecoderCompanion {
    @Implementation
    fun newInstance(stream: InputStream, cropBorders: Boolean, displayProfile: ByteArray?): ImageDecoder? {
        check(!cropBorders && displayProfile == null) { "only the default options" }
        stream.readBytes()
        val decoder = ReflectionHelpers.callConstructor(
            ImageDecoder::class.java,
            ClassParameter.from(java.lang.Long.TYPE, 0L),
            ClassParameter.from(Integer.TYPE, 1),
            ClassParameter.from(Integer.TYPE, 1),
        )
        Shadow.extract<PagerShadowDecoder>(decoder)
        return decoder
    }
}
