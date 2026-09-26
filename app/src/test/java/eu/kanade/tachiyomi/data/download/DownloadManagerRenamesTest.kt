package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Renaming source, manga and chapter folders; a rename onto a non-empty directory fails. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerRenamesTest : DownloadManagerTestBase() {

    private fun dir(path: String): File = File(root, path).apply { mkdirs() }

    private fun blocker(path: String) = File(dir(path), "keep").writeText("x")

    @Test
    fun sourceFolderIsRenamed() {
        manager.renameSource(httpSource(name = "Gone", id = 9L), source)
        dir("Old")
        manager.renameSource(httpSource(name = "Old", id = 9L), source)
        File(root, "Source").isDirectory shouldBe true
        manager.renameSource(source, source)
        File(root, "Source").isDirectory shouldBe true
    }

    @Test
    fun sourceCaseChangeGoesThroughTmp() {
        dir("SOURCE")
        manager.renameSource(httpSource(name = "SOURCE", id = 9L), source)
        File(root, "Source").isDirectory shouldBe true
    }

    @Test
    fun sourceRenameFailuresKeep() {
        dir("SOURCE")
        blocker("Source_tmp")
        manager.renameSource(httpSource(name = "SOURCE", id = 9L), source)
        File(root, "SOURCE").isDirectory shouldBe true
        dir("Old")
        blocker("Source")
        manager.renameSource(httpSource(name = "Old", id = 9L), source)
        File(root, "Old").isDirectory shouldBe true
    }

    @Test
    fun mangaFolderIsRenamed() = runTest {
        manager.renameManga(manga, "New")
        dir("Source/Title")
        manager.renameManga(manga, "Title")
        manager.renameManga(manga, "New")
        File(root, "Source/New").isDirectory shouldBe true
        coVerify { cache.renameManga(manga, any(), "New") }
    }

    @Test
    fun mangaCaseChangeAndFailures() = runTest {
        dir("Source/Title")
        manager.renameManga(manga, "TITLE")
        File(root, "Source/TITLE").isDirectory shouldBe true
        val upper = manga.copy(ogTitle = "TITLE")
        blocker("Source/Title_tmp")
        manager.renameManga(upper, "Title")
        blocker("Source/Other")
        manager.renameManga(upper, "Other")
        File(root, "Source/TITLE").isDirectory shouldBe true
    }

    @Test
    fun chapterFolderIsRenamed() = runTest {
        dir("Source/Title/Ch 1")
        manager.renameChapter(source, manga, chapter(1L), chapter(id = 1L, name = "Ch 1"))
        manager.renameChapter(source, manga, chapter(2L), chapter(id = 2L, name = "Two"))
        manager.renameChapter(source, manga, chapter(1L), chapter(id = 1L, name = "One"))
        File(root, "Source/Title/One").isDirectory shouldBe true
        coVerify { cache.addChapter("One", any(), manga) }
    }

    @Test
    fun cbzChapterKeepsItsExtension() = runTest {
        dir("Source/Title")
        File(root, "Source/Title/Ch 1.cbz").writeText("zip")
        manager.renameChapter(source, manga, chapter(1L), chapter(id = 1L, name = "One"))
        File(root, "Source/Title/One.cbz").isFile shouldBe true
        File(root, "Source/Title/Ch 2").writeText("not an archive")
        manager.renameChapter(source, manga, chapter(2L), chapter(id = 2L, name = "Two"))
        File(root, "Source/Title/Two").isFile shouldBe true
    }

    @Test
    fun chapterRenameFailures() = runTest {
        dir("Source/Title/Ch 1")
        blocker("Source/Title/One")
        manager.renameChapter(source, manga, chapter(1L), chapter(id = 1L, name = "One"))
        File(root, "Source/Title/Ch 1").isDirectory shouldBe true
        every { provider.storageManager.getDownloadsDirectory() } returns null
        manager.renameChapter(source, manga, chapter(1L), chapter(id = 1L, name = "Two"))
    }

    @Test
    fun mangaDirByTitle() {
        manager.renameMangaDir("Title", "New", 5L)
        dir("Source")
        manager.renameMangaDir("Title", "New", 5L)
        dir("Source/Title")
        manager.renameMangaDir("Title", "New", 5L)
        File(root, "Source/New").isDirectory shouldBe true
    }
}
