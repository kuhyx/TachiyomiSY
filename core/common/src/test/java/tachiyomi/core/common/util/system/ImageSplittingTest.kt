package tachiyomi.core.common.util.system

import android.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import tachiyomi.core.common.util.system.ImageUtil.Side
import tachiyomi.core.common.util.system.TestImages.pixelIsNear

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ImageSplittingTest {
    private val spread = TestImages.halves(width = 8, height = 4, left = Color.RED, right = Color.BLUE)

    @Test
    fun wideMeansWiderThanTall() {
        ImageSplitting.isWideImage(spread) shouldBe true
        ImageSplitting.isWideImage(TestImages.png(TestImages.bitmap(4, 4))) shouldBe false
        ImageSplitting.isWideImage(TestImages.png(TestImages.bitmap(4, 8))) shouldBe false
    }

    @Test
    fun leftHalfKeepsTheLeftPixels() {
        val half = TestImages.decode(ImageSplitting.splitInHalf(spread, Side.LEFT, sidePadding = 0))
        half.width shouldBe 4
        half.height shouldBe 4
        half.pixelIsNear(1, 1, Color.RED) shouldBe true
    }

    @Test
    fun rightHalfKeepsRightAndPadding() {
        val half = TestImages.decode(ImageSplitting.splitInHalf(spread, Side.RIGHT, sidePadding = 2))
        half.width shouldBe 6
        half.pixelIsNear(1, 1, Color.BLUE) shouldBe true
    }

    @Test
    fun rotationSwapsTheDimensions() {
        val rotated = TestImages.decode(ImageSplitting.rotateImage(spread, 90f))
        rotated.width shouldBe 4
        rotated.height shouldBe 8
        val unrotated = ImageSplitting.rotateBitMap(TestImages.bitmap(8, 4), 180f)
        unrotated.width shouldBe 8
        unrotated.height shouldBe 4
    }

    @Test
    fun splitAndMergeStacksRightOnTop() {
        val stacked = TestImages.decode(ImageSplitting.splitAndMerge(spread, Side.RIGHT))
        stacked.width shouldBe 4
        stacked.height shouldBe 8
        stacked.pixelIsNear(1, 1, Color.BLUE) shouldBe true
        stacked.pixelIsNear(1, 6, Color.RED) shouldBe true
    }

    @Test
    fun splitAndMergeStacksLeftOnTop() {
        val stacked = TestImages.decode(ImageSplitting.splitAndMerge(spread, Side.LEFT))
        stacked.pixelIsNear(1, 1, Color.RED) shouldBe true
        stacked.pixelIsNear(1, 6, Color.BLUE) shouldBe true
    }
}
