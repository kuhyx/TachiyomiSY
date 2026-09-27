package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.track.TrackerManager
import exh.md.utils.FollowStatus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.track.model.Track

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateQueueTest {

    private val harness = LibraryJobHarness()
    private val first = libraryEntry(libManga(id = 1, source = 10), categories = listOf(1L))
    private val second = libraryEntry(libManga(id = 2, source = 20).copy(ogStatus = 3), categories = listOf(2L))
    private val third = libraryEntry(libManga(id = 3, source = 10), categories = listOf(1L, 3L))

    @Before
    fun setUp() {
        harness.start()
        coEvery { harness.getLibraryManga.await() } returns listOf(first, second, third)
        every { harness.mdList.id } returns TrackerManager.MDLIST
    }

    @After
    fun tearDown() = harness.stop()

    private suspend fun select(category: Long = -1L, group: Int = LibraryGroup.BY_DEFAULT, extra: String? = null) =
        harness.job().selectMangaToUpdate(category, group, extra).map { it.id }

    private fun mode(mode: GroupLibraryMode) = harness.libraryPreferences.groupLibraryUpdateType.set(mode)

    private fun track(mangaId: Long, status: Long) = Track(
        id = mangaId,
        mangaId = mangaId,
        trackerId = TrackerManager.MDLIST,
        remoteId = 0,
        libraryId = null,
        title = "",
        lastChapterRead = 0.0,
        totalChapters = 0,
        status = status,
        score = 0.0,
        remoteUrl = "",
        startDate = 0,
        finishDate = 0,
        private = false,
    )

    @Test
    fun categoryPicksItsMembers() = runTest {
        select(category = 1L) shouldBe listOf(1L, 3L)
    }

    @Test
    fun wholeLibraryHonoursPrefs() = runTest {
        select() shouldBe listOf(1L, 2L, 3L)
        harness.libraryPreferences.updateCategories.set(setOf("1"))
        select() shouldBe listOf(1L, 3L)
        harness.libraryPreferences.updateCategoriesExclude.set(setOf("3"))
        select() shouldBe listOf(1L)
    }

    @Test
    fun globalModeIgnoresTheGroup() = runTest {
        mode(GroupLibraryMode.GLOBAL)
        select(group = LibraryGroup.BY_STATUS, extra = "3") shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun ungroupedViewIsWholeLibrary() = runTest {
        mode(GroupLibraryMode.ALL_BUT_UNGROUPED)
        select(group = LibraryGroup.UNGROUPED) shouldBe listOf(1L, 2L, 3L)
        select(group = LibraryGroup.BY_STATUS, extra = "3") shouldBe listOf(2L)
        mode(GroupLibraryMode.ALL)
        harness.libraryPreferences.updateCategories.set(setOf("2"))
        select(group = LibraryGroup.UNGROUPED) shouldBe listOf(1L, 2L, 3L)
        select(group = LIBRARY_GROUP_UNKNOWN) shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun statusGroupNeedsANumber() = runTest {
        mode(GroupLibraryMode.ALL)
        select(group = LibraryGroup.BY_STATUS, extra = null) shouldBe emptyList()
        select(group = LibraryGroup.BY_STATUS, extra = "0") shouldBe listOf(1L, 3L)
    }

    @Test
    fun sourceGroupIndexesSources() = runTest {
        mode(GroupLibraryMode.ALL)
        select(group = LibraryGroup.BY_SOURCE, extra = "0") shouldBe listOf(1L, 3L)
        select(group = LibraryGroup.BY_SOURCE, extra = "1") shouldBe listOf(2L)
        select(group = LibraryGroup.BY_SOURCE, extra = "5") shouldBe emptyList()
        select(group = LibraryGroup.BY_SOURCE, extra = " ") shouldBe emptyList()
        select(group = LibraryGroup.BY_SOURCE, extra = "x") shouldBe emptyList()
        select(group = LibraryGroup.BY_SOURCE, extra = null) shouldBe emptyList()
    }

    @Test
    fun trackGroupMatchesStatus() = runTest {
        mode(GroupLibraryMode.ALL)
        coEvery { harness.getTracks.await() } returns listOf(
            track(mangaId = 1, status = FollowStatus.READING.long),
            track(mangaId = 2, status = FollowStatus.UNFOLLOWED.long),
        )
        select(group = LibraryGroup.BY_TRACK_STATUS, extra = "1") shouldBe listOf(1L)
        select(group = LibraryGroup.BY_TRACK_STATUS, extra = "7") shouldBe listOf(2L, 3L)
        select(group = LibraryGroup.BY_TRACK_STATUS, extra = null) shouldBe emptyList()
        select(group = LibraryGroup.BY_TRACK_STATUS, extra = "x") shouldBe emptyList()
    }

    private companion object {
        const val LIBRARY_GROUP_UNKNOWN = 99
    }
}
