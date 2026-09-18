package tachiyomi.core.common.util.system

import android.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ImageOptionsTest {
    @Test
    fun readsDimensionsWithoutReading() {
        val source = TestImages.png(TestImages.bitmap(30, 20, Color.RED))
        val before = source.buffer.size
        val options = extractImageOptions(source)
        options.outWidth shouldBe 30
        options.outHeight shouldBe 20
        options.inJustDecodeBounds shouldBe true
        source.buffer.size shouldBe before
    }

    @Test
    fun undecodableBytesGiveMinusOne() {
        val options = extractImageOptions(okio.Buffer().writeUtf8("not an image"))
        options.outWidth shouldBe -1
        options.outHeight shouldBe -1
    }

    @Test
    fun bitmapPixelGridReadsTheBitmap() {
        val bitmap = TestImages.bitmap(3, 2, Color.WHITE).apply { setPixel(2, 1, Color.BLACK) }
        val grid = BitmapPixelGrid(bitmap)
        grid.width shouldBe 3
        grid.height shouldBe 2
        grid[0, 0] shouldBe Color.WHITE
        grid[2, 1] shouldBe Color.BLACK
    }

    @Test
    fun displayMaxHeightIsLongerSide() {
        val metrics = android.content.res.Resources.getSystem().displayMetrics
        getDisplayMaxHeightInPx shouldBe maxOf(metrics.widthPixels, metrics.heightPixels)
    }
}
