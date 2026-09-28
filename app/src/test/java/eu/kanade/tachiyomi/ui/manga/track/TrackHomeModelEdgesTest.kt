package eu.kanade.tachiyomi.ui.manga.track

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.ui.manga.eventually
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Refresh failures in the log, privacy switched back off, and source metadata that carries no tracker ids. */
@RunWith(RobolectricTestRunner::class)
internal class TrackHomeModelEdgesTest {
    private val harness = TrackHomeHarness()
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun model() = TrackInfoDialogHomeModel(mangaId = 1L, sourceId = 7L)

    @Test
    fun refreshFailuresAreLogged() {
        val logged = captureLogcat()
        try {
            coEvery { harness.refreshTracks.await(1L) } returns listOf(
                harness.tracker(3L, "Broken") to IllegalStateException("down"),
            )
            model()
            eventually { logged.any { "Failed to refresh track data mangaId=1 for service 3" in it } }
        } finally {
            releaseLogcat()
        }
    }

    @Test
    fun privateTrackTurnsPublic() {
        val track = domainTrack(trackerId = 1L).copy(private = true)
        model().togglePrivate(TrackItem(track, harness.tracker))
        coVerify(timeout = 5_000) { harness.tracker.setRemotePrivate(any(), false) }
    }

    @Test
    fun metadataWithoutTrackerIds() {
        harness.trackPreferences.resolveUsingSourceMetadata.set(true)
        every { harness.metadataSource.metaClass } returns EHentaiSearchMetadata::class
        coEvery { harness.getFlatMetadata.await(1L) } returns EHentaiSearchMetadata().apply { mangaId = 1L }.flatten()
        model().newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(any<TrackerSearchScreen>()) }
    }

    @Test
    fun searchWithoutMetadataLookup() {
        harness.trackPreferences.resolveUsingSourceMetadata.set(false)
        model().newSearch(navigator, TrackItem(null, harness.tracker), "Needle")
        verify(timeout = 5_000) { navigator.push(any<TrackerSearchScreen>()) }
        coVerify(exactly = 0) { harness.getFlatMetadata.await(any()) }
        harness.trackPreferences.resolveUsingSourceMetadata.get() shouldBe false
    }
}
