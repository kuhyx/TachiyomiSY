package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.spyk
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
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.source.local.image.LocalCoverManager
import java.io.File

/** The top-level, archived and chapter-copied `ComicInfo.xml` paths of [LocalMangaDetails.fetch]. */
@RunWith(RobolectricTestRunner::class)
internal class LocalMangaDetailsTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)

    @Before
    fun setUp() {
        RecordingLogger.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun details(comicInfoFiles: ComicInfoFiles = files): LocalMangaDetails {
        val fileSystem = fileSystemOver(folder.root)
        return LocalMangaDetails(
            context = context,
            fileSystem = fileSystem,
            coverManager = LocalCoverManager(context, fileSystem),
            json = testJson,
            comicInfoFiles = comicInfoFiles,
        )
    }

    private fun seriesXml(name: String): ByteArray =
        comicInfoXml(emptyComicInfo().copy(series = ComicInfo.Series(name)))

    private fun fetch(manga: SManga = sampleManga("m"), comicInfoFiles: ComicInfoFiles = files): SManga {
        runTest { details(comicInfoFiles).fetch(manga) shouldBe manga }
        return manga
    }

    @Test
    fun topLevelComicInfoWinsAndClears() {
        val dir = folder.newFolder("m")
        File(dir, "cover.jpg").writeBytes(PNG_HEADER)
        File(dir, COMIC_INFO_FILE).writeBytes(seriesXml("Series"))
        File(dir, ".noxml").createNewFile()
        File(dir, LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        val manga = fetch()
        manga.title shouldBe "Series"
        manga.thumbnail_url shouldEndWith "/m/cover.jpg"
        File(dir, ".noxml").exists() shouldBe false
    }

    @Test
    fun topLevelComicInfoWithoutMarker() {
        val dir = folder.newFolder("m")
        File(dir, COMIC_INFO_FILE).writeBytes(seriesXml("Series"))
        val manga = fetch()
        manga.title shouldBe "Series"
        manga.thumbnail_url.shouldBeNull()
    }

    @Test
    fun archivedComicInfoIsRead() {
        val dir = folder.newFolder("m")
        File(dir, LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        File(dir, ".noxml").createNewFile()
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to seriesXml("Archived"))))
        fetch().title shouldBe "Archived"
        File(dir, ".noxml").exists() shouldBe false
    }

    @Test
    fun emptyArchiveChangesNothing() {
        val dir = folder.newFolder("m")
        File(dir, LocalSource.COMIC_INFO_ARCHIVE).createNewFile()
        stubArchiveReaders(fakeArchiveReader(emptyMap()))
        fetch().title shouldBe "m"
    }

    @Test
    fun chapterComicInfoIsCopiedUp() {
        val dir = folder.newFolder("m")
        File(dir, "c1.cbz").createNewFile()
        File(dir, "notes.txt").createNewFile()
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to seriesXml("Copied"))))
        fetch().title shouldBe "Copied"
        File(dir, COMIC_INFO_FILE).readBytes() shouldBe seriesXml("Copied")
        File(dir, ".noxml").exists() shouldBe false
    }

    @Test
    fun chaptersWithoutInfoAreMarked() {
        val dir = folder.newFolder("m")
        File(dir, "c1.cbz").createNewFile()
        stubArchiveReaders(fakeArchiveReader(emptyMap()))
        fetch().title shouldBe "m"
        File(dir, ".noxml").isFile shouldBe true
        File(dir, COMIC_INFO_FILE).exists() shouldBe false
    }

    @Test
    fun encryptedChapterCopyIsRead() {
        val dir = folder.newFolder("m")
        File(dir, "c1.cbz").createNewFile()
        val copied = folder.newFile(LocalSource.COMIC_INFO_ARCHIVE).uni()
        val spy = spyk(files)
        every { spy.copyFromChapters(any(), any()) } returns copied
        stubArchiveReaders(fakeArchiveReader(mapOf(COMIC_INFO_FILE to seriesXml("Encrypted"))))
        fetch(comicInfoFiles = spy).title shouldBe "Encrypted"
    }

    @Test
    fun markerSkipsTheChapterScan() {
        val dir = folder.newFolder("m")
        File(dir, "c1.cbz").createNewFile()
        File(dir, ".noxml").createNewFile()
        fetch().title shouldBe "m"
        File(dir, COMIC_INFO_FILE).exists() shouldBe false
        RecordingLogger.messages shouldBe emptyList()
    }

    @Test
    fun missingFolderIsLogged() {
        val manga = fetch(sampleManga("missing", "Ghost"))
        manga.title shouldBe "Ghost"
        manga.thumbnail_url.shouldBeNull()
        RecordingLogger.messages.single() shouldContain "Error setting manga details from local metadata for Ghost"
    }
}
