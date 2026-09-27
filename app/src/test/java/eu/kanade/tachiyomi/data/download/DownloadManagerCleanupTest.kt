package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerCleanupTest : DownloadManagerTestBase() {

    private fun cleanup(
        manager: DownloadManager = this.manager,
        owner: Manga = manga,
        removeRead: Boolean = false,
        removeNonFavorite: Boolean = false,
    ): Int = runBlocking {
        manager.cleanupChapters(
            allChapters = listOf(chapter(1), chapter(2, read = true)),
            manga = owner,
            source = source,
            removeRead = removeRead,
            removeNonFavorite = removeNonFavorite,
        )
    }

    @Test
    fun nonFavoriteLosesEverything() {
        downloaded(chapter(1))
        downloaded(chapter(2))
        cleanup(owner = manga.copy(favorite = false), removeNonFavorite = true) shouldBe 3
        File(root, "Source/Title").exists() shouldBe false
        coVerify { cache.removeManga(any()) }
    }

    @Test
    fun strayDirsGo() {
        // The provider flags a folder as unmatched only when no chapter folder matches at all.
        File(root, "Source/Title/Stray").mkdirs()
        every { cache.getDownloadCount(manga) } returns 1
        cleanup(removeRead = true) shouldBe 1
        File(root, "Source/Title/Stray").exists() shouldBe false
        coVerify { cache.removeFolders(listOf("Stray"), any()) }
    }

    @Test
    fun readDirsGo() {
        downloaded(chapter(1))
        downloaded(chapter(2))
        every { cache.getDownloadCount(any()) } returns 1
        cleanup(owner = manga.copy(favorite = false), removeRead = true) shouldBe 1
        File(root, "Source/Title/Ch 1").exists() shouldBe true
        File(root, "Source/Title/Ch 2").exists() shouldBe false
    }

    @Test
    fun uncachedFolderIsDropped() {
        downloaded(chapter(1))
        every { cache.getDownloadCount(manga) } returns 0
        cleanup(removeNonFavorite = true) shouldBe 0
        File(root, "Source/Title").exists() shouldBe false
    }

    @Test
    fun emptyUncachedFolderIsLogged() {
        every { cache.getDownloadCount(manga) } returns 0
        cleanup() shouldBe 0
        File(root, "Source/Title").exists() shouldBe true
    }

    @Test
    fun noStorageCleansNothing() {
        val detached = DownloadManager(
            context = context,
            provider = DownloadProviderHarness(root = null, context = context).provider,
            cache = cache,
            getCategories = getCategories,
            sourceManager = sourceManager,
            downloadPreferences = provider.downloadPreferences,
        )
        try {
            every { cache.getDownloadCount(any()) } returns 0
            cleanup(manager = detached, owner = manga.copy(favorite = false), removeNonFavorite = true) shouldBe 0
        } finally {
            detached.downloader.scope.cancel()
        }
    }

    @Test
    fun pendingDeletionsRunLater() {
        val one = downloaded(chapter(1, read = true))
        val elsewhere = manga.copy(id = 9L, source = 9L)
        every { sourceManager.get(9L) } returns null
        runBlocking {
            manager.enqueueChaptersToDelete(listOf(chapter(1, read = true)), manga)
            manager.enqueueChaptersToDelete(listOf(chapter(3)), elsewhere)
        }
        manager.deletePendingChapters()
        waitUntil { !one.exists() }
    }
}
