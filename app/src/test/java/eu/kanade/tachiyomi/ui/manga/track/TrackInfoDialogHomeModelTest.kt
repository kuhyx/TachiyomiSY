package eu.kanade.tachiyomi.ui.manga.track

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.manga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class TrackInfoDialogHomeModelTest {
    private val harness = TrackHomeHarness()
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun model(sourceId: Long = 7L) = TrackInfoDialogHomeModel(mangaId = 1L, sourceId = sourceId)

    private fun enhanced(accepts: Boolean): BaseTracker {
        val tracker = mockk<BaseTracker>(relaxed = true, moreInterfaces = arrayOf(EnhancedTracker::class))
        every { (tracker as EnhancedTracker).accept(any()) } returns accepts
        every { tracker.id } returns 2L
        return tracker
    }

    @Test
    fun itemsFollowTheTracks() {
        val hidden = enhanced(accepts = false)
        val shown = enhanced(accepts = true)
        every { harness.trackerManager.loggedInTrackers() } returns listOf(harness.tracker, hidden, shown)
        harness.tracks.value = listOf(domainTrack(trackerId = 1L))
        val model = model()
        eventually { model.state.value.trackItems.size == 2 }
        model.state.value.trackItems.first().track?.trackerId shouldBe 1L
        model.state.value.copy(isLoading = true).isLoading shouldBe true
    }

    @Test
    fun refreshErrorsAreToasted() {
        val broken = harness.tracker(3L, "Broken")
        coEvery { harness.refreshTracks.await(1L) } returns listOf(
            null to IllegalStateException(),
            broken to IllegalStateException("down"),
            broken to IllegalStateException(),
        )
        model()
        eventually { ShadowToast.shownToastCount() == 2 }
        ShadowToast.getTextOfLatestToast().contains("Broken") shouldBe true
    }

    @Test
    fun trackErrorsAreLogged() {
        coEvery { harness.getTracks.subscribe(any<Long>()) } returns flow { error("db") }
        model().state.value.trackItems shouldBe emptyList()
    }

    @Test
    fun enhancedTrackingRegisters() {
        val tracker = enhanced(accepts = true)
        val item = TrackItem(null, tracker)
        coEvery { harness.getManga.await(1L) } returnsMany listOf(null, manga(), manga())
        coEvery { (tracker as EnhancedTracker).match(any()) } returnsMany listOf(mockk(relaxed = true), null)
        val model = model()
        model.registerEnhancedTracking(item)
        model.registerEnhancedTracking(item)
        coVerify(timeout = 5_000) { tracker.register(any(), 1L) }
        model.registerEnhancedTracking(item)
        eventually { ShadowToast.shownToastCount() == 1 }
    }

    @Test
    fun searchOpensTheSearchScreen() {
        val model = model()
        model.newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(TrackerSearchScreen(1L, "Needle", null, 1L)) }
        val track = domainTrack(trackerId = 1L, remoteUrl = "u")
        model.newSearch(navigator, TrackItem(track, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(TrackerSearchScreen(1L, "Title", "u", 1L)) }
    }

    @Test
    fun metadataIdRegistersDirectly() {
        harness.trackPreferences.resolveUsingSourceMetadata.set(true)
        every { harness.trackerManager.aniList.id } returns 1L
        coEvery { harness.getFlatMetadata.await(1L) } returns harness.metadata(anilist = "42")
        coEvery { harness.tracker.searchById("42") } returnsMany listOf(mockk(relaxed = true), null)
        val model = model()
        model.newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        coVerify(timeout = 5_000) { harness.tracker.register(any(), 1L) }
        verify(exactly = 0) { navigator.push(any<TrackerSearchScreen>()) }
        model.newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(any<TrackerSearchScreen>()) }
        model.state.value.isLoading shouldBe false
    }

    @Test
    fun unresolvedMetadataSearches() {
        harness.trackPreferences.resolveUsingSourceMetadata.set(true)
        coEvery { harness.getFlatMetadata.await(1L) } returns null
        model().newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(any<TrackerSearchScreen>()) }
    }

    @Test
    fun idsMapPerTracker() = runBlocking {
        every { harness.trackerManager.aniList.id } returns 1L
        every { harness.trackerManager.kitsu.id } returns 2L
        every { harness.trackerManager.myAnimeList.id } returns 3L
        every { harness.trackerManager.mangaUpdates.id } returns 4L
        coEvery { harness.getFlatMetadata.await(1L) } returns harness.metadata("a", "k", "m", "u")
        val model = model()
        (1L..5L).map { model.getTrackerIdFromMetadata(it) } shouldBe listOf("a", "k", "m", "u", null)
        model(sourceId = 8L).getTrackerIdFromMetadata(1L) shouldBe null
        every { harness.sourceManager.get(7L) } returns mockk(relaxed = true)
        model.getTrackerIdFromMetadata(1L) shouldBe null
        coEvery { harness.getFlatMetadata.await(1L) } throws IllegalStateException()
        every { harness.sourceManager.get(7L) } returns harness.metadataSource
        model.getTrackerIdFromMetadata(1L) shouldBe null
    }

    @Test
    fun registerByIdHandlesFailures() = runBlocking {
        val model = model()
        model.registerTrackingById(99L, "x") shouldBe false
        coEvery { harness.tracker.searchById("x") } throws IllegalStateException()
        model.registerTrackingById(1L, "x") shouldBe false
    }

    @Test
    fun togglesPrivacy() {
        val track = domainTrack(trackerId = 1L)
        model().togglePrivate(TrackItem(track, harness.tracker))
        coVerify(timeout = 5_000) { harness.tracker.setRemotePrivate(any(), true) }
        TrackItem(track, harness.tracker).copy(track = null).track shouldBe null
    }
}
