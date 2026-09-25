package eu.kanade.tachiyomi.data.coil

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.bitmapConfig
import eu.kanade.domain.installFakeAndroidKeyStore
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.common.archive.ArchiveReader
import mihon.core.common.archive.archiveReader
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
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

/** The protected-archive cover path, hardware bitmaps and the factory's applicability test. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class TachiyomiImageDecoderArchiveTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val options: Options = coilOptions(context = context)
    private val reader: ArchiveReader = mockk()

    @Before
    fun setUp() {
        installFakeAndroidKeyStore()
        DecoderScript.reset()
        DecoderScript.bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        mockkObject(ImageUtil)
        every { ImageUtil.findImageType(any<InputStream>()) } returns null
        mockkStatic("mihon.core.common.archive.ArchiveReaderKt")
        every { any<com.hippo.unifile.UniFile>().archiveReader(any()) } returns reader
        mockkObject(CbzCrypto)
        startKoin { modules(module { single { context } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    // A file-backed source, already opened the way the factory's applicability check leaves it.
    private fun openedArchive(): ImageSource {
        val file = File(context.cacheDir, "archive.cbz").apply { writeText("PK..cover.jpg..") }
        return ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM).apply { source() }
    }

    @Test
    fun archiveCoverIsDecoded() = runTest {
        every { with(CbzCrypto) { reader.getCoverStream() } } returns BufferedInputStream("inner".byteInputStream())
        TachiyomiImageDecoder(openedArchive(), options).decode()
        DecoderScript.lastInput shouldBe "inner"
    }

    @Test
    fun archiveWithoutCoverUsesTheFile() = runTest {
        every { with(CbzCrypto) { reader.getCoverStream() } } returns null
        TachiyomiImageDecoder(openedArchive(), options).decode()
        DecoderScript.lastInput shouldBe "PK..cover.jpg.."
    }

    @Test
    fun hardwareCopyReplacesTheBitmap() = runTest {
        val hardware = ImageRequest.Builder(context).bitmapConfig(Bitmap.Config.HARDWARE).build()
        val hwOptions = options.copy(extras = hardware.extras)
        hwOptions.bitmapConfig shouldBe Bitmap.Config.HARDWARE
        val copy = mockk<Bitmap>(relaxed = true)
        val decoded = mockk<Bitmap>(relaxed = true)
        every { decoded.copy(Bitmap.Config.HARDWARE, false) } returnsMany listOf(copy, null)
        DecoderScript.bitmap = decoded
        every { ImageUtil.canUseHardwareBitmap(any<Bitmap>()) } returnsMany listOf(true, true, false)
        (TachiyomiImageDecoder(memorySource("x"), hwOptions).decode().image as BitmapImage).bitmap shouldBe copy
        verify { decoded.recycle() }
        (TachiyomiImageDecoder(memorySource("x"), hwOptions).decode().image as BitmapImage).bitmap shouldBe decoded
        (TachiyomiImageDecoder(memorySource("x"), hwOptions).decode().image as BitmapImage).bitmap shouldBe decoded
    }

    @Test
    fun factoryPicksWhatItCanDecode() {
        val loader = mockk<ImageLoader>()
        fun create(text: String, custom: Boolean = false) = TachiyomiImageDecoder.Factory().create(
            result = SourceFetchResult(memorySource(text), mimeType = null, dataSource = DataSource.MEMORY),
            options = if (custom) {
                options.copy(extras = ImageRequest.Builder(context).customDecoder(true).build().extras)
            } else {
                options
            },
            imageLoader = loader,
        )
        create(text = "x", custom = true).shouldBeInstanceOf<TachiyomiImageDecoder>()
        create("plain").shouldBeNull()
        create("cover.jpg").shouldBeInstanceOf<TachiyomiImageDecoder>()
        every { ImageUtil.findImageType(any<InputStream>()) } returnsMany listOf(
            ImageUtil.ImageType.AVIF,
            ImageUtil.ImageType.JXL,
        )
        create("a").shouldBeInstanceOf<TachiyomiImageDecoder>()
        create("j").shouldBeInstanceOf<TachiyomiImageDecoder>()
    }

    @Test
    fun factoriesAreEqual() {
        TachiyomiImageDecoder.Factory() shouldBe TachiyomiImageDecoder.Factory()
        TachiyomiImageDecoder.Factory().hashCode() shouldBe TachiyomiImageDecoder.Factory().hashCode()
        TachiyomiImageDecoder.Factory().equals("other") shouldBe false
        TachiyomiImageDecoder.displayProfile = byteArrayOf(1)
        TachiyomiImageDecoder.displayProfile!!.size shouldBe 1
        TachiyomiImageDecoder.displayProfile = null
    }
}
