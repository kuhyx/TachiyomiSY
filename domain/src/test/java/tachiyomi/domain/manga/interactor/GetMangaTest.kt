package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.repository.MangaRepository

internal class GetMangaTest {

    private val manga = MangaFixtures.manga(id = 4L).copy(url = "/m", source = 9L)
    private val repository = mockk<MangaRepository>()
    private val getManga = GetManga(repository)

    @Test
    fun awaitByIdReturnsManga() = runTest {
        coEvery { repository.getMangaById(4L) } returns manga

        getManga.await(4L) shouldBe manga
    }

    @Test
    fun awaitByIdSwallowsFailure() = runTest {
        coEvery { repository.getMangaById(4L) } throws IllegalStateException("store failed")

        getManga.await(4L) shouldBe null
    }

    @Test
    fun subscribeByIdDelegates() = runTest {
        coEvery { repository.getMangaByIdAsFlow(4L) } returns flowOf(manga)

        getManga.subscribe(4L).first() shouldBe manga
    }

    @Test
    fun subscribeByUrlDelegates() = runTest {
        every { repository.getMangaByUrlAndSourceIdAsFlow("/m", 9L) } returns flowOf(manga, null)

        getManga.subscribe("/m", 9L).first() shouldBe manga
    }

    @Test
    fun awaitByUrlDelegates() = runTest {
        coEvery { repository.getMangaByUrlAndSourceId("/m", 9L) } returns manga

        getManga.await("/m", 9L) shouldBe manga
    }

    @Test
    fun awaitIdReturnsTheId() = runTest {
        coEvery { repository.getMangaByUrlAndSourceId("/m", 9L) } returns manga

        getManga.awaitId("/m", 9L) shouldBe 4L
    }

    @Test
    fun awaitIdIsNullWhenUnknown() = runTest {
        coEvery { repository.getMangaByUrlAndSourceId("/m", 9L) } returns null

        getManga.awaitId("/m", 9L) shouldBe null
    }
}
