package eu.kanade.tachiyomi.data.coil

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.BitmapImage
import coil3.decode.ImageSource
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.size.Size
import eu.kanade.domain.installFakeAndroidKeyStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File
import java.io.InputStream

internal fun memorySource(text: String): ImageSource =
    ImageSource(source = Buffer().writeUtf8(text), fileSystem = FileSystem.SYSTEM)

@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class TachiyomiImageDecoderTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val options: Options = coilOptions(context = context)

    @Before
    fun setUp() {
        installFakeAndroidKeyStore()
        DecoderScript.reset()
        DecoderScript.bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        mockkObject(ImageUtil)
        every { ImageUtil.findImageType(any<InputStream>()) } returns ImageUtil.ImageType.JPEG
        startKoin { modules(module { single { context } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun plainImageIsDecoded() = runTest {
        val result = TachiyomiImageDecoder(memorySource("pixels"), options).decode()
        (result.image as BitmapImage).bitmap.width shouldBe 100
        result.isSampled shouldBe false
        DecoderScript.lastInput shouldBe "pixels"
    }

    @Test
    fun softwareConfigKeepsTheBitmap() = runTest {
        val software = ImageRequest.Builder(context).bitmapConfig(Bitmap.Config.ARGB_8888).build()
        val result = TachiyomiImageDecoder(memorySource("x"), options.copy(extras = software.extras)).decode()
        (result.image as BitmapImage).bitmap shouldBe DecoderScript.bitmap
    }

    @Test
    fun smallTargetsAreSampled() = runTest {
        val small = options.copy(size = Size(10, 10))
        TachiyomiImageDecoder(memorySource("pixels"), small).decode().isSampled shouldBe true
    }

    @Test
    fun coverNamedImageIsNotAnArchive() = runTest {
        TachiyomiImageDecoder(memorySource("cover.jpg is in the header"), options).decode()
        DecoderScript.lastInput shouldBe "cover.jpg is in the header"
    }

    @Test
    fun unopenedFileHasNoDecoder() = runTest {
        val file = File(context.cacheDir, "img").apply { writeText("pixels") }
        val source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM)
        shouldThrow<IllegalStateException> { TachiyomiImageDecoder(source, options).decode() }.message shouldBe
            "Failed to initialize decoder"
    }

    @Test
    fun badDimensionsFail() = runTest {
        DecoderScript.size = null
        shouldThrow<IllegalStateException> { TachiyomiImageDecoder(memorySource("x"), options).decode() }
        DecoderScript.size = 0 to 10
        shouldThrow<IllegalStateException> { TachiyomiImageDecoder(memorySource("x"), options).decode() }
        DecoderScript.size = 10 to 0
        shouldThrow<IllegalStateException> { TachiyomiImageDecoder(memorySource("x"), options).decode() }
    }

    @Test
    fun failedDecodeFails() = runTest {
        DecoderScript.bitmap = null
        val failure = shouldThrow<IllegalStateException> { TachiyomiImageDecoder(memorySource("x"), options).decode() }
        failure.message shouldBe "Failed to decode image"
    }
}
