package eu.kanade.tachiyomi.data.library

import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import exh.md.utils.FollowStatus
import exh.source.mangaDexSourceIds
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.track.model.Track
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

/** MDList trackers: pushing favourites as follows and creating the first tracker of an entry. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateMdListTest : LibraryUpdateTestBase() {

    @Before
    fun setUpMdList() {
        mangaDexSourceIds = listOf(MD_ID)
        every { mdList.createInitialTracker(any(), any()) } answers { dbTrack(FollowStatus.UNFOLLOWED.long) }
        coEvery { mdList.update(any(), any()) } answers { firstArg() }
        coEvery { mdList.refresh(any()) } answers { firstArg() }
    }

    @After
    fun restoreMangaDexIds() {
        mangaDexSourceIds = emptyList()
    }

    private fun dbTrack(status: Long, tracker: Long = TrackerManager.MDLIST): DbTrack =
        DbTrack.create(tracker).also { it.status = status }

    private fun track(tracker: Long, followStatus: Long = FollowStatus.READING.long): Track =
        dbTrack(followStatus, tracker).toDomainTrack(idRequired = false)!!

    @Test
    fun loggedOutPushesNothing() = runTest {
        coEvery { getFavorites.await() } returns listOf(manga(1L, source = MD_ID))
        job().pushFavorites()
        coVerify(exactly = 0) { getTracks.await(any<Long>()) }
    }

    @Test
    fun unfollowedFavoritesAreFollowed() = runTest {
        every { mdList.isLoggedIn } returns true
        coEvery { getFavorites.await() } returns listOf(1L, 2L, 3L, 5L).map { manga(it, source = MD_ID) } + manga(4L)
        coEvery { getTracks.await(1L) } returns emptyList()
        coEvery { getTracks.await(2L) } returns listOf(track(TrackerManager.MDLIST))
        coEvery { getTracks.await(3L) } returns listOf(track(TrackerManager.MDLIST, FollowStatus.UNFOLLOWED.long))
        coEvery { getTracks.await(5L) } returns listOf(track(OTHER_TRACKER))
        job().pushFavorites()
        coVerify(exactly = 3) { mdList.update(match { it.status == FollowStatus.READING.long }, any()) }
        coVerify(exactly = 3) { insertTrack.await(any()) }
        coVerify(exactly = 0) { getTracks.await(4L) }
    }

    @Test
    fun missingTrackersAreCreated() = runTest {
        coEvery { getTracks.await(1L) } returns emptyList()
        coEvery { getTracks.await(2L) } returns listOf(track(OTHER_TRACKER))
        coEvery { getTracks.await(3L) } returns listOf(track(TrackerManager.MDLIST))
        coEvery { getTracks.await(4L) } throws IllegalStateException("db")
        job().addInitialMdListTracks(listOf(1L, 2L, 3L, 4L).map { libraryManga(manga(it, source = MD_ID)) })
        coVerify(exactly = 2) { mdList.refresh(any()) }
        coVerify(exactly = 2) { insertTrack.await(any()) }
    }

    @Test
    fun cancellationPropagates() = runTest {
        coEvery { getTracks.await(1L) } throws CancellationException("stop")
        shouldThrow<CancellationException> {
            job().addInitialMdListTracks(listOf(libraryManga(manga(1L, source = MD_ID))))
        }
    }

    private companion object {
        const val MD_ID = 7L
        const val OTHER_TRACKER = 1L
    }
}
