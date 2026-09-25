package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import exh.md.utils.FollowStatus
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

private const val MANGADEX = 2499283573021220255L

@RunWith(RobolectricTestRunner::class)
internal class MangaTrackingTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val saved = mangaDexSourceIds
    private val plain = tracker(1L)
    private val tracks = MutableStateFlow(listOf(domainTrack(trackerId = 1L)))

    private fun tracker(trackerId: Long): Tracker = mockk { every { id } returns trackerId }

    @Before
    fun setUp() {
        mangaDexSourceIds = listOf(MANGADEX)
        harness.start()
        coEvery { parts.getTracks.subscribe(1L) } returns tracks
        every { harness.trackerManager.loggedInTrackersFlow() } returns MutableStateFlow(listOf(plain))
    }

    @After
    fun tearDown() {
        harness.stop()
        mangaDexSourceIds = saved
    }

    private fun load(manga: Manga): MangaScreenModel {
        harness.mangaFlow.value = manga to listOf(chapter(1L))
        return harness.loaded()
    }

    @Test
    fun countsSupportedTracks() {
        val state = load(manga(favorite = true)).awaitSuccess { it.hasLoggedInTrackers }
        state.trackingCount shouldBe 1
    }

    @Test
    fun enhancedTrackersMustAccept() {
        val enhanced = mockk<EnhancedTracker>(moreInterfaces = arrayOf(Tracker::class)) {
            every { accept(any()) } returns false
        }
        every { (enhanced as Tracker).id } returns 1L
        every { harness.trackerManager.loggedInTrackersFlow() } returns MutableStateFlow(listOf(enhanced as Tracker))
        val state = load(manga(favorite = true)).awaitSuccess()
        state.trackingCount shouldBe 0
        state.hasLoggedInTrackers shouldBe false
    }

    @Test
    fun unfollowedMdListDoesNotCount() {
        val md = tracker(TrackerManager.MDLIST)
        every { harness.trackerManager.loggedInTrackersFlow() } returns MutableStateFlow(listOf(plain, md))
        tracks.value = listOf(
            domainTrack(trackerId = 1L),
            domainTrack(id = 2L, trackerId = TrackerManager.MDLIST).copy(status = FollowStatus.UNFOLLOWED.long),
            domainTrack(id = 3L, trackerId = TrackerManager.MDLIST).copy(status = FollowStatus.READING.long),
        )
        load(manga(favorite = true)).awaitSuccess { it.trackingCount == 2 }
    }

    @Test
    fun mangaDexGetsAnMdListTrack() {
        every { harness.trackerManager.mdList.isLoggedIn } returns true
        every { harness.trackerManager.mdList.id } returns TrackerManager.MDLIST
        coEvery { harness.trackerManager.mdList.createInitialTracker(any(), any()) } returns
            dbTrack(TrackerManager.MDLIST).also { it.manga_id = 1L }
        coEvery { parts.getTracks.await(1L) } returns listOf(domainTrack(id = 7L, trackerId = TrackerManager.MDLIST))
        load(manga(source = MANGADEX, favorite = true)).awaitSuccess { it.hasLoggedInTrackers }
        coVerify(timeout = 5_000) { parts.insertTrack.await(any()) }
    }

    @Test
    fun loggedOutMdListAddsNothing() {
        every { harness.trackerManager.mdList.isLoggedIn } returns false
        load(manga(source = MANGADEX, favorite = true)).awaitSuccess { it.hasLoggedInTrackers }
        tracks.value = listOf(domainTrack(trackerId = TrackerManager.MDLIST))
        coVerify(exactly = 0) { parts.insertTrack.await(any()) }
    }

    @Test
    fun mergedMangaDexMemberCounts() {
        every { harness.trackerManager.mdList.isLoggedIn } returns true
        every { harness.trackerManager.mdList.id } returns TrackerManager.MDLIST
        coEvery { harness.trackerManager.mdList.createInitialTracker(any(), any()) } returns
            dbTrack(TrackerManager.MDLIST).also { it.manga_id = 1L }
        coEvery { parts.getTracks.await(1L) } returns listOf(domainTrack(id = 7L, trackerId = TrackerManager.MDLIST))
        val member = manga().copy(id = 5L, source = MANGADEX)
        coEvery { harness.getMergedReferences.await(1L) } returns listOf(mockk(relaxed = true))
        coEvery { harness.getMergedManga.await(1L) } returns listOf(member, manga().copy(id = 6L))
        load(manga(favorite = true)).awaitSuccess { it.hasLoggedInTrackers }
        coVerify(timeout = 5_000) { harness.trackerManager.mdList.createInitialTracker(any(), member) }
    }

    @Test
    fun trackErrorsAreLogged() {
        coEvery { parts.getTracks.subscribe(1L) } returns flow { error("db") }
        val model = load(manga(favorite = true))
        model.awaitSuccess().trackingCount shouldBe 0
        harness.loading().observeTrackers()
    }
}
