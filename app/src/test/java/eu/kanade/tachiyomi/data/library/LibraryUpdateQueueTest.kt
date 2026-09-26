package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import exh.md.utils.FollowStatus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track

/** Which entries a run picks: category, SY group, then the auto-update restrictions. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateQueueTest : LibraryUpdateTestBase() {

    private val one by lazy { libraryManga(manga(1L, source = 1L), categories = listOf(1L)) }
    private val two by lazy { libraryManga(manga(2L, source = 2L).copy(ogStatus = 2L), categories = listOf(1L, 2L)) }
    private val three by lazy { libraryManga(manga(3L, source = 1L), categories = listOf(3L)) }

    private suspend fun select(group: Int, extra: String? = null, category: Long = -1L): List<Long> {
        coEvery { getLibraryManga.await() } returns listOf(one, two, three)
        return job().selectMangaToUpdate(category, group, extra).map { it.id }
    }

    private fun track(manga: Long, followStatus: Long) = mockk<Track> {
        every { mangaId } returns manga
        every { trackerId } returns MDLIST_ID
        every { status } returns followStatus
    }

    @Test
    fun categoryPicksItsEntries() = runTest {
        select(LibraryGroup.BY_DEFAULT, category = 1L) shouldBe listOf(1L, 2L)
    }

    @Test
    fun wholeLibraryHonoursCategories() = runTest {
        select(LibraryGroup.BY_DEFAULT) shouldBe listOf(1L, 2L, 3L)
        libraryPreferences.updateCategoriesExclude.set(setOf("2"))
        select(LibraryGroup.BY_DEFAULT) shouldBe listOf(1L, 3L)
        libraryPreferences.updateCategories.set(setOf("1"))
        select(LibraryGroup.BY_SOURCE, extra = "0") shouldBe listOf(1L)
    }

    @Test
    fun ungroupedViewIsWholeLibrary() = runTest {
        libraryPreferences.groupLibraryUpdateType.set(GroupLibraryMode.ALL_BUT_UNGROUPED)
        select(LibraryGroup.UNGROUPED) shouldBe listOf(1L, 2L, 3L)
        select(LibraryGroup.BY_STATUS, extra = "2") shouldBe listOf(2L)
        libraryPreferences.groupLibraryUpdateType.set(GroupLibraryMode.ALL)
        libraryPreferences.updateCategoriesExclude.set(setOf("2"))
        select(LibraryGroup.UNGROUPED) shouldBe listOf(1L, 2L, 3L)
        select(UNKNOWN_GROUP) shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun groupBySourceIndex() = runTest {
        libraryPreferences.groupLibraryUpdateType.set(GroupLibraryMode.ALL)
        select(LibraryGroup.BY_SOURCE, extra = "0") shouldBe listOf(1L, 3L)
        select(LibraryGroup.BY_SOURCE, extra = "1") shouldBe listOf(2L)
        select(LibraryGroup.BY_SOURCE, extra = " ") shouldBe emptyList()
        select(LibraryGroup.BY_SOURCE, extra = "9") shouldBe emptyList()
        select(LibraryGroup.BY_SOURCE) shouldBe emptyList()
    }

    @Test
    fun groupByStatus() = runTest {
        libraryPreferences.groupLibraryUpdateType.set(GroupLibraryMode.ALL)
        select(LibraryGroup.BY_STATUS, extra = "0") shouldBe listOf(1L, 3L)
        select(LibraryGroup.BY_STATUS) shouldBe emptyList()
    }

    @Test
    fun groupByTrackStatus() = runTest {
        libraryPreferences.groupLibraryUpdateType.set(GroupLibraryMode.ALL)
        every { mdList.id } returns MDLIST_ID
        val tracks = listOf(track(1L, FollowStatus.READING.long), track(3L, FollowStatus.UNFOLLOWED.long))
        coEvery { getTracks.await() } returns tracks
        select(LibraryGroup.BY_TRACK_STATUS, extra = "1") shouldBe listOf(1L)
        select(LibraryGroup.BY_TRACK_STATUS, extra = "7") shouldBe listOf(2L, 3L)
        select(LibraryGroup.BY_TRACK_STATUS) shouldBe emptyList()
    }

    @Test
    fun restrictionsSkipWithReasons() {
        every { fetchInterval.getWindow(any()) } returns (0L to 100L)
        fun entry(id: Long, total: Long = 0L, read: Long = 0L, edit: Manga.() -> Manga = { this }) =
            libraryManga(manga(id, title = "T$id").edit(), total = total, read = read)
        val entries = listOf(
            entry(8L, total = 2L, read = 2L),
            entry(1L, total = 1L) { copy(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE) },
            entry(2L) { copy(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE, ogStatus = SManga.COMPLETED.toLong()) },
            entry(3L, total = 3L, read = 1L),
            entry(4L),
            entry(8L, total = 2L, read = 2L),
        )
        restrict(entries, all) shouldBe (listOf(4L, 8L) to listOf(1L, 2L, 3L))
        val later = listOf(entry(5L, total = 2L), entry(6L, total = 2L, read = 2L) { copy(nextUpdate = 200L) })
        val someRestrictions = setOf(MANGA_NON_READ, MANGA_OUTSIDE_RELEASE_PERIOD)
        restrict(later, someRestrictions) shouldBe (emptyList<Long>() to listOf(5L, 6L))
        restrict(entries + later, emptySet()).first shouldBe listOf(2L, 3L, 4L, 5L, 6L, 8L)
    }

    private fun restrict(entries: List<LibraryManga>, restrictions: Set<String>): Pair<List<Long>, List<Long>> {
        libraryPreferences.autoUpdateMangaRestrictions.set(restrictions)
        val skipped = mutableListOf<Pair<Manga, String?>>()
        val kept = job().applyUpdateRestrictions(entries, skipped)
        return kept.map { it.id } to skipped.map { it.first.id }
    }

    private companion object {
        const val MDLIST_ID = 60L
        const val UNKNOWN_GROUP = 99
        val all = setOf(MANGA_HAS_UNREAD, MANGA_NON_COMPLETED, MANGA_NON_READ, MANGA_OUTSIDE_RELEASE_PERIOD)
    }
}
