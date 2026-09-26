package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

internal class LibraryItemSortingTest {
    private val none = lazy<Map<Long, Double?>> { emptyMap() }
    private val noTags = lazy<List<String>> { emptyList() }

    private fun LibrarySort.Type.sign(a: LibraryItem, b: LibraryItem, ascending: Boolean = true): Int =
        comparator(ascending, none, noTags).compare(a, b).coerceIn(-1, 1)

    private fun entry(id: Long, block: (LibraryItem) -> LibraryItem = { it }): LibraryItem = block(libItem(id))

    @Test
    fun alphabeticalIgnoresCase() {
        val apple = libItem(libEntry(libManga(1L, title = "apple")))
        val banana = libItem(libEntry(libManga(2L, title = "Banana")))
        (ALPHABETICALLY.compare(apple, banana) < 0) shouldBe true
        LibrarySort.Type.Alphabetical.sign(banana, apple) shouldBe 1
    }

    @Test
    fun fieldComparatorsFollowTheField() {
        val low = libItem(
            LibraryMangaFields(lastRead = 1L, update = 1L, total = 1L, upload = 1L, fetched = 1L, added = 1L).entry(1L),
        )
        val high = libItem(
            LibraryMangaFields(lastRead = 2L, update = 2L, total = 2L, upload = 2L, fetched = 2L, added = 2L).entry(2L),
        )
        val types = listOf(
            LibrarySort.Type.LastRead,
            LibrarySort.Type.LastUpdate,
            LibrarySort.Type.TotalChapters,
            LibrarySort.Type.LatestChapter,
            LibrarySort.Type.ChapterFetchDate,
            LibrarySort.Type.DateAdded,
        )
        types.map { it.sign(low, high) } shouldContainExactly List(types.size) { -1 }
    }

    @Test
    fun unreadFirstWhenAscending() {
        val zero = libItem(libEntry(libManga(1L), total = 0L))
        val one = libItem(libEntry(libManga(2L), total = 1L))
        val two = libItem(libEntry(libManga(3L), total = 2L))
        val type = LibrarySort.Type.UnreadCount
        type.sign(zero, zero) shouldBe 0
        type.sign(zero, one) shouldBe 1
        type.sign(one, zero) shouldBe -1
        type.sign(one, two) shouldBe -1
    }

    @Test
    fun unreadFirstWhenDescending() {
        val zero = libItem(libEntry(libManga(1L), total = 0L))
        val one = libItem(libEntry(libManga(2L), total = 1L))
        val type = LibrarySort.Type.UnreadCount
        type.sign(zero, one, ascending = false) shouldBe -1
        type.sign(one, zero, ascending = false) shouldBe 1
    }

    @Test
    fun trackerMeanScoresUnscoredLast() {
        val scores = lazy<Map<Long, Double?>> { mapOf(1L to 8.0, 2L to null) }
        val comparator = LibrarySort.Type.TrackerMean.comparator(true, scores, noTags)
        comparator.compare(entry(2L), entry(1L)) shouldBe -1
        comparator.compare(entry(3L), entry(1L)) shouldBe -1
    }

    @Test
    fun tagListOrdersByFirstTag() {
        val tags = lazy { listOf("b", "a") }
        val first = libItem(libEntry(libManga(1L).copy(ogGenre = listOf("b"))))
        val second = libItem(libEntry(libManga(2L).copy(ogGenre = listOf("a"))))
        val untagged = libItem(libEntry(libManga(3L)))
        val comparator = LibrarySort.Type.TagList.comparator(true, none, tags)
        comparator.compare(first, second) shouldBe -1
        comparator.compare(untagged, first) shouldBe -1
    }

    @Test
    fun randomHasNoComparator() {
        shouldThrow<IllegalStateException> { LibrarySort.Type.Random.comparator(true, none, noTags) }
    }

    @Test
    fun sortTagsKeepValidEntries() {
        val preferences = LibraryPreferences(FlowPreferenceStore())
        preferences.sortTagsForLibrary.set(setOf("2|b", "1|a", "x|c", "3"))
        preferences.sortTagList() shouldContainExactly listOf("a", "b")
    }

    @Test
    fun meanScoresPerEntry() {
        val tracker = mockk<BaseTracker> {
            every { id } returns 1L
            every { get10PointScore(any()) } returns 6.0
        }
        val manager = mockk<TrackerManager> { every { getAll(setOf(1L)) } returns listOf(tracker) }
        val scores = manager.meanScores(
            trackMap = mapOf(1L to emptyList(), 2L to listOf(track(2L, 1L)), 3L to listOf(track(3L, 9L))),
            loggedInTrackerIds = setOf(1L),
        )
        scores[1L] shouldBe null
        scores[2L] shouldBe 6.0
        scores[3L]!!.isNaN() shouldBe true
    }
}

/** The sortable counters of one library entry. */
private class LibraryMangaFields(
    val lastRead: Long,
    val update: Long,
    val total: Long,
    val upload: Long,
    val fetched: Long,
    val added: Long,
) {
    fun entry(id: Long): LibraryManga = libEntry(
        manga = libManga(id).copy(lastUpdate = update, dateAdded = added),
        total = total,
    ).copy(
        latestUpload = upload,
        chapterFetchedAt = fetched,
        lastRead = lastRead,
    )
}
