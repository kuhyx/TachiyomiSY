package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaFixtures
import java.time.ZonedDateTime

/** A last update that lies after [dateTime] makes the elapsed days negative, which is where floor division matters. */
internal class FetchIntervalFutureTest {

    private val getChaptersByMangaId = mockk<GetChaptersByMangaId> {
        coEvery { await(any(), any()) } returns emptyList()
    }
    private val fetchInterval = FetchInterval(getChaptersByMangaId)
    private val dateTime = ZonedDateTime.parse("2020-01-10T00:00:00Z")
    private val window = 1_000L to 2_000L

    private fun millis(text: String): Long = ZonedDateTime.parse(text).toEpochSecond() * 1_000L

    private fun manga(lastUpdate: String): Manga =
        MangaFixtures.manga(id = 3L).copy(fetchInterval = 0, nextUpdate = 0L, lastUpdate = millis(lastUpdate))

    @Test
    fun negativeCycleRoundsDown() = runTest {
        // -5 days at the default 7-day interval: floorDiv gives -1, so the next update is the last update itself.
        val update = fetchInterval.toMangaUpdate(manga("2020-01-15T00:00:00Z"), dateTime, window)

        update.fetchInterval shouldBe 7
        update.nextUpdate shouldBe millis("2020-01-15T00:00:00Z")
    }

    @Test
    fun exactNegativeCycleIsKept() = runTest {
        // -7 days at 7: the division is exact, floorDiv does not adjust.
        val update = fetchInterval.toMangaUpdate(manga("2020-01-17T00:00:00Z"), dateTime, window)

        update.fetchInterval shouldBe 7
        update.nextUpdate shouldBe millis("2020-01-17T00:00:00Z")
    }
}
