package eu.kanade.domain.track.interactor

import android.content.Context
import eu.kanade.domain.track.model.domainTrack
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateJob
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

internal class TrackChapterTest {

    private val context = mockk<Context>()
    private val getTracks = mockk<GetTracks>()
    private val trackerManager = mockk<TrackerManager>()
    private val insertTrack = mockk<InsertTrack>()
    private val store = mockk<DelayedTrackingStore>(relaxed = true)
    private val interactor = TrackChapter(getTracks, trackerManager, insertTrack, store)

    private val tracker = mockk<BaseTracker> {
        every { id } returns 1L
        every { isLoggedIn } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun skipsWhatCannotBeUpdated() = runTest {
        val loggedOut = mockk<BaseTracker> { every { isLoggedIn } returns false }
        val mdList = mockk<MdList> { every { isLoggedIn } returns true }
        val followedMdList = mockk<MdList> { every { isLoggedIn } returns true }
        every { trackerManager.get(1) } returns tracker
        every { trackerManager.get(2) } returns loggedOut
        every { trackerManager.get(3) } returns mdList
        every { trackerManager.get(4) } returns null
        every { trackerManager.get(5) } returns followedMdList
        coEvery { getTracks.await(9) } returns listOf(
            domainTrack(id = 1, trackerId = 1, lastChapterRead = 10.0),
            domainTrack(id = 2, trackerId = 2, lastChapterRead = 0.0),
            domainTrack(id = 3, trackerId = 3, lastChapterRead = 0.0, status = 0),
            domainTrack(id = 4, trackerId = 4, lastChapterRead = 0.0),
            domainTrack(id = 5, trackerId = 5, lastChapterRead = 5.0, status = 1),
        )
        interactor.await(context, 9, 5.0)
        coVerify(exactly = 0) { insertTrack.await(any()) }
    }

    @Test
    fun pushesTheNewChapter() = runTest {
        every { trackerManager.get(1) } returns tracker
        val track = domainTrack(id = 1, trackerId = 1, lastChapterRead = 2.0)
        coEvery { getTracks.await(9) } returns listOf(track)
        coEvery { tracker.refresh(any()) } returns track.toDbTrack()
        coEvery { tracker.update(any(), true) } answers { firstArg() }
        coEvery { insertTrack.await(track.copy(lastChapterRead = 5.0)) } returns Unit
        interactor.await(context, 9, 5.0)
        coVerify(exactly = 1) { insertTrack.await(track.copy(lastChapterRead = 5.0)) }
        verify(exactly = 1) { store.remove(1) }
    }

    @Test
    fun queuesFailuresWithRetryJob() = runTest {
        every { trackerManager.get(1) } returns tracker
        coEvery { getTracks.await(9) } returns listOf(domainTrack(id = 1, trackerId = 1, lastChapterRead = 2.0))
        coEvery { tracker.refresh(any()) } throws IllegalStateException("offline")
        mockkObject(DelayedTrackingUpdateJob.Companion)
        every { DelayedTrackingUpdateJob.setupTask(context) } returns Unit
        interactor.await(context, 9, 5.0)
        verify(exactly = 1) { store.add(1, 5.0) }
        verify(exactly = 1) { DelayedTrackingUpdateJob.setupTask(context) }
    }

    @Test
    fun queuesFailuresWithoutRetryJob() = runTest {
        every { trackerManager.get(1) } returns tracker
        coEvery { getTracks.await(9) } returns listOf(domainTrack(id = 1, trackerId = 1, lastChapterRead = 2.0))
        coEvery { tracker.refresh(any()) } throws IllegalStateException("offline")
        mockkObject(DelayedTrackingUpdateJob.Companion)
        interactor.await(context, 9, 5.0, setupJobOnFailure = false)
        verify(exactly = 1) { store.add(1, 5.0) }
        verify(exactly = 0) { DelayedTrackingUpdateJob.setupTask(any()) }
    }
}
