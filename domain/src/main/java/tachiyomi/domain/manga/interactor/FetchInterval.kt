package tachiyomi.domain.manga.interactor

import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/** Estimates how often a manga gets new chapters and when to next check it. */
public class FetchInterval(
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    /**
     * A [MangaUpdate] carrying [manga]'s next-check time and fetch interval as of [dateTime]; a
     * user-set interval is kept, any other is derived from chapter dates. A zero [window] means
     * today's window.
     */
    public suspend fun toMangaUpdate(
        manga: Manga,
        dateTime: ZonedDateTime,
        window: Pair<Long, Long>,
    ): MangaUpdate {
        val interval = manga.fetchInterval.takeIf { it < 0 } ?: calculateInterval(
            chapters = getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true),
            zone = dateTime.zone,
        )
        val currentWindow = if (window.first == 0L && window.second == 0L) {
            getWindow(ZonedDateTime.now())
        } else {
            window
        }
        val nextUpdate = calculateNextUpdate(manga, interval, dateTime, currentWindow)

        return MangaUpdate(id = manga.id, nextUpdate = nextUpdate, fetchInterval = interval)
    }

    /** Epoch-millis bounds of the fetch window around [dateTime]'s day: a day of grace on either side. */
    public fun getWindow(dateTime: ZonedDateTime): Pair<Long, Long> {
        val today = dateTime.toLocalDate().atStartOfDay(dateTime.zone)
        val lowerBound = today.minusDays(GRACE_PERIOD)
        val upperBound = today.plusDays(GRACE_PERIOD)
        return Pair(lowerBound.toEpochSecond() * MILLIS_PER_SECOND, upperBound.toEpochSecond() * MILLIS_PER_SECOND - 1)
    }

    internal fun calculateInterval(chapters: List<Chapter>, zone: ZoneId): Int {
        val chapterWindow = if (chapters.size <= SMALL_LIST) SMALL_WINDOW else LARGE_WINDOW

        val uploadDates = chapters.filter { it.dateUpload > 0L }.distinctDays(chapterWindow, zone) { it.dateUpload }
        val fetchDates = chapters.distinctDays(chapterWindow, zone) { it.dateFetch }

        val interval = when {
            // Enough upload date from source
            uploadDates.size >= MIN_DATES -> medianGapDays(uploadDates)
            // Enough fetch date from client
            fetchDates.size >= MIN_DATES -> medianGapDays(fetchDates)
            else -> DEFAULT_INTERVAL
        }

        return interval.coerceIn(1, MAX_INTERVAL)
    }

    // The newest [count] distinct days, newest first, on which [date] fell.
    private fun List<Chapter>.distinctDays(count: Int, zone: ZoneId, date: (Chapter) -> Long): List<LocalDateTime> {
        return asSequence()
            .sortedByDescending(date)
            .map { ZonedDateTime.ofInstant(Instant.ofEpochMilli(date(it)), zone).toLocalDate().atStartOfDay() }
            .distinct()
            .take(count)
            .toList()
    }

    // The median of the gaps between consecutive dates (newest first).
    private fun medianGapDays(dates: List<LocalDateTime>): Int {
        val ranges = dates.windowed(2).map { x -> x[1].until(x[0], ChronoUnit.DAYS) }.sorted()
        return ranges[(ranges.size - 1) / 2].toInt()
    }

    private fun calculateNextUpdate(
        manga: Manga,
        interval: Int,
        dateTime: ZonedDateTime,
        window: Pair<Long, Long>,
    ): Long {
        if (manga.nextUpdate in window.first.rangeTo(window.second + 1)) {
            return manga.nextUpdate
        }
        val latestDate = ZonedDateTime.ofInstant(
            if (manga.lastUpdate > 0) Instant.ofEpochMilli(manga.lastUpdate) else Instant.now(),
            dateTime.zone,
        )
            .toLocalDate()
            .atStartOfDay()
        val timeSinceLatest = ChronoUnit.DAYS.between(latestDate, dateTime).toInt()
        val cycle = timeSinceLatest.floorDiv(
            interval.absoluteValue.takeIf { interval < 0 }
                ?: increaseInterval(interval, timeSinceLatest, increaseWhenOver = MISSED_CHECKS),
        )
        return latestDate.plusDays((cycle + 1) * interval.absoluteValue.toLong())
            .toEpochSecond(dateTime.offset) * MILLIS_PER_SECOND
    }

    private fun increaseInterval(delta: Int, timeSinceLatest: Int, increaseWhenOver: Int): Int {
        if (delta >= MAX_INTERVAL) return MAX_INTERVAL

        // double delta again if missed more than 9 check in new delta
        val cycle = timeSinceLatest.floorDiv(delta) + 1
        return if (cycle > increaseWhenOver) {
            increaseInterval(delta * 2, timeSinceLatest, increaseWhenOver)
        } else {
            delta
        }
    }

    /** Tuning constants of the estimate. */
    public companion object {
        /** Longest interval, in days, the estimate is clamped to. */
        public const val MAX_INTERVAL: Int = 28

        private const val GRACE_PERIOD = 1L
        private const val MILLIS_PER_SECOND = 1000L
        private const val SMALL_LIST = 8
        private const val SMALL_WINDOW = 3
        private const val LARGE_WINDOW = 10
        private const val MIN_DATES = 3
        private const val DEFAULT_INTERVAL = 7
        private const val MISSED_CHECKS = 10
    }
}
