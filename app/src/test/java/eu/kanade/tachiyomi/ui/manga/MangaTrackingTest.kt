package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
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
import tachiyomi.domain.manga.model.MergedMangaReference
import java.util.concurrent.atomic.AtomicBoolean

private const val MANGADEX = 2_499_283_573_021_220_255L

@RunWith(RobolectricTestRunner::class)
internal class MangaTrackingTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val saved = mangaDexSourceIds
    private val plain = tracker(1L)
    private val tracks = MutableStateFlow(listOf(domainTrack(trackerId = 1L)))

    private fun tracker(trackerId: Long): BaseTracker = mockk { every { id } returns trackerId }

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
        val enhanced = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        every { (enhanced as EnhancedTracker).accept(any()) } returns false
        every { enhanced.id } returns 1L
        every { harness.trackerManager.loggedInTrackersFlow() } returns MutableStateFlow(listOf(enhanced))
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
            dbTrack(TrackerManager.MDLIST).also { it.mangaId = 1L }
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
        val created = AtomicBoolean()
        coEvery { harness.trackerManager.mdList.createInitialTracker(any(), any()) } answers {
            created.set(true)
            dbTrack(TrackerManager.MDLIST).also { it.mangaId = 1L }
        }
        coEvery { parts.getTracks.await(1L) } returns listOf(domainTrack(id = 7L, trackerId = TrackerManager.MDLIST))
        val member = manga().copy(id = 5L, source = MANGADEX)
        val references = listOf(mockk<MergedMangaReference>(relaxed = true))
        val members = listOf(member, manga().copy(id = 6L))
        // The load reads the merge tables once and the observer keeps reading them: both must agree.
        coEvery { harness.getMergedReferences.await(1L) } returns references
        coEvery { harness.getMergedReferences.subscribe(1L) } returns MutableStateFlow(references)
        coEvery { harness.getMergedManga.await(1L) } returns members
        coEvery { harness.getMergedManga.subscribe(1L) } returns MutableStateFlow(members)
        load(manga(favorite = true)).awaitSuccess { it.hasLoggedInTrackers }
        // coVerify(timeout) would block the main looper the tracker work resumes on; idle it instead.
        eventually { created.get() }
        coVerify { harness.trackerManager.mdList.createInitialTracker(any(), member) }
    }

    @Test
    fun trackErrorsAreLogged() {
        coEvery { parts.getTracks.subscribe(1L) } returns flow { error("db") }
        val model = load(manga(favorite = true))
        model.awaitSuccess().trackingCount shouldBe 0
        harness.loading().observeTrackers()
    }
}
