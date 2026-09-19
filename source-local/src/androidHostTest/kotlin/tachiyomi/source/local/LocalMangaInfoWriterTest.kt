package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class LocalMangaInfoWriterTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)
    private val writer by lazy { LocalMangaInfoWriter(context, fileSystemOver(folder.root), files) }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun writtenInfo(dir: File): ComicInfo = files.parse(File(dir, COMIC_INFO_FILE).inputStream())

    private fun editedManga(): SManga = sampleManga("m", "Title").apply {
        author = "A"
        artist = "R"
        description = "D"
        genre = "G"
        status = SManga.COMPLETED
    }

    @Test
    fun createsTheComicInfoFromScratch() {
        val dir = folder.newFolder("m")
        writer.write(editedManga())
        val info = writtenInfo(dir)
        info.series?.value shouldBe "Title"
        info.writer?.value shouldBe "A"
        info.penciller?.value shouldBe "R"
        info.summary?.value shouldBe "D"
        info.genre?.value shouldBe "G"
        info.publishingStatus?.value shouldBe "Completed"
        info.title.shouldBeNull()
    }

    @Test
    fun mergesEveryEditableField() {
        val dir = folder.newFolder("m")
        val existing = emptyComicInfo().copy(title = ComicInfo.Title("Keep"), web = ComicInfo.Web("https://x"))
        File(dir, COMIC_INFO_FILE).writeBytes(comicInfoXml(existing))
        writer.write(editedManga())
        val info = writtenInfo(dir)
        info.title?.value shouldBe "Keep"
        info.web?.value shouldBe "https://x"
        info.series?.value shouldBe "Title"
        info.writer?.value shouldBe "A"
        info.penciller?.value shouldBe "R"
        info.summary?.value shouldBe "D"
        info.genre?.value shouldBe "G"
        info.publishingStatus?.value shouldBe "Completed"
    }

    @Test
    fun blankEditsClearOldFields() {
        val dir = folder.newFolder("m")
        val existing = emptyComicInfo().copy(
            writer = ComicInfo.Writer("Old"),
            penciller = ComicInfo.Penciller("Old"),
            summary = ComicInfo.Summary("Old"),
            genre = ComicInfo.Genre("Old"),
        )
        File(dir, COMIC_INFO_FILE).writeBytes(comicInfoXml(existing))
        writer.write(sampleManga("m", "New"))
        val info = writtenInfo(dir)
        info.series?.value shouldBe "New"
        info.writer.shouldBeNull()
        info.penciller.shouldBeNull()
        info.summary.shouldBeNull()
        info.genre.shouldBeNull()
        info.publishingStatus?.value shouldBe "Unknown"
    }

    @Test
    fun readsTheArchivedComicInfo() {
        val dir = folder.newFolder("m")
        File(dir, LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        val existing = emptyComicInfo().copy(title = ComicInfo.Title("Keep"))
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to comicInfoXml(existing))))
        writer.write(sampleManga("m", "New"))
        val info = writtenInfo(dir)
        info.title?.value shouldBe "Keep"
        info.series?.value shouldBe "New"
    }

    @Test
    fun emptyArchiveStartsFromScratch() {
        val dir = folder.newFolder("m")
        File(dir, LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        stubArchiveReaders(fakeArchiveReader(emptyMap()))
        writer.write(sampleManga("m", "New"))
        writtenInfo(dir).series?.value shouldBe "New"
    }

    @Test
    fun encryptedArchiveStaysEncrypted() {
        File(folder.newFolder("m"), LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        val existing = emptyComicInfo().copy(title = ComicInfo.Title("Keep"))
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to comicInfoXml(existing)), encrypted = true))
        val spy = spyk(files)
        every { spy.copy(any(), any(), any()) } returns null
        LocalMangaInfoWriter(context, fileSystemOver(folder.root), spy).write(sampleManga("m", "New"))
        verify { spy.copy(any(), match { it.name == "m" }, true) }
    }

    @Test
    fun missingFolderWritesNothing() {
        writer.write(sampleManga("missing"))
        folder.root.walk().count() shouldBe 1
    }
}
