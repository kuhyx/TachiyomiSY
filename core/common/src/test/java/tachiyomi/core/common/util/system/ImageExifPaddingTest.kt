package tachiyomi.core.common.util.system

import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import logcat.LogPriority
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ImageExifPaddingTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val logger = RecordingLogcatLogger

    @Before
    fun setUp() {
        logger.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun jpegFile(): File = folder.newFile("page.jpg").apply {
        writeBytes(TestImages.jpeg(TestImages.bitmap(8, 8, Color.GRAY)).readByteArray())
    }

    @Test
    fun paddingIsWrittenAsAUserComment() {
        val file = jpegFile()
        val before = file.length()
        ImageExifPadding.addPaddingToImageExif(file)
        val comment = requireNotNull(ExifInterface(file.absolutePath).getAttribute(ExifInterface.TAG_USER_COMMENT))
        comment.length shouldBeGreaterThanOrEqual 16_384
        comment.all { it.isLetterOrDigit() } shouldBe true
        (file.length() - before).toInt() shouldBeGreaterThanOrEqual 16_384
        logger.entries.size shouldBe 0
    }

    @Test
    fun missingFileIsLoggedAsAnError() {
        ImageExifPadding.addPaddingToImageExif(File(folder.root, "missing.jpg"))
        val entry = logger.entries.single()
        entry.priority shouldBe LogPriority.ERROR
        entry.message shouldContain "missing.jpg"
    }

    @Test
    fun unsupportedFormatIsLogged() {
        mockkConstructor(ExifInterface::class)
        every { anyConstructed<ExifInterface>().setAttribute(any(), any()) } returns Unit
        every { anyConstructed<ExifInterface>().saveAttributes() } throws UnsupportedOperationException("strips")
        ImageExifPadding.addPaddingToImageExif(jpegFile())
        val entry = logger.entries.single()
        entry.priority shouldBe LogPriority.ERROR
        entry.message shouldContain "UnsupportedOperationException: strips"
    }
}
