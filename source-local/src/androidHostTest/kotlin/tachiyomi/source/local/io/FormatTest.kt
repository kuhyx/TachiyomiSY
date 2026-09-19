package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.source.local.uni

@RunWith(RobolectricTestRunner::class)
internal class FormatTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun everyFormatIsClassified() {
        val directory = folder.newFolder("ch1").uni()
        val archive = folder.newFile("ch2.cbz").uni()
        val epub = folder.newFile("ch3.EPUB").uni()
        Format.valueOf(directory).shouldBeInstanceOf<Format.Directory>().file shouldBe directory
        Format.valueOf(archive).shouldBeInstanceOf<Format.Archive>().file shouldBe archive
        Format.valueOf(epub).shouldBeInstanceOf<Format.Epub>().file shouldBe epub
    }

    @Test
    fun otherFilesAreUnknown() {
        shouldThrow<Format.UnknownFormatException> { Format.valueOf(folder.newFile("notes.txt").uni()) }
        val nameless = mockk<UniFile> {
            every { isDirectory } returns false
            every { name } returns null
        }
        shouldThrow<Format.UnknownFormatException> { Format.valueOf(nameless) }
    }

    @Test
    fun directoryIsADataClass() {
        val file = folder.newFolder("a").uni()
        val other = folder.newFolder("b").uni()
        val format = Format.Directory(file)
        val (component) = format
        component shouldBe file
        format shouldBe Format.Directory(file)
        format shouldNotBe Format.Directory(other)
        format.hashCode() shouldBe Format.Directory(file).hashCode()
        format.copy(file = other).file shouldBe other
        format.copy() shouldBe format
        format.toString() shouldStartWith "Directory(file="
    }

    @Test
    fun archiveIsADataClass() {
        val file = folder.newFile("a.cbz").uni()
        val other = folder.newFile("b.cbz").uni()
        val format = Format.Archive(file)
        val (component) = format
        component shouldBe file
        format shouldBe Format.Archive(file)
        format shouldNotBe Format.Archive(other)
        format.hashCode() shouldBe Format.Archive(file).hashCode()
        format.copy(file = other).file shouldBe other
        format.copy() shouldBe format
        format.toString() shouldStartWith "Archive(file="
    }

    @Test
    fun epubIsADataClass() {
        val file = folder.newFile("a.epub").uni()
        val other = folder.newFile("b.epub").uni()
        val format = Format.Epub(file)
        val (component) = format
        component shouldBe file
        format shouldBe Format.Epub(file)
        format shouldNotBe Format.Epub(other)
        format.hashCode() shouldBe Format.Epub(file).hashCode()
        format.copy(file = other).file shouldBe other
        format.copy() shouldBe format
        format.toString() shouldStartWith "Epub(file="
    }
}
