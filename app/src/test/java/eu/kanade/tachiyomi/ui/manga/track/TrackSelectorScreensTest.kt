package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.presentation.util.Screen as AppScreen
import io.mockk.coVerify
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR

/** The screen a dialog pops back to. */
internal class BlankScreen : AppScreen() {
    @Composable
    override fun Content() {
        Text("Blank")
    }
}

@RunWith(RobolectricTestRunner::class)
internal class TrackSelectorScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = TrackHomeHarness()
    private val tracker get() = harness.tracker
    private val track: Track = domainTrack(trackerId = 1L).copy(status = 1L, lastChapterRead = 3.0, totalChapters = 5L)

    @Before
    fun setUp() {
        harness.start()
        every { tracker.getStatusList() } returns listOf(1L, 2L)
        every { tracker.getStatus(1L) } returns MR.strings.reading
        every { tracker.getStatus(2L) } returns null
        every { tracker.displayScore(any()) } returns "5"
        every { tracker.getScoreList() } returns (1..10).map { it.toString() }
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(screen: Screen) {
        compose.setContent { MaterialTheme { Navigator(listOf(BlankScreen(), screen)) } }
        compose.waitForIdle()
    }

    private fun confirmAndClose() {
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Blank").assertExists()
    }

    @Test
    fun statusIsSaved() {
        show(TrackStatusSelectorScreen(track, 1L))
        compose.onNodeWithText("Status").assertExists()
        compose.onNodeWithText("Reading").performClick()
        confirmAndClose()
        coVerify(timeout = 5_000) { tracker.setRemoteStatus(any(), 1L) }
    }

    @Test
    fun chapterIsSaved() {
        show(TrackChapterSelectorScreen(track, 1L))
        compose.onNodeWithText("Chapters").assertExists()
        confirmAndClose()
        coVerify(timeout = 5_000) { tracker.setRemoteLastChapterRead(any(), 3) }
    }

    @Test
    fun unboundedChapterRange() {
        show(TrackChapterSelectorScreen(track.copy(totalChapters = 0L), 1L))
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Blank").assertExists()
    }

    @Test
    fun scoreIsSaved() {
        show(TrackScoreSelectorScreen(track, 1L))
        compose.onNodeWithText("Score").assertExists()
        confirmAndClose()
        coVerify(timeout = 5_000) { tracker.setRemoteScore(any(), "5") }
    }

    @Test
    fun startDateIsSaved() {
        show(TrackDateSelectorScreen(track, 1L, start = true))
        compose.onNodeWithText("Start date").assertExists()
        confirmAndClose()
        coVerify(timeout = 5_000) { tracker.setRemoteStartDate(any(), any()) }
    }

    @Test
    fun finishDateIsSaved() {
        show(TrackDateSelectorScreen(track.copy(finishDate = 86_400_000L), 1L, start = false))
        compose.onNodeWithText("Finish date").assertExists()
        confirmAndClose()
        coVerify(timeout = 5_000) { tracker.setRemoteFinishDate(any(), any()) }
    }

    @Test
    fun startDateCanBeRemoved() {
        show(TrackDateSelectorScreen(track.copy(startDate = 86_400_000L), 1L, start = true))
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Remove date?").assertExists()
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { tracker.setRemoteStartDate(any(), 0L) }
    }

    @Test
    fun finishDateIsRemoved() {
        show(TrackDateRemoverScreen(track, 1L, start = false))
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { tracker.setRemoteFinishDate(any(), 0L) }
    }

    @Test
    fun dateRemovalCanBeCancelled() {
        show(TrackDateRemoverScreen(track, 1L, start = true))
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Blank").assertExists()
    }
}
