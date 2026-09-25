package eu.kanade.presentation.track

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.test.DummyTracker
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.track.model.Track
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
internal class TrackInfoDialogHomeTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val day = 86_400_000L

    private fun track(total: Long = 12L, score: Double = 2.0, start: Long = 0L, finish: Long = 0L) = Track(
        id = 1L,
        mangaId = 2L,
        trackerId = 3L,
        remoteId = 4L,
        libraryId = null,
        title = "Tracked title",
        lastChapterRead = 2.0,
        totalChapters = total,
        status = 1L,
        score = score,
        remoteUrl = "https://example.com",
        startDate = start,
        finishDate = finish,
        private = true,
    )

    private fun show(vararg items: TrackItem) {
        compose.setContent {
            MaterialTheme {
                TrackInfoDialogHome(
                    trackItems = items.toList(),
                    dateFormat = DateTimeFormatter.ISO_LOCAL_DATE,
                    onStatusClick = { events += "status" },
                    onChapterClick = { events += "chapter" },
                    onScoreClick = { events += "score" },
                    onStartDateEdit = { events += "start" },
                    onEndDateEdit = { events += "end" },
                    onNewSearch = { events += "search" },
                    onOpenInBrowser = { events += "browser" },
                    onRemoved = { events += "removed" },
                    onCopyLink = { events += "copy" },
                    onTogglePrivate = { events += "private" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun emptyTrackerOffersSearch() {
        show(TrackItem(track = null, tracker = DummyTracker(id = 1L, name = "Dummy")))
        compose.onNodeWithText("Add tracking").performClick()
        events shouldContainExactly listOf("search")
    }

    @Test
    fun fullTrackerWiresEveryCell() {
        val tracker = DummyTracker(id = 1L, name = "Dummy", supportsReadingDates = true, supportsPrivateTracking = true)
        show(TrackItem(track = track(start = day * 3 / 2, finish = day * 5 / 2), tracker = tracker))
        compose.onNodeWithText("Reading").performClick()
        compose.onNodeWithText("2 / 12").performClick()
        compose.onNodeWithText("2.0").performClick()
        compose.onNodeWithText("1970-01-02").performClick()
        compose.onNodeWithText("1970-01-03").performClick()
        compose.onNodeWithText("Tracked title").performClick()
        compose.onNodeWithContentDescription("Dummy").performClick()
        compose.onNodeWithContentDescription("Tracked privately").assertExists()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Track publicly").performClick()
        events shouldContainExactly
            listOf("status", "chapter", "score", "start", "end", "search", "browser", "private")
    }

    @Test
    fun unsetValuesShowPlaceholders() {
        val tracker = DummyTracker(id = 1L, name = "Dummy", supportsReadingDates = true)
        show(TrackItem(track = track(total = 0L, score = 0.0), tracker = tracker))
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithText("Score").assertExists()
        compose.onNodeWithText("Start date").assertExists()
        compose.onNodeWithText("Finish date").assertExists()
    }

    @Test
    fun trackerWithoutScoresOrDates() {
        val tracker = DummyTracker(id = 1L, name = "Dummy", valScoreList = emptyList())
        show(TrackItem(track = track().copy(status = 99L), tracker = tracker))
        compose.onNodeWithText("Score").assertDoesNotExist()
        compose.onNodeWithText("Start date").assertDoesNotExist()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Track publicly").assertDoesNotExist()
        compose.onNodeWithText("Open in browser").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Copy link").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Remove").performClick()
        events shouldContainExactly listOf("browser", "copy", "removed")
    }

    @Test
    fun everyPreviewRenders() {
        val previews = TrackInfoDialogHomePreviewProvider().values.toList()
        compose.setContent { previews.forEach { TrackInfoDialogHomePreviews(it) } }
        compose.onNodeWithText("Track privately").assertDoesNotExist()
        compose.onNodeWithText("Example Tracker", substring = true).assertExists()
    }
}
