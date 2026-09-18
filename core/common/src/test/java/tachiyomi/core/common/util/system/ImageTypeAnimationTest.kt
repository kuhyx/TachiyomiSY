package tachiyomi.core.common.util.system

import android.os.Build
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.BufferedSource
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class ImageTypeAnimationTest {
    private val apiLevel = Build.VERSION.SDK_INT

    @After
    fun tearDown() {
        setSdk(apiLevel)
    }

    private fun setSdk(level: Int) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", level)
    }

    private fun animated(header: ByteArray): Boolean = ImageTypeDetection.isAnimatedAndSupported(Buffer().write(header))

    @Test
    fun gifIsAlwaysAnimated() {
        animated(ImageHeaders.gif) shouldBe true
    }

    @Test
    fun stillFormatsAreNotAnimated() {
        listOf(ImageHeaders.jpeg, ImageHeaders.png, ImageHeaders.avif, ImageHeaders.jxl).forEach {
            withClue(String(it, Charsets.ISO_8859_1)) { animated(it) shouldBe false }
        }
        animated("nope".toByteArray()) shouldBe false
        animated(ByteArray(0)) shouldBe false
    }

    @Test
    fun animatedWebpNeedsPie() {
        animated(ImageHeaders.animatedWebp) shouldBe true
        animated(ImageHeaders.stillWebpVp8x) shouldBe false
        animated(ImageHeaders.webp) shouldBe false
        setSdk(Build.VERSION_CODES.O_MR1)
        animated(ImageHeaders.animatedWebp) shouldBe false
    }

    @Test
    fun animatedHeifNeedsAndroidEleven() {
        animated(ImageHeaders.animatedHeif) shouldBe true
        animated(ImageHeaders.heif) shouldBe false
        setSdk(Build.VERSION_CODES.Q)
        animated(ImageHeaders.animatedHeif) shouldBe false
    }

    @Test
    fun sourceFailuresAreSwallowed() {
        val source = mockk<BufferedSource> { every { peek() } throws IOException("closed") }
        ImageTypeDetection.isAnimatedAndSupported(source) shouldBe false
    }

    @Test
    fun sourceIsNotConsumed() {
        val source = Buffer().write(ImageHeaders.gif)
        ImageTypeDetection.isAnimatedAndSupported(source) shouldBe true
        source.size shouldBe ImageHeaders.gif.size.toLong()
    }
}
