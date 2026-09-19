package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.File

/** [LocalChapterLister.list]: which files become chapters and what fills them. */
@RunWith(RobolectricTestRunner::class)
internal class LocalChapterListerTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val coverManager = mockk<LocalCoverManager>()
    private val files = ComicInfoFiles(context, testXml)

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun lister(fileSystem: LocalSourceFileSystem = fileSystemOver(folder.root)): LocalChapterLister =
        LocalChapterLister(
            context = context,
            fileSystem = fileSystem,
            coverManager = coverManager,
            comicInfoFiles = files,
            formats = LocalChapterFormats(context, fileSystem),
        )

    // A manga whose cover is already known, so listing never touches the cover manager.
    private fun coveredManga(): SManga = sampleManga("Manga").apply { thumbnail_url = "set" }

    private fun list(manga: SManga = coveredManga(), fileSystem: LocalSourceFileSystem? = null): List<SChapter> {
        val lister = if (fileSystem == null) lister() else lister(fileSystem)
        var chapters: List<SChapter> = emptyList()
        runTest { chapters = lister.list(manga) }
        return chapters
    }

    private fun chapterFile(name: String, modified: Long = 86_400_000L): File = File(folder.root, "Manga/$name").apply {
        createNewFile()
        setLastModified(modified)
    }

    @Test
    fun listsSupportedFilesByName() {
        val mangaDir = folder.newFolder("Manga")
        File(mangaDir, "Chapter 2").mkdir()
        File(mangaDir, "Chapter 2").setLastModified(86_400_000L)
        chapterFile("Chapter 10.cbz")
        chapterFile("Chapter 3.EPUB", modified = 172_800_000L)
        File(mangaDir, ".hidden").mkdir()
        chapterFile("notes.txt")
        stubArchiveReaders(fakeArchiveReader(EpubFixture.entries(metadata = "<dc:creator>C</dc:creator>")))
        val chapters = list()
        chapters.map { it.name } shouldBe listOf("Chapter 10", "Chapter 3", "Chapter 2")
        chapters.map { it.url } shouldBe listOf("Manga/Chapter 10.cbz", "Manga/Chapter 3.EPUB", "Manga/Chapter 2")
        chapters.map { it.chapter_number } shouldBe listOf(10f, 3f, 2f)
        chapters.map { it.date_upload } shouldBe listOf(86_400_000L, 172_800_000L, 86_400_000L)
        chapters.map { it.scanlator } shouldBe listOf(null, "C", null)
        verify(exactly = 0) { coverManager.update(any(), any(), any()) }
    }

    @Test
    fun folderComicInfoFillsTheChapter() {
        val chapter = File(folder.newFolder("Manga"), "c1").apply { mkdir() }
        val info = emptyComicInfo().copy(
            title = ComicInfo.Title("Named"),
            number = ComicInfo.Number("7"),
            translator = ComicInfo.Translator("T"),
        )
        File(chapter, COMIC_INFO_FILE).writeBytes(comicInfoXml(info))
        val listed = list().single()
        listed.name shouldBe "Named"
        listed.chapter_number shouldBe 7f
        listed.scanlator shouldBe "T"
    }

    @Test
    fun archiveComicInfoFillsChapter() {
        folder.newFolder("Manga")
        chapterFile("c1.cbz")
        val info = emptyComicInfo().copy(title = ComicInfo.Title("Named"), number = ComicInfo.Number("7"))
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to comicInfoXml(info))))
        val listed = list().single()
        listed.name shouldBe "Named"
        listed.chapter_number shouldBe 7f
        listed.scanlator.shouldBeNull()
    }

    @Test
    fun epubMetadataFillsBoth() {
        folder.newFolder("Manga")
        chapterFile("c1.epub")
        stubArchiveReaders(fakeArchiveReader(EpubFixture.entries()))
        val manga = coveredManga()
        val listed = list(manga).single()
        listed.name shouldBe "Chapter One"
        listed.scanlator shouldBe "Pub P"
        listed.date_upload shouldBe EpubFixture.FULL_METADATA_DATE
        manga.author shouldBe "Author A"
        manga.description shouldBe "Desc D"
    }

    @Test
    fun emptyFolderGivesNoChapters() {
        folder.newFolder("Manga")
        list(sampleManga("Manga")) shouldBe emptyList()
        list(sampleManga("Manga").apply { thumbnail_url = "" }) shouldBe emptyList()
        verify(exactly = 0) { coverManager.update(any(), any(), any()) }
    }

    @Test
    fun namelessFolderIsListedAsEmpty() {
        val nameless = mockk<UniFile> {
            every { name } returns null
            every { isDirectory } returns true
            every { lastModified() } returns 5L
            every { findFile(COMIC_INFO_FILE) } returns null
        }
        val fileSystem = mockk<LocalSourceFileSystem> {
            every { getFilesInMangaDirectory("Manga") } returns listOf(nameless)
        }
        val listed = list(fileSystem = fileSystem).single()
        listed.name shouldBe ""
        listed.url shouldBe "Manga/null"
        listed.date_upload shouldBe 5L
        listed.chapter_number shouldBe -1f
    }
}
