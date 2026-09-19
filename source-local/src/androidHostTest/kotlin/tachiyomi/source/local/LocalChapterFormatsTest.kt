package tachiyomi.source.local

import android.content.Context
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.source.local.io.Format
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class LocalChapterFormatsTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun formats(root: File?): LocalChapterFormats = LocalChapterFormats(context, fileSystemOver(root))

    private fun failureFor(root: File?, url: String): String? =
        shouldThrow<IllegalStateException> { formats(root).resolve(sampleChapter(url)) }.message

    @Test
    fun resolvesEveryChapterFormat() {
        val manga = folder.newFolder("m")
        val directory = File(manga, "c1").apply { mkdir() }
        val archive = File(manga, "c2.cbz").apply { createNewFile() }
        val epub = File(manga, "c3.epub").apply { createNewFile() }
        val formats = formats(folder.root)
        val directoryFormat = formats.resolve(sampleChapter("m/c1")).shouldBeInstanceOf<Format.Directory>()
        directoryFormat.file.filePath shouldBe directory.path
        val archiveFormat = formats.resolve(sampleChapter("m/c2.cbz")).shouldBeInstanceOf<Format.Archive>()
        archiveFormat.file.filePath shouldBe archive.path
        val epubFormat = formats.resolve(sampleChapter("m/c3.epub")).shouldBeInstanceOf<Format.Epub>()
        epubFormat.file.filePath shouldBe epub.path
    }

    @Test
    fun missingFilesAreReported() {
        folder.newFolder("m")
        val message = context.stringResource(MR.strings.chapter_not_found)
        failureFor(root = null, url = "m/c1") shouldBe message
        failureFor(root = folder.root, url = "x/c1") shouldBe message
        failureFor(root = folder.root, url = "m/c1") shouldBe message
    }

    @Test
    fun unsupportedFilesAreReported() {
        File(folder.newFolder("m"), "notes.txt").createNewFile()
        val error = shouldThrow<IllegalStateException> { formats(folder.root).resolve(sampleChapter("m/notes.txt")) }
        error.message shouldBe context.stringResource(MR.strings.local_invalid_format)
        error.cause.shouldBeInstanceOf<Format.UnknownFormatException>()
    }
}
