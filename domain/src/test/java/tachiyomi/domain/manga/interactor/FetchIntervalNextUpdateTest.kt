package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.model.MangaUpdate
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal class FetchIntervalNextUpdateTest {

    private val getChaptersByMangaId = mockk<GetChaptersByMangaId> {
        coEvery { await(any(), any()) } returns emptyList()
    }
    private val fetchInterval = FetchInterval(getChaptersByMangaId)
    private val dateTime = ZonedDateTime.parse("2020-01-10T00:00:00Z")
    private val window = 1_000L to 2_000L

    private fun manga(interval: Int, nextUpdate: Long = 0L, lastUpdate: Long = 0L): Manga =
        MangaFixtures.manga(id = 3L).copy(fetchInterval = interval, nextUpdate = nextUpdate, lastUpdate = lastUpdate)

    private fun millis(text: String): Long = ZonedDateTime.parse(text).toEpochSecond() * 1_000L

    private fun stubChaptersEvery(gapDays: Long) {
        // Twenty chapters uploaded every gapDays days make the computed interval gapDays (capped at 28).
        val chapters = (1..20).map { index ->
            val at = dateTime.minusDays(200L - index * gapDays).toEpochSecond() * 1_000L
            Chapter.create().copy(dateUpload = at, dateFetch = at)
        }
        coEvery { getChaptersByMangaId.await(3L, applyScanlatorFilter = true) } returns chapters
    }

    @Test
    fun windowSpansTheGracePeriod() {
        val expected = millis("2020-01-09T00:00:00Z") to millis("2020-01-11T00:00:00Z") - 1

        fetchInterval.getWindow(dateTime) shouldBe expected
    }

    @Test
    fun userIntervalSkipsChapters() = runTest {
        val update = fetchInterval.toMangaUpdate(manga(interval = -5, nextUpdate = 1_500L), dateTime, window)

        update shouldBe MangaUpdate(id = 3L, nextUpdate = 1_500L, fetchInterval = -5)
        coVerify(exactly = 0) { getChaptersByMangaId.await(any(), any()) }
    }

    @Test
    fun nextUpdateAtWindowEndIsKept() = runTest {
        val update = fetchInterval.toMangaUpdate(manga(interval = -5, nextUpdate = 2_001L), dateTime, window)

        update.nextUpdate shouldBe 2_001L
    }

    @Test
    fun computedIntervalUsesChapters() = runTest {
        val manga = manga(interval = 0, lastUpdate = millis("2020-01-09T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, 0L to 0L)

        update shouldBe MangaUpdate(id = 3L, nextUpdate = millis("2020-01-16T00:00:00Z"), fetchInterval = 7)
        coVerify(exactly = 1) { getChaptersByMangaId.await(3L, applyScanlatorFilter = true) }
    }

    @Test
    fun halfZeroWindowIsUsedAsIs() = runTest {
        stubChaptersEvery(2L)
        val manga = manga(interval = 0, nextUpdate = 5_000L, lastUpdate = millis("2020-01-09T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, 0L to 2_000L)

        update.nextUpdate shouldBe millis("2020-01-11T00:00:00Z")
        update.fetchInterval shouldBe 2
    }

    @Test
    fun aboveWindowIsRecomputed() = runTest {
        stubChaptersEvery(3L)
        val manga = manga(interval = 0, nextUpdate = 5_000L, lastUpdate = millis("2020-01-09T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, window)

        update.nextUpdate shouldBe millis("2020-01-12T00:00:00Z")
        update.fetchInterval shouldBe 3
    }

    @Test
    fun negativeIntervalFromToday() = runTest {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val expected = now.toLocalDate().atStartOfDay().plusDays(1).toEpochSecond(ZoneOffset.UTC) * 1_000L

        val update = fetchInterval.toMangaUpdate(manga(interval = -1), now, window)

        update.nextUpdate shouldBe expected
        update.fetchInterval shouldBe -1
    }

    @Test
    fun missedChecksWidenTheCycle() = runTest {
        stubChaptersEvery(1L)
        val manga = manga(interval = 0, lastUpdate = millis("2019-10-02T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, window)

        // 100 days at interval 1: the check interval doubles to 16 (6 full cycles), the next day after that.
        update.nextUpdate shouldBe millis("2019-10-09T00:00:00Z")
        update.fetchInterval shouldBe 1
    }

    @Test
    fun wideningStopsAtMaxInterval() = runTest {
        stubChaptersEvery(1L)
        val manga = manga(interval = 0, lastUpdate = millis("2017-04-15T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, window)

        // 1000 days: doubling reaches 32 and is clamped to 28; 35 full cycles of 28 plus one interval.
        update.nextUpdate shouldBe millis("2017-05-21T00:00:00Z")
    }

    @Test
    fun maxIntervalIsNotWidened() = runTest {
        stubChaptersEvery(40L)
        val manga = manga(interval = 0, lastUpdate = millis("2020-01-09T00:00:00Z"))

        val update = fetchInterval.toMangaUpdate(manga, dateTime, window)

        update.fetchInterval shouldBe FetchInterval.MAX_INTERVAL
        update.nextUpdate shouldBe millis("2020-02-06T00:00:00Z")
    }
}
