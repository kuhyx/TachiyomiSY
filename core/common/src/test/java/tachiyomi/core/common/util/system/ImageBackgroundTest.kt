package tachiyomi.core.common.util.system

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tachiyomi.core.common.util.system.TestImages.pixelIsNear

/**
 * 200px wide pages: narrower ones put the outermost scanned column past the bitmap edge
 * (`rightOffsetX + offsetX == width`), which a real Bitmap rejects.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class ImageBackgroundTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun darkTopHalf(): BufferedSource =
        TestImages.bands(width = 200, height = 200, split = 100, top = Color.BLACK, bottom = Color.WHITE)

    private fun contextWith(configuration: Configuration?): Context {
        val resources = mockk<Resources>()
        every { resources.configuration } returns configuration
        val context = mockk<Context>()
        every { context.resources } returns resources
        return context
    }

    private fun landscape(): Context =
        contextWith(Configuration().apply { orientation = Configuration.ORIENTATION_LANDSCAPE })

    @Test
    fun whitePageGetsWhiteDrawable() {
        val drawable = ImageBackground.chooseBackground(context, TestImages.png(TestImages.bitmap(200, 200)))
        drawable.shouldBeInstanceOf<ColorDrawable>().color shouldBe Color.WHITE
    }

    @Test
    fun darkTopHalfGetsGradient() {
        val page = darkTopHalf()
        ImageBackground.chooseBackground(context, page).shouldBeInstanceOf<GradientDrawable>()
    }

    @Test
    fun darkTopHalfIsBlackInLandscape() {
        val page = darkTopHalf()
        val drawable = ImageBackground.chooseBackground(landscape(), page)
        drawable.shouldBeInstanceOf<ColorDrawable>().color shouldBe Color.BLACK
    }

    @Test
    fun missingConfigurationIsPortrait() {
        ImageBackground.chooseBackground(contextWith(null), darkTopHalf()).shouldBeInstanceOf<GradientDrawable>()
    }

    @Test
    fun undecodableImageGetsWhite() {
        val empty = ImageBackground.chooseBackground(context, Buffer())
        empty.shouldBeInstanceOf<ColorDrawable>().color shouldBe Color.WHITE
        val garbage = ImageBackground.chooseBackground(context, Buffer().writeUtf8("not an image"))
        garbage.shouldBeInstanceOf<ColorDrawable>().color shouldBe Color.WHITE
    }

    @Test
    fun centerMarginTakesBackground() {
        val spread = TestImages.halves(width = 200, height = 100, left = Color.RED, right = Color.BLUE)
        val merged = TestImages.decode(ImageSplitting.addHorizontalCenterMargin(spread, viewHeight = 200, context))
        merged.width shouldBe 200 + 48
        merged.height shouldBe 100
        merged.pixelIsNear(10, 50, Color.RED) shouldBe true
        merged.pixelIsNear(120, 50, Color.WHITE) shouldBe true
        merged.pixelIsNear(240, 50, Color.BLUE) shouldBe true
    }

    @Test
    fun smallViewKeepsTheFullMargin() {
        val spread = TestImages.halves(width = 200, height = 100, left = Color.RED, right = Color.BLUE)
        val merged = TestImages.decode(ImageSplitting.addHorizontalCenterMargin(spread, viewHeight = 0, context))
        merged.width shouldBe 200 + 96
    }

    @Test
    fun undecodableSpreadFailsLoudly() {
        shouldThrow<NullPointerException> {
            ImageSplitting.addHorizontalCenterMargin(Buffer(), viewHeight = 100, context)
        }
    }
}
