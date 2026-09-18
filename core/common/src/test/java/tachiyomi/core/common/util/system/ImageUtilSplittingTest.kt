package tachiyomi.core.common.util.system

import android.content.Context
import android.graphics.Color
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The splitting half of the facade, one call per member. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class ImageUtilSplittingTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()

    // Each operation drains its source, so every call gets a fresh spread.
    private fun spread(): okio.BufferedSource =
        TestImages.halves(width = 8, height = 4, left = Color.RED, right = Color.BLUE)

    @Test
    fun spreadOperationsAreForwarded() {
        ImageUtil.isWideImage(spread()) shouldBe true
        TestImages.decode(ImageUtil.splitInHalf(spread(), ImageUtil.Side.LEFT, 1)).width shouldBe 5
        TestImages.decode(ImageUtil.rotateImage(spread(), 90f)).height shouldBe 8
        TestImages.decode(ImageUtil.splitAndMerge(spread(), ImageUtil.Side.RIGHT)).height shouldBe 8
        val wide = TestImages.halves(width = 200, height = 100, left = Color.RED, right = Color.BLUE)
        TestImages.decode(ImageUtil.addHorizontalCenterMargin(wide, 100, context)).width shouldBe 296
    }

    @Test
    fun tallSplitIsForwarded() {
        val page = File(folder.root, "tall.png").apply {
            writeBytes(TestImages.png(TestImages.bitmap(100, 2000)).readByteArray())
        }
        ImageUtil.splitTallImage(TestImages.uniFile(folder.root), TestImages.uniFile(page), "x") shouldBe true
        page.exists() shouldBe false
        File(folder.root, "x__003.jpg").isFile shouldBe true
    }

    @Test
    fun mergeIsForwardedBothWays() {
        val left = TestImages.bitmap(4, 4, Color.RED)
        val right = TestImages.bitmap(4, 4, Color.BLUE)
        val plain = ImageUtil.mergeBitmaps(imageBitmap = left, imageBitmap2 = right, isLTR = true, centerMargin = 2)
        TestImages.decode(plain).width shouldBe 10
        val progress = mutableListOf<Int>()
        val merged = ImageUtil.mergeBitmaps(
            imageBitmap = left,
            imageBitmap2 = right,
            isLTR = false,
            centerMargin = 0,
            background = Color.BLACK,
            progressCallback = { progress += it },
        )
        TestImages.decode(merged).width shouldBe 8
        progress shouldContainExactly listOf(98, 99, 100)
    }
}
