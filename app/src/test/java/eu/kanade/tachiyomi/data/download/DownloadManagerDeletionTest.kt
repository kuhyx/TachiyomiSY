package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import java.io.File

/** Deleting downloaded chapters and manga, and what the queue does meanwhile. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerDeletionTest : DownloadManagerTestBase() {

    private fun dir(path: String): File = File(root, path).apply { mkdirs() }

    @Test
    fun lastChapterDropsTheTree() {
        dir("Source/Title/Ch 1")
        manager.deleteChapters(listOf(chapter(1L)), manga, source)
        waitUntil { !File(root, "Source").exists() }
        coVerify(timeout = 5_000) { cache.removeSource(source) }
    }

    @Test
    fun otherChaptersKeepTheManga() {
        dir("Source/Title/Ch 1")
        dir("Source/Title/Ch 2")
        manager.deleteChapters(listOf(chapter(1L)), manga, source)
        waitUntil { !File(root, "Source/Title/Ch 1").exists() }
        coVerify(timeout = 5_000) { cache.removeChapters(listOf(chapter(1L)), manga) }
        File(root, "Source/Title/Ch 2").isDirectory shouldBe true
    }

    @Test
    fun bookmarkedChaptersAreKept() {
        dir("Source/Title/Ch 1")
        manager.deleteChapters(listOf(chapter(1L).copy(bookmark = true)), manga, source)
        Thread.sleep(300)
        File(root, "Source/Title/Ch 1").isDirectory shouldBe true
    }

    @Test
    fun deletingAMangaKeepsOtherManga() {
        dir("Source/Title/Ch 1")
        dir("Source/Other")
        manager.downloader.addAllToQueue(listOf(download(1L)))
        manager.deleteManga(manga, source)
        waitUntil { !File(root, "Source/Title").exists() }
        manager.queueState.value shouldBe emptyList()
        File(root, "Source").isDirectory shouldBe true
    }

    @Test
    fun excludedCategoriesKeepUnread() = runTest {
        provider.downloadPreferences.removeExcludeCategories.set(setOf("7"))
        coEvery { getCategories.await(1L) } returns listOf(Category(id = 7L, name = "Keep", order = 0L, flags = 0L))
        val read = chapter(1L).copy(read = true)
        val marked = chapter(3L).copy(bookmark = true)
        manager.getChaptersToDelete(listOf(read, chapter(2L), marked), manga) shouldBe listOf(chapter(2L))
        provider.downloadPreferences.removeBookmarkedChapters.set(true)
        manager.getChaptersToDelete(listOf(read, marked), manga) shouldBe listOf(marked)
    }

    @Test
    fun runningQueueRestarts() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        manager.downloader.addAllToQueue(listOf(download(1L), download(2L)))
        manager.downloader.start()
        manager.removeFromDownloadQueue(listOf(chapter(1L)))
        manager.isRunning shouldBe true
        manager.removeFromDownloadQueue(listOf(chapter(2L)))
        manager.isRunning shouldBe false
        manager.removeFromDownloadQueue(listOf(chapter(3L)))
    }

    @Test
    fun pendingDeletionsRun() = runTest {
        dir("Source/Title/Ch 1")
        every { sourceManager.get(404L) } returns null
        manager.enqueueChaptersToDelete(listOf(chapter(1L)), manga)
        manager.enqueueChaptersToDelete(listOf(chapter(4L)), manga.copy(id = 2L, source = 404L))
        manager.deletePendingChapters()
        waitUntil { !File(root, "Source/Title/Ch 1").exists() }
    }

    @Test
    fun queuedStatusIsResetOnRemoval() {
        manager.downloader.addAllToQueue(listOf(download(1L)))
        val queued = manager.queueState.value.single()
        manager.cancelQueuedDownloads(listOf(queued))
        queued.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun missingFoldersAreSkipped() {
        manager.deleteChapters(listOf(chapter(1L)), manga, source)
        manager.deleteManga(manga, source)
        coVerify(timeout = 5_000) { cache.removeManga(manga) }
        coVerify(exactly = 0) { cache.removeSource(any()) }
    }

    @Test
    fun filesInsteadOfFoldersAreKept() {
        dir("Source")
        File(root, "Source/Title").writeText("x")
        manager.deleteChapters(listOf(chapter(1L)), manga, source)
        coVerify(timeout = 5_000) { cache.removeChapters(listOf(chapter(1L)), manga) }
        File(root, "Source").deleteRecursively()
        File(root, "Source").writeText("x")
        manager.deleteManga(manga, source)
        coVerify(timeout = 5_000) { cache.removeManga(manga) }
        File(root, "Source").isFile shouldBe true
    }
}
