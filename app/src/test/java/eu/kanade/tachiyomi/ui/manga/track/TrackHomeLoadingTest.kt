package eu.kanade.tachiyomi.ui.manga.track

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.i18n.MR

/** The home dialog's placeholder while a metadata id registers, and links that have no URL to act on. */
@RunWith(RobolectricTestRunner::class)
internal class TrackHomeLoadingTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = TrackHomeHarness()
    private val tracker get() = harness.tracker
    private val track = domainTrack(trackerId = 1L, score = 5.0, remoteUrl = "")

    @Before
    fun setUp() {
        harness.start()
        every { tracker.getLogo() } returns R.drawable.brand_anilist
        every { tracker.getStatus(any()) } returns MR.strings.reading
        every { tracker.displayScore(any()) } returns "5"
        harness.tracks.value = listOf(track)
    }

    @After
    fun tearDown() = harness.stop()

    private fun waitFor(text: String) = compose.waitUntil(timeoutMillis = 10_000) {
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun registeringByIdShowsLoading() {
        val hold = CompletableDeferred<TrackSearch?>()
        harness.trackPreferences.resolveUsingSourceMetadata.set(true)
        every { harness.trackerManager.aniList.id } returns 1L
        coEvery { harness.getFlatMetadata.await(1L) } returns harness.metadata(anilist = "42")
        coEvery { tracker.searchById("42") } coAnswers { hold.await() }
        coEvery { tracker.search(any()) } returns emptyList()
        compose.setContent {
            MaterialTheme { Navigator(listOf(BlankScreen(), TrackInfoDialogHomeScreen(1L, "Needle", 7L))) }
        }
        waitFor("Title")
        compose.onNodeWithText("Title").performClick()
        waitFor("Loading…")
        hold.complete(null)
        waitFor("No results found")
    }

    @Test
    fun linksWithoutUrlDoNothing() {
        val screen = TrackInfoDialogHomeScreen(1L, "Needle", 7L)
        screen.openTrackerInBrowser(harness.app, TrackItem(null, tracker))
        screen.openTrackerInBrowser(harness.app, TrackItem(track, tracker))
        harness.app.copyTrackerLink(TrackItem(null, tracker))
        harness.app.copyTrackerLink(TrackItem(track, tracker))
        shadowOf(harness.app).nextStartedActivity.shouldBeNull()
        harness.app.getSystemService(ClipboardManager::class.java).primaryClip.shouldBeNull()
    }
}
