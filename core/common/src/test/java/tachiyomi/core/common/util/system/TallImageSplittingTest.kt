package tachiyomi.core.common.util.system

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import java.io.File

/** Robolectric's default display is 320x470, so the optimal part height is 940px. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class TallImageSplittingTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val apiLevel = Build.VERSION.SDK_INT
    private val logger = RecordingLogcatLogger

    @Before
    fun setUp() {
        logger.start()
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", apiLevel)
    }

    private fun tallPage(height: Int = 2000): File = File(folder.root, "page.png").apply {
        val page = TestImages.bands(
            width = 100,
            height = height,
            split = height / 2,
            top = Color.BLACK,
            bottom = Color.WHITE,
        )
        writeBytes(page.readByteArray())
    }

    @Test
    fun optimalHeightIsTwiceTheDisplay() {
        TallImageSplitting.optimalImageHeight shouldBe getDisplayMaxHeightInPx * 2
        TallImageSplitting.optimalImageHeight shouldBe 940
    }

    @Test
    fun splitDataPadsTheLastPart() {
        val options = BitmapFactory.Options().apply {
            outWidth = 100
            outHeight = 2000
        }
        val splits = with(TallImageSplitting) { options.splitData }
        splits.map { it.index } shouldContainExactly listOf(0, 1, 2)
        splits.map { it.topOffset } shouldContainExactly listOf(0, 666, 1332)
        splits.map { it.splitHeight } shouldContainExactly listOf(666, 666, 668)
        splits.map { it.splitWidth } shouldContainExactly listOf(100, 100, 100)
        splits.last().bottomOffset shouldBe 2000
        logger.messages().single() shouldContain "3 parts @ 666px height per part"
    }

    @Test
    fun singlePartKeepsTheWholeHeight() {
        val options = BitmapFactory.Options().apply {
            outWidth = 100
            outHeight = 940
        }
        with(TallImageSplitting) { options.splitData } shouldContainExactly listOf(
            ImageUtil.SplitData(index = 0, topOffset = 0, splitHeight = 940, splitWidth = 100),
        )
    }

    @Test
    fun tallNeedsRatioAndTwoParts() {
        TallImageSplitting.isTallImage(TestImages.png(TestImages.bitmap(100, 2000))) shouldBe true
        TallImageSplitting.isTallImage(TestImages.png(TestImages.bitmap(100, 400))) shouldBe false
        TallImageSplitting.isTallImage(TestImages.png(TestImages.bitmap(400, 1000))) shouldBe false
    }

    @Test
    fun splitNamesAreZeroPadded() {
        TallImageSplitting.splitImageName("ch_001", 0) shouldBe "ch_001__001.jpg"
        TallImageSplitting.splitImageName("p", 41) shouldBe "p__042.jpg"
    }

    @Test
    fun animatedOrShortAreLeftAlone() {
        val gif = File(folder.root, "anim.gif").apply { writeBytes(ImageHeaders.gif) }
        TallImageSplitting.splitTallImage(TestImages.uniFile(folder.root), TestImages.uniFile(gif), "a") shouldBe true
        gif.exists() shouldBe true
        val short = tallPage(height = 400)
        TallImageSplitting.splitTallImage(TestImages.uniFile(folder.root), TestImages.uniFile(short), "s") shouldBe true
        short.exists() shouldBe true
    }

    @Test
    fun tallImageIsSplitAndDeleted() {
        val page = tallPage()
        File(folder.root, "t__002.jpg").writeText("stale")
        TallImageSplitting.splitTallImage(TestImages.uniFile(folder.root), TestImages.uniFile(page), "t") shouldBe true
        page.exists() shouldBe false
        val parts = listOf("t__001.jpg", "t__002.jpg", "t__003.jpg").map { File(folder.root, it) }
        parts.all { it.isFile } shouldBe true
        val first = TestImages.decode(Buffer().write(parts[0].readBytes()))
        first.width shouldBe 100
        first.height shouldBe 666
        TestImages.decode(Buffer().write(parts[2].readBytes())).height shouldBe 668
        logger.messages().last() shouldContain "Split #3 with topOffset=1332 height=668 bottomOffset=2000"
    }

    @Test
    fun legacyFactoryIsUsedBeforeS() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.R)
        val bytes = TestImages.png(TestImages.bitmap(10, 10)).readByteArray()
        val decoder = requireNotNull(TallImageSplitting.getBitmapRegionDecoder(bytes.inputStream()))
        decoder.width shouldBe 10
        decoder.recycle()
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)
        requireNotNull(TallImageSplitting.getBitmapRegionDecoder(bytes.inputStream())).height shouldBe 10
    }

    @Test
    fun undecodableInputIsAnError() {
        val bytes = Buffer().writeUtf8("not an image").readByteArray()
        val result = runCatching { TallImageSplitting.getBitmapRegionDecoder(bytes.inputStream()) }
        (result.isFailure || result.getOrNull() == null) shouldBe true
    }
}
