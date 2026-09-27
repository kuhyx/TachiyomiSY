package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerDeletionTest : DownloadManagerTestBase() {

    @Before
    fun stubDisk() {
        mockkObject(DiskUtil)
        every { DiskUtil.getAvailableStorageSpace(any<UniFile>()) } returns (1L shl 40)
    }

    private fun sourceDir() = File(root, "Source")

    @Test
    fun lastChapterTakesItsParents() {
        downloaded(chapter(1))
        manager.deleteChapters(listOf(chapter(1)), manga, source)
        waitUntil { !sourceDir().exists() }
        coVerify(timeout = 5_000) { cache.removeSource(source) }
        coVerify { cache.removeChapters(listOf(chapter(1)), manga) }
        coVerify { cache.removeManga(manga) }
    }

    @Test
    fun otherChaptersKeepTheManga() {
        val one = downloaded(chapter(1))
        downloaded(chapter(2))
        manager.deleteChapters(listOf(chapter(1)), manga, source)
        waitUntil { !one.exists() }
        coVerify(timeout = 5_000) { cache.removeChapters(any(), manga) }
        coVerify(exactly = 0) { cache.removeManga(any()) }
    }

    @Test
    fun bookmarksSurviveUnlessAllowed() {
        val kept = downloaded(chapter(1, bookmark = true))
        manager.deleteChapters(listOf(chapter(1, bookmark = true)), manga, source)
        coVerify(timeout = 5_000) { getCategories.await(manga.id) }
        Thread.sleep(SETTLE_MS)
        kept.exists() shouldBe true
        provider.downloadPreferences.removeBookmarkedChapters.set(true)
        manager.deleteChapters(listOf(chapter(1, bookmark = true)), manga, source)
        waitUntil { !kept.exists() }
    }

    @Test
    fun excludedCategoryKeepsUnread() {
        val chapters = listOf(chapter(1), chapter(2, read = true))
        provider.downloadPreferences.removeExcludeCategories.set(setOf("3"))
        runBlocking { manager.getChaptersToDelete(chapters, manga) }.map { it.id } shouldBe listOf(1L, 2L)
        coEvery { getCategories.await(manga.id) } returns listOf(Category(id = 3, name = "c", order = 0, flags = 0))
        runBlocking { manager.getChaptersToDelete(chapters, manga) }.map { it.id } shouldBe listOf(1L)
    }

    @Test
    fun excludedDefaultKeepsUnread() {
        val chapters = listOf(chapter(1), chapter(2, read = true))
        provider.downloadPreferences.removeExcludeCategories.set(setOf("0"))
        coEvery { getCategories.await(manga.id) } returns emptyList()
        runBlocking { manager.getChaptersToDelete(chapters, manga) }.map { it.id } shouldBe listOf(1L)
    }

    @Test
    fun missingFoldersAreFine() {
        manager.deleteChapters(listOf(chapter(1)), manga, source)
        coVerify(timeout = 5_000) { cache.removeChapters(listOf(chapter(1)), manga) }
        manager.deleteManga(manga, source)
        coVerify(timeout = 5_000) { cache.removeManga(manga) }
        coVerify(exactly = 0) { cache.removeSource(any()) }
    }

    @Test
    fun deletingMangaDropsItsQueue() {
        manager.downloadChapters(manga, listOf(chapter(1)), autoStart = false)
        downloaded(chapter(1))
        downloaded(chapter(1), title = "Other")
        manager.deleteManga(manga, source)
        waitUntil { !File(root, "Source/Title").exists() }
        waitUntil { manager.queueState.value.isEmpty() }
        sourceDir().exists() shouldBe true
    }

    @Test
    fun removalRestartsTheQueue() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        manager.downloadChapters(manga, listOf(chapter(1), chapter(2)), autoStart = false)
        manager.downloader.start()
        manager.removeFromDownloadQueue(listOf(chapter(1)))
        manager.isRunning shouldBe true
        manager.removeFromDownloadQueue(listOf(chapter(2)))
        manager.isRunning shouldBe false
        manager.removeFromDownloadQueue(listOf(chapter(3)))
    }

    private companion object {
        const val SETTLE_MS = 200L
    }
}
