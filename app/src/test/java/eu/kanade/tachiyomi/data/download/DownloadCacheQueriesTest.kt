package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class DownloadCacheQueriesTest : DownloadCacheTestBase() {

    private val manga = Manga.create().copy(id = 1L, source = 1L, ogTitle = "Title")

    private fun DownloadCache.isDownloaded(name: String, sourceId: Long = 1L, title: String = "Title") =
        isChapterDownloaded(
            chapterName = name,
            chapterScanlator = null,
            chapterUrl = "/c",
            mangaTitle = title,
            sourceId = sourceId,
            skipCache = false,
        )

    @Test
    fun lookupsFollowTheIndex() {
        val cache = newCache()
        cache.seed(root, 1L to mapOf("Title" to setOf("Ch 1")))
        cache.isDownloaded("Ch 1") shouldBe true
        cache.isDownloaded("Ch 2") shouldBe false
        cache.isDownloaded(name = "Ch 1", title = "Other") shouldBe false
        cache.isDownloaded(name = "Ch 1", sourceId = 3L) shouldBe false
    }

    @Test
    fun countsFollowTheIndex() {
        val cache = newCache()
        cache.seed(root, 1L to mapOf("Title" to setOf("Ch 1", "Ch 2")))
        cache.getDownloadCount(manga) shouldBe 2
        cache.getDownloadCount(manga.copy(ogTitle = "Other")) shouldBe 0
        cache.getDownloadCount(manga.copy(source = 3L)) shouldBe 0
    }

    @Test
    fun addingToKnownManga() = runTest {
        val cache = newCache()
        cache.seed(root, 1L to mapOf("Title" to setOf("Ch 1")))
        cache.addChapter(chapterDirName = "Ch 2", mangaUniFile = uni(root), manga = manga)
        cache.getDownloadCount(manga) shouldBe 2
    }

    @Test
    fun addingCreatesSourceAndManga() = runTest {
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val cache = newCache()
        cache.seed(root)
        cache.addChapter(chapterDirName = "Ch 1", mangaUniFile = uni(root.resolve("Alpha/Title")), manga = manga)
        cache.getDownloadCount(manga) shouldBe 1
    }

    @Test
    fun addingNeedsASourceAndItsDir() = runTest {
        every { sourceManager.get(3L) } returns null
        val cache = newCache()
        cache.seed(root)
        cache.addChapter(chapterDirName = "Ch 1", mangaUniFile = uni(root), manga = manga.copy(source = 3L))
        cache.addChapter(chapterDirName = "Ch 1", mangaUniFile = uni(root), manga = manga)
        cache.rootDownloadsDir.sourceDirs shouldBe emptyMap()
    }
}
