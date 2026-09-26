package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.ui.base.customInfoModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.track.model.Track

internal class LibraryItemSortingTest {
    private val scores = lazy { mapOf(1L to 9.0, 2L to null) }
    private val tags = lazy { listOf("b", "a") }

    // id 1: "Alpha", read 5 of 5, genre b; id 2: "beta", 3 unread, genre a; id 3: "Gamma", 1 unread, no genre.
    private val items by lazy {
        listOf(
            libraryItem(
                manga(1, "Alpha").copy(lastUpdate = 3, dateAdded = 1, ogGenre = listOf("b")),
                readCount = 5,
                totalChapters = 5,
            ),
            libraryItem(manga(2, "beta").copy(lastUpdate = 1, dateAdded = 3, ogGenre = listOf("a")), totalChapters = 3),
            libraryItem(manga(3, "Gamma").copy(lastUpdate = 2, dateAdded = 2), totalChapters = 1),
        )
    }

    @BeforeEach
    fun setUp() {
        startKoin { modules(customInfoModule()) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun order(type: LibrarySort.Type, ascending: Boolean = true): List<Long> =
        items.sortedWith(type.comparator(ascending, scores, tags)).map { it.id }

    @Test
    fun simpleTypesCompareTheirField() {
        order(LibrarySort.Type.Alphabetical) shouldBe listOf(1L, 2L, 3L)
        order(LibrarySort.Type.LastUpdate) shouldBe listOf(2L, 3L, 1L)
        order(LibrarySort.Type.DateAdded) shouldBe listOf(1L, 3L, 2L)
        order(LibrarySort.Type.TotalChapters) shouldBe listOf(3L, 2L, 1L)
        order(LibrarySort.Type.LastRead).size shouldBe 3
        order(LibrarySort.Type.LatestChapter).size shouldBe 3
        order(LibrarySort.Type.ChapterFetchDate).size shouldBe 3
    }

    @Test
    fun unreadCountKeepsUnreadFirst() {
        order(LibrarySort.Type.UnreadCount, ascending = true) shouldBe listOf(3L, 2L, 1L)
        order(LibrarySort.Type.UnreadCount, ascending = false) shouldBe listOf(1L, 3L, 2L)
        val (read, unread) = items[0] to items[1]
        for (ascending in listOf(true, false)) {
            val comparator = LibrarySort.Type.UnreadCount.comparator(ascending, scores, tags)
            val sign = if (ascending) 1 else -1
            comparator.compare(read, unread) shouldBe sign
            comparator.compare(unread, read) shouldBe -sign
            comparator.compare(read, read) shouldBe 0
        }
    }

    @Test
    fun scoresAndTagsSort() {
        order(LibrarySort.Type.TrackerMean) shouldBe listOf(2L, 3L, 1L)
        order(LibrarySort.Type.TagList) shouldBe listOf(3L, 1L, 2L)
    }

    @Test
    fun randomHasNoComparator() {
        shouldThrow<IllegalStateException> { LibrarySort.Type.Random.comparator(true, scores, tags) }
    }

    @Test
    fun sortTagsAreOrdered() {
        val preferences = LibraryPreferences(MapPreferenceStore())
        preferences.sortTagsForLibrary.set(setOf("2|second", "1|first", "x|bad", "3"))
        preferences.sortTagList() shouldBe listOf("first", "second")
    }

    @Test
    fun meanScoresAverageLoggedInTrackers() {
        val tracker = mockk<BaseTracker> {
            every { id } returns 10L
            every { get10PointScore(any()) } answers { firstArg<Track>().score }
        }
        val manager = mockk<TrackerManager> { every { getAll(setOf(10L)) } returns listOf(tracker) }
        val trackMap = mapOf(
            1L to listOf(domainTrack(trackerId = 10L, score = 6.0), domainTrack(trackerId = 10L, score = 8.0)),
            2L to listOf(domainTrack(trackerId = 99L, score = 5.0)),
            3L to emptyList(),
        )
        val means = manager.meanScores(trackMap, setOf(10L))
        means[1L] shouldBe 7.0
        means[2L]!!.isNaN() shouldBe true
        means[3L] shouldBe null
    }
}
