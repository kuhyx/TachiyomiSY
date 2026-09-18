package tachiyomi.core.common.util.system

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.exifinterface.media.ExifInterface
import eu.kanade.tachiyomi.util.system.GLUtil
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okio.Buffer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream

/** The public facade delegates to the package-private objects; each member is called once. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class ImageUtilTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        ImageUtil.hardwareBitmapThreshold = GLUtil.SAFE_TEXTURE_LIMIT
    }

    @Test
    fun thresholdIsSharedWithHardware() {
        ImageUtil.hardwareBitmapThreshold shouldBe GLUtil.SAFE_TEXTURE_LIMIT
        ImageUtil.hardwareBitmapThreshold = 512
        HardwareBitmaps.hardwareBitmapThreshold shouldBe 512
        ImageUtil.HARDWARE_BITMAP_UNSUPPORTED shouldBe false
    }

    @Test
    fun hardwareChecksUseTheThreshold() {
        ImageUtil.hardwareBitmapThreshold = 64
        ImageUtil.canUseHardwareBitmap(TestImages.bitmap(64, 10)) shouldBe true
        ImageUtil.canUseHardwareBitmap(TestImages.bitmap(65, 10)) shouldBe false
        ImageUtil.canUseHardwareBitmap(TestImages.png(TestImages.bitmap(10, 64))) shouldBe true
        ImageUtil.canUseHardwareBitmap(TestImages.png(TestImages.bitmap(10, 65))) shouldBe false
    }

    @Test
    fun typeDetectionIsForwarded() {
        ImageUtil.isImage("a.png") shouldBe true
        ImageUtil.isImage("a") { ByteArrayInputStream(ImageHeaders.gif) } shouldBe true
        ImageUtil.findImageType { ByteArrayInputStream(ImageHeaders.jpeg) } shouldBe ImageUtil.ImageType.JPEG
        ImageUtil.findImageType(ByteArrayInputStream(ImageHeaders.webp)) shouldBe ImageUtil.ImageType.WEBP
        ImageUtil.findImageType(ByteArrayInputStream(ByteArray(0))).shouldBeNull()
        ImageUtil.getExtensionFromMimeType("image/avif") { ByteArrayInputStream(ByteArray(0)) } shouldBe "avif"
        ImageUtil.isAnimatedAndSupported(Buffer().write(ImageHeaders.gif)) shouldBe true
        ImageUtil.isAnimatedAndSupported(Buffer().write(ImageHeaders.png)) shouldBe false
    }

    @Test
    fun backgroundIsForwarded() {
        val drawable = ImageUtil.chooseBackground(context, TestImages.png(TestImages.bitmap(200, 200)))
        drawable.shouldBeInstanceOf<ColorDrawable>().color shouldBe Color.WHITE
    }

    @Test
    fun exifPaddingIsForwarded() {
        val file = folder.newFile("p.jpg").apply {
            writeBytes(TestImages.jpeg(TestImages.bitmap(4, 4)).readByteArray())
        }
        ImageUtil.addPaddingToImageExif(file)
        ExifInterface(file.absolutePath).getAttribute(ExifInterface.TAG_USER_COMMENT) shouldNotBe null
    }

    @Test
    fun sidesAndSplitDataAreValues() {
        ImageUtil.Side.entries shouldBe listOf(ImageUtil.Side.RIGHT, ImageUtil.Side.LEFT)
        val split = ImageUtil.SplitData(index = 1, topOffset = 10, splitHeight = 20, splitWidth = 30)
        split.bottomOffset shouldBe 30
        split shouldBe ImageUtil.SplitData(index = 1, topOffset = 10, splitHeight = 20, splitWidth = 30)
        split.hashCode() shouldBe split.copy().hashCode()
        split.toString() shouldBe "SplitData(index=1, topOffset=10, splitHeight=20, splitWidth=30)"
        split.copy(splitHeight = 5).bottomOffset shouldBe 15
        split.component1() shouldBe 1
        split.component2() shouldBe 10
        split.component3() shouldBe 20
        split.component4() shouldBe 30
        split shouldNotBe split.copy(index = 2)
    }
}
