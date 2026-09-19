package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.File
import java.io.InputStream

/** The cover a [LocalChapterLister] takes from a chapter folder or EPUB when the manga has none. */
@RunWith(RobolectricTestRunner::class)
internal class LocalChapterListerCoverTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val covers = mutableListOf<Pair<Int, Boolean>>()
    private val coverManager = mockk<LocalCoverManager> {
        every { update(any(), any(), any()) } answers {
            covers += secondArg<InputStream>().readBytes().size to thirdArg<Boolean>()
            null
        }
    }

    @Before
    fun setUp() {
        RecordingLogger.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun listWith(fileSystem: LocalSourceFileSystem, manga: SManga = sampleManga("m")) {
        val lister = LocalChapterLister(
            context = context,
            fileSystem = fileSystem,
            coverManager = coverManager,
            comicInfoFiles = ComicInfoFiles(context, testXml),
            formats = LocalChapterFormats(context, fileSystem),
        )
        runTest { lister.list(manga).size shouldBe 1 }
    }

    private fun listReal(manga: SManga = sampleManga("m")) {
        listWith(fileSystemOver(folder.root), manga)
    }

    private fun chapterFolder(): File = File(folder.newFolder("m"), "c1").apply { mkdir() }

    @Test
    fun folderCoverIsTheFirstImage() {
        val chapter = chapterFolder()
        File(chapter, "b.png").writeBytes(PNG_HEADER)
        File(chapter, "a.bin").createNewFile()
        File(chapter, "0dir").mkdir()
        listReal()
        covers shouldBe listOf(PNG_HEADER.size to false)
    }

    @Test
    fun folderWithoutImagesHasNoCover() {
        File(chapterFolder(), "a.bin").createNewFile()
        listReal()
        covers shouldBe emptyList()
    }

    @Test
    fun blankThumbnailStillTakesACover() {
        File(chapterFolder(), "b.png").writeBytes(PNG_HEADER)
        listReal(sampleManga("m").apply { thumbnail_url = "" })
        covers shouldBe listOf(PNG_HEADER.size to false)
    }

    @Test
    fun knownThumbnailIsKept() {
        File(chapterFolder(), "b.png").writeBytes(PNG_HEADER)
        listReal(sampleManga("m").apply { thumbnail_url = "set" })
        covers shouldBe emptyList()
    }

    @Test
    fun epubCoverIsTheFirstPageImage() {
        File(folder.newFolder("m"), "c1.epub").createNewFile()
        stubArchiveReaders(fakeArchiveReader(EpubFixture.entries()))
        listReal()
        covers shouldBe listOf(PNG_HEADER.size to false)
    }

    @Test
    fun epubWithoutImagesKeepsNoCover() {
        File(folder.newFolder("m"), "c1.epub").createNewFile()
        stubArchiveReaders(fakeArchiveReader(EpubFixture.entries(page = "<html><body>text</body></html>")))
        listReal()
        covers shouldBe emptyList()
    }

    @Test
    fun epubImageWithoutStreamIsLogged() {
        File(folder.newFolder("m"), "c1.epub").createNewFile()
        stubArchiveReaders(fakeArchiveReader(EpubFixture.entries() - EpubFixture.IMAGE_PATH))
        listReal()
        covers shouldBe emptyList()
        RecordingLogger.messages.single() shouldContain "Error updating cover for m"
    }

    @Test
    fun unresolvableChapterIsLogged() {
        val chapter = mockk<UniFile> {
            every { name } returns "c1"
            every { isDirectory } returns true
            every { lastModified() } returns 0L
            every { findFile(any()) } returns null
        }
        val fileSystem = mockk<LocalSourceFileSystem> {
            every { getFilesInMangaDirectory("m") } returns listOf(chapter)
            every { getBaseDirectory() } returns null
        }
        listWith(fileSystem)
        covers shouldBe emptyList()
        RecordingLogger.messages.single() shouldContain "Error updating cover for m"
    }

    @Test
    fun unlistableFolderKeepsNoCover() {
        val chapter = mockk<UniFile> {
            every { name } returns "c1"
            every { isDirectory } returns true
            every { lastModified() } returns 0L
            every { findFile(any()) } returns null
            every { listFiles() } returns null
        }
        listWith(fileSystemOf(chapter))
        covers shouldBe emptyList()
    }

    @Test
    fun namelessFolderEntriesAreSorted() {
        val nameless = mockk<UniFile> {
            every { name } returns null
            every { isDirectory } returns false
        }
        val image = mockk<UniFile> {
            every { name } returns "z.png"
            every { isDirectory } returns false
            every { openInputStream() } answers { PNG_HEADER.inputStream() }
        }
        val chapter = mockk<UniFile> {
            every { name } returns "c1"
            every { isDirectory } returns true
            every { lastModified() } returns 0L
            every { findFile(any()) } returns null
            every { listFiles() } returns arrayOf(nameless, image, nameless)
        }
        listWith(fileSystemOf(chapter))
        covers shouldBe listOf(PNG_HEADER.size to false)
    }

    // A file system whose only manga "m" holds [chapter], resolvable through the base directory.
    private fun fileSystemOf(chapter: UniFile): LocalSourceFileSystem {
        val mangaDir = mockk<UniFile> { every { findFile("c1") } returns chapter }
        val base = mockk<UniFile> { every { findFile("m") } returns mangaDir }
        return mockk {
            every { getFilesInMangaDirectory("m") } returns listOf(chapter)
            every { getBaseDirectory() } returns base
        }
    }
}
