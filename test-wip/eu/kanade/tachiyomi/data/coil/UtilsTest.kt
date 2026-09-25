package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

private fun options(request: ImageRequest): Options = Options(context = request.context, extras = request.extras)

private fun sizeMethod(name: String): Method = Class.forName("eu.kanade.tachiyomi.data.coil.UtilsKt")
    .getDeclaredMethod(name, Size::class.java, Scale::class.java, Function0::class.java)

private fun Method.callPx(size: Size, scale: Scale, original: () -> Int): Int =
    invoke(null, size, scale, original) as Int

internal class UtilsTest {

    private val context: Context = mockk(relaxed = true)
    private val known = Size(width = 30, height = 40)

    @Test
    fun originalSizeUsesFallback() {
        sizeMethod("widthPx").callPx(Size.ORIGINAL, Scale.FIT) { 11 } shouldBe 11
        sizeMethod("heightPx").callPx(Size.ORIGINAL, Scale.FIT) { 22 } shouldBe 22
    }

    @Test
    fun knownSizeUsesItsPixels() {
        sizeMethod("widthPx").callPx(known, Scale.FIT) { 0 } shouldBe 30
        sizeMethod("heightPx").callPx(known, Scale.FIT) { 0 } shouldBe 40
    }

    @Test
    fun inlinedCallsAgreeWithTheMethod() {
        known.widthPx(Scale.FIT) { 0 } shouldBe 30
        known.heightPx(Scale.FIT) { 0 } shouldBe 40
        Size.ORIGINAL.widthPx(Scale.FILL) { 5 } shouldBe 5
        Size.ORIGINAL.heightPx(Scale.FILL) { 6 } shouldBe 6
    }

    @Test
    fun undefinedDimensionFollowsScale() {
        Dimension.Undefined.toPx(Scale.FILL) shouldBe Int.MIN_VALUE
        Dimension.Undefined.toPx(Scale.FIT) shouldBe Int.MAX_VALUE
    }

    @Test
    fun pixelDimensionIgnoresScale() {
        Dimension(5).toPx(Scale.FILL) shouldBe 5
        Dimension(5).toPx(Scale.FIT) shouldBe 5
    }

    @Test
    fun cropBordersDefaultsToFalse() {
        options(ImageRequest.Builder(context).build()).cropBorders shouldBe false
    }

    @Test
    fun cropBordersRoundTrips() {
        options(ImageRequest.Builder(context).cropBorders(true).build()).cropBorders shouldBe true
        options(ImageRequest.Builder(context).cropBorders(false).build()).cropBorders shouldBe false
    }

    @Test
    fun customDecoderDefaultsToFalse() {
        options(ImageRequest.Builder(context).build()).customDecoder shouldBe false
    }

    @Test
    fun customDecoderRoundTrips() {
        options(ImageRequest.Builder(context).customDecoder(true).build()).customDecoder shouldBe true
        options(ImageRequest.Builder(context).customDecoder(false).build()).customDecoder shouldBe false
    }
}
