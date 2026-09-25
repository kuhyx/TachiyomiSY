package eu.kanade.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import java.time.ZonedDateTime

internal class UpdateMangaTest {

    private val repository = mockk<MangaRepository>()
    private val fetchInterval = mockk<FetchInterval>()
    private val interactor = UpdateManga(repository, fetchInterval)
    private val manga = Manga.create().copy(id = 4)

    @Test
    fun delegatesPlainUpdates() = runTest {
        val update = MangaUpdate(id = 4, url = "/x")
        coEvery { repository.update(update) } returns true
        coEvery { repository.updateAll(listOf(update)) } returns false
        interactor.await(update) shouldBe true
        interactor.awaitAll(listOf(update)) shouldBe false
    }

    @Test
    fun fetchIntervalDefaultsWindow() = runTest {
        val now = ZonedDateTime.now()
        val window = 1L to 2L
        val update = MangaUpdate(id = 4, nextUpdate = 9)
        every { fetchInterval.getWindow(any()) } returns window
        coEvery { fetchInterval.toMangaUpdate(manga, any(), window) } returns update
        coEvery { repository.update(update) } returns true
        interactor.awaitUpdateFetchInterval(manga) shouldBe true
        interactor.awaitUpdateFetchInterval(manga, now) shouldBe true
        coEvery { fetchInterval.toMangaUpdate(manga, now, 5L to 6L) } returns update
        interactor.awaitUpdateFetchInterval(manga, now, 5L to 6L) shouldBe true
        coVerify(exactly = 2) { fetchInterval.getWindow(any()) }
    }

    @Test
    fun stampsTimestamps() = runTest {
        val updates = mutableListOf<MangaUpdate>()
        coEvery { repository.update(capture(updates)) } returns true
        val before = System.currentTimeMillis()
        interactor.awaitUpdateLastUpdate(4) shouldBe true
        interactor.awaitUpdateCoverLastModified(4) shouldBe true
        interactor.awaitUpdateFavorite(4, favorite = true) shouldBe true
        interactor.awaitUpdateFavorite(4, favorite = false) shouldBe true
        (updates[0].lastUpdate!! >= before) shouldBe true
        (updates[1].coverLastModified!! >= before) shouldBe true
        updates[2].favorite shouldBe true
        (updates[2].dateAdded!! >= before) shouldBe true
        updates[3].favorite shouldBe false
        updates[3].dateAdded shouldBe 0L
        updates.all { it.id == 4L } shouldBe true
    }
}
