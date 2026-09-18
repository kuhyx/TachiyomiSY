package tachiyomi.core.common.util.system

import android.graphics.Color
import android.graphics.Rect
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import tachiyomi.core.common.util.system.BitmapMerging.rect
import tachiyomi.core.common.util.system.TestImages.pixelIsNear

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class BitmapMergingTest {
    private val left = TestImages.bitmap(4, 4, Color.RED)
    private val right = TestImages.bitmap(4, 4, Color.BLUE)

    @Test
    fun rectSpansTheWholeBitmap() {
        left.rect shouldBe Rect(0, 0, 4, 4)
    }

    @Test
    fun leftToRightPutsFirstPageLeft() {
        val merged = TestImages.decode(
            BitmapMerging.mergeBitmaps(imageBitmap = left, imageBitmap2 = right, isLTR = true, centerMargin = 0),
        )
        merged.width shouldBe 8
        merged.height shouldBe 4
        merged.pixelIsNear(1, 1, Color.RED) shouldBe true
        merged.pixelIsNear(6, 1, Color.BLUE) shouldBe true
    }

    @Test
    fun rightToLeftPutsFirstPageRight() {
        val merged = TestImages.decode(
            BitmapMerging.mergeBitmaps(imageBitmap = left, imageBitmap2 = right, isLTR = false, centerMargin = 0),
        )
        merged.pixelIsNear(1, 1, Color.BLUE) shouldBe true
        merged.pixelIsNear(6, 1, Color.RED) shouldBe true
    }

    @Test
    fun centerMarginTakesBackground() {
        val source = BitmapMerging.mergeBitmaps(
            imageBitmap = left,
            imageBitmap2 = right,
            isLTR = true,
            centerMargin = 4,
            background = Color.BLACK,
        )
        val merged = TestImages.decode(source)
        merged.width shouldBe 12
        merged.pixelIsNear(5, 2, Color.BLACK) shouldBe true
    }

    @Test
    fun defaultBackgroundIsWhite() {
        val merged = TestImages.decode(
            BitmapMerging.mergeBitmaps(imageBitmap = left, imageBitmap2 = right, isLTR = true, centerMargin = 4),
        )
        merged.pixelIsNear(6, 2, Color.WHITE) shouldBe true
    }

    @Test
    fun shorterPageIsCentered() {
        val tall = TestImages.bitmap(4, 8, Color.GREEN)
        val merged = TestImages.decode(
            BitmapMerging.mergeBitmaps(imageBitmap = left, imageBitmap2 = tall, isLTR = true, centerMargin = 0),
        )
        merged.height shouldBe 8
        merged.pixelIsNear(1, 3, Color.RED) shouldBe true
        merged.pixelIsNear(1, 0, Color.WHITE) shouldBe true
        merged.pixelIsNear(6, 0, Color.GREEN) shouldBe true
    }

    @Test
    fun progressIsReportedInThreeSteps() {
        val progress = mutableListOf<Int>()
        BitmapMerging.mergeBitmaps(
            imageBitmap = left,
            imageBitmap2 = right,
            isLTR = true,
            centerMargin = 0,
            progressCallback = { progress += it },
        )
        progress shouldContainExactly listOf(98, 99, 100)
    }
}
