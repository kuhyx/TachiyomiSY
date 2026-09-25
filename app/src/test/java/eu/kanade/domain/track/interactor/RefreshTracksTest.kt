package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.domainTrack
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

internal class RefreshTracksTest {

    private val getTracks = mockk<GetTracks>()
    private val trackerManager = mockk<TrackerManager>()
    private val insertTrack = mockk<InsertTrack>()
    private val sync = mockk<SyncChapterProgressWithTrack>()
    private val interactor = RefreshTracks(getTracks, trackerManager, insertTrack, sync)

    private val loggedIn = mockk<BaseTracker> {
        every { id } returns 1L
        every { isLoggedIn } returns true
    }
    private val loggedOut = mockk<BaseTracker> {
        every { id } returns 2L
        every { isLoggedIn } returns false
    }
    private val broken = mockk<BaseTracker> {
        every { id } returns 3L
        every { isLoggedIn } returns true
    }

    private fun stubTrackers() {
        every { trackerManager.get(1) } returns loggedIn
        every { trackerManager.get(2) } returns loggedOut
        every { trackerManager.get(3) } returns broken
        every { trackerManager.get(4) } returns null
    }

    @Test
    fun refreshesOnlyLoggedInTrackers() = runTest {
        val tracks = listOf(
            domainTrack(id = 1, trackerId = 1),
            domainTrack(id = 2, trackerId = 2),
            domainTrack(id = 4, trackerId = 4),
        )
        coEvery { getTracks.await(9) } returns tracks
        stubTrackers()
        val refreshed = domainTrack(id = 1, trackerId = 1, lastChapterRead = 10.0)
        coEvery { loggedIn.refresh(any()) } returns refreshed.toDbTrack()
        coEvery { insertTrack.await(refreshed) } returns Unit
        coEvery { sync.await(9, refreshed, loggedIn) } returns Unit
        interactor.await(9) shouldBe emptyList()
        coVerify(exactly = 1) { insertTrack.await(refreshed) }
        coVerify(exactly = 0) { loggedOut.refresh(any()) }
    }

    @Test
    fun reportsFailures() = runTest {
        val failure = IllegalStateException("down")
        coEvery { getTracks.await(9) } returns listOf(domainTrack(id = 3, trackerId = 3))
        stubTrackers()
        coEvery { broken.refresh(any()) } throws failure
        interactor.await(9) shouldBe listOf(broken to failure)
    }
}
