package tachiyomi.core.common.util.system

import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import logcat.LogPriority
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Every way writing the splits can fail: the partial output is removed and the original kept. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ShadowImageDecoder::class, ShadowImageDecoderCompanion::class],
)
internal class TallImageSplitFailureTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val logger = RecordingLogcatLogger
    private lateinit var page: File

    @Before
    fun setUp() {
        logger.start()
        page = File(folder.root, "page.png").apply {
            writeBytes(TestImages.png(TestImages.bitmap(100, 2000, Color.GRAY)).readByteArray())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun split(tmpDir: UniFile = TestImages.uniFile(folder.root)): Boolean =
        TallImageSplitting.splitTallImage(tmpDir, TestImages.uniFile(page), "f")

    private fun lastError(): String {
        val entry = logger.entries.last()
        entry.priority shouldBe LogPriority.ERROR
        return entry.message
    }

    // A directory whose split files behave as onOpen says when opened for writing.
    private fun brokenDir(onOpen: () -> OutputStream): UniFile {
        val existing = mockk<UniFile>(relaxed = true)
        val created = mockk<UniFile> { every { openOutputStream() } answers { onOpen() } }
        return mockk {
            every { findFile(any()) } returns existing
            every { createFile(any()) } returns created
        }
    }

    @Test
    fun missingDecoderReturnsFalse() {
        mockkStatic(BitmapRegionDecoder::class)
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } returns null
        split() shouldBe false
        page.exists() shouldBe true
        logger.messages().last() shouldBe "Failed to create new instance of BitmapRegionDecoder"
    }

    @Test
    fun undecodableRegionIsAnIoError() {
        mockkStatic(BitmapRegionDecoder::class)
        val decoder = mockk<BitmapRegionDecoder>(relaxed = true)
        every { decoder.decodeRegion(any(), any()) } returns null
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } returns decoder
        split() shouldBe false
        lastError() shouldContain "Cannot decode Rect(0, 0 - 100, 666)"
        File(folder.root, "f__001.jpg").exists() shouldBe false
        page.exists() shouldBe true
        verify { decoder.recycle() }
    }

    @Test
    fun uncreatableSplitIsAnIoError() {
        val dir = mockk<UniFile> {
            every { findFile(any()) } returns null
            every { createFile(any()) } returns null
        }
        split(dir) shouldBe false
        lastError() shouldContain "Cannot create f__001.jpg"
        page.exists() shouldBe true
    }

    @Test
    fun ioFailureWhileWritingIsLogged() {
        split(brokenDir { throw IOException("disk full") }) shouldBe false
        lastError() shouldContain "disk full"
    }

    @Test
    fun illegalArgumentIsLogged() {
        split(brokenDir { throw IllegalArgumentException("bad rect") }) shouldBe false
        lastError() shouldContain "bad rect"
    }

    @Test
    fun illegalStateIsLogged() {
        split(brokenDir { throw IllegalStateException("recycled") }) shouldBe false
        lastError() shouldContain "recycled"
        page.exists() shouldBe true
    }
}
