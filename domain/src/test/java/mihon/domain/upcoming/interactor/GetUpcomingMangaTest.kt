package mihon.domain.upcoming.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

internal class GetUpcomingMangaTest {

    private val repository = mockk<MangaRepository>()

    @Test
    fun asksForOngoingAndFinished() = runTest {
        val upcoming = listOf(Manga.create().copy(id = 1L))
        coEvery { repository.getUpcomingManga(setOf(1L, 4L)) } returns flowOf(upcoming)

        GetUpcomingManga(repository).subscribe().first() shouldBe upcoming
    }
}
