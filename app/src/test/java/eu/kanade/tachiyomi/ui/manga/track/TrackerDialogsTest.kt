package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.track.interactor.DeleteTrack

@RunWith(RobolectricTestRunner::class)
internal class TrackerDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = TrackHomeHarness()
    private val deleteTrack = mockk<DeleteTrack>(relaxed = true)
    private val track = domainTrack(trackerId = 1L)

    @Before
    fun setUp() {
        harness.start()
        loadKoinModules(module { single { deleteTrack } })
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(screen: Screen) {
        compose.setContent { MaterialTheme { Navigator(listOf(BlankScreen(), screen)) } }
        compose.waitForIdle()
    }

    @Test
    fun removeUnregisters() {
        show(TrackerRemoveScreen(1L, track, 1L))
        compose.onNodeWithText("Remove Plain tracking?").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { deleteTrack.await(1L, 1L) }
    }

    @Test
    fun removeAlsoDeletesRemotely() {
        val deletable = mockk<BaseTracker>(relaxed = true, moreInterfaces = arrayOf(DeletableTracker::class))
        every { deletable.name } returns "Del"
        coEvery { (deletable as DeletableTracker).delete(any()) } throws IllegalStateException("remote")
        every { harness.trackerManager.get(5L) } returns deletable
        show(TrackerRemoveScreen(1L, track, 5L))
        compose.onNodeWithText("Also remove from Del").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { (deletable as DeletableTracker).delete(track) }
    }

    @Test
    fun removeCanBeCancelled() {
        show(TrackerRemoveScreen(1L, track, 1L))
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Blank").assertExists()
    }

    @Test
    fun searchRegistersTheSelection() {
        val hit = TrackSearch.create(1L).apply {
            title = "Hit"
            trackingUrl = "u"
        }
        coEvery { harness.tracker.search("Needle") } returns listOf(hit)
        show(TrackerSearchScreen(1L, "Needle", "u", 1L))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Hit")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Track").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { harness.tracker.register(hit, 1L) }
    }

    @Test
    fun searchFailureShowsError() {
        coEvery { harness.tracker.search(any()) } throws IllegalStateException("offline")
        show(TrackerSearchScreen(1L, "Needle", null, 1L))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("offline", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun blankQueryWaits() {
        show(TrackerSearchScreen(1L, "", null, 1L))
        coVerify(exactly = 0) { harness.tracker.search(any()) }
    }
}
