package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class DownloadCacheRemovalsTest : DownloadCacheTestBase() {

    private val manga = Manga.create().copy(id = 1L, source = 1L, ogTitle = "Title")
    private val elsewhere = manga.copy(source = 3L)
    private val untitled = manga.copy(ogTitle = "Other")

    private fun chapter(name: String) = Chapter.create().copy(name = name, url = "/c")

    private fun seeded(): DownloadCache = newCache().apply {
        seed(root, 1L to mapOf("Title" to setOf("Ch 1", "Ch 2", "Ch 3")), 2L to mapOf("B" to setOf("x")))
    }

    private fun DownloadCache.chapters(title: String = "Title") =
        rootDownloadsDir.sourceDirs.getValue(1L).mangaDirs[title]?.chapterDirs

    @Test
    fun removeChapterDropsItsNames() = runTest {
        val cache = seeded()
        cache.removeChapter(chapter("Ch 1"), manga)
        cache.removeChapter(chapter("Ch 9"), manga)
        cache.removeChapter(chapter("Ch 2"), elsewhere)
        cache.removeChapter(chapter("Ch 2"), untitled)
        cache.chapters() shouldBe setOf("Ch 2", "Ch 3")
    }

    @Test
    fun removeChaptersDropsEach() = runTest {
        val cache = seeded()
        cache.removeChapters(listOf(chapter("Ch 1"), chapter("Ch 9")), manga)
        cache.removeChapters(listOf(chapter("Ch 2")), elsewhere)
        cache.removeChapters(listOf(chapter("Ch 2")), untitled)
        cache.chapters() shouldBe setOf("Ch 2", "Ch 3")
    }

    @Test
    fun removeFoldersDropsRawNames() = runTest {
        val cache = seeded()
        cache.removeFolders(listOf("Ch 3", "missing"), manga)
        cache.removeFolders(listOf("Ch 2"), elsewhere)
        cache.removeFolders(listOf("Ch 2"), untitled)
        cache.chapters() shouldBe setOf("Ch 1", "Ch 2")
    }

    @Test
    fun removeMangaAndSource() = runTest {
        val cache = seeded()
        cache.removeManga(untitled)
        cache.removeManga(elsewhere)
        cache.removeManga(manga)
        cache.chapters() shouldBe null
        cache.removeSource(beta)
        cache.rootDownloadsDir.sourceDirs.keys shouldBe setOf(1L)
    }

    @Test
    fun renameMovesTheChapters() = runTest {
        val cache = seeded()
        cache.renameManga(manga = manga, mangaUniFile = uni(root), newTitle = "Renamed")
        cache.chapters("Renamed") shouldBe setOf("Ch 1", "Ch 2", "Ch 3")
        cache.chapters() shouldBe null
    }

    @Test
    fun renameIntoAnExistingManga() = runTest {
        val cache = newCache().apply {
            seed(root, 1L to mapOf("Title" to setOf("Ch 1"), "Renamed" to setOf("Ch 2"), "Empty" to emptySet()))
        }
        cache.renameManga(manga = manga, mangaUniFile = uni(root), newTitle = "Renamed")
        cache.chapters("Renamed") shouldBe setOf("Ch 1", "Ch 2")
        cache.renameManga(manga = manga.copy(ogTitle = "Empty"), mangaUniFile = uni(root), newTitle = "Renamed")
        cache.chapters("Renamed") shouldBe setOf("Ch 1", "Ch 2")
    }

    @Test
    fun renameOfUnknownManga() = runTest {
        val cache = seeded()
        cache.renameManga(manga = untitled, mangaUniFile = uni(root), newTitle = "Fresh")
        cache.chapters("Fresh") shouldBe emptySet()
        cache.renameManga(manga = elsewhere, mangaUniFile = uni(root), newTitle = "Fresh")
        cache.rootDownloadsDir.sourceDirs.keys shouldBe setOf(1L, 2L)
    }
}
