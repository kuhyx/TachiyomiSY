package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction

@RunWith(RobolectricTestRunner::class)
internal class ChapterListItemSwipeTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    private data class Row(
        val title: String,
        val read: Boolean,
        val bookmark: Boolean,
        val state: Download.State,
        val start: ChapterSwipeAction,
        val end: ChapterSwipeAction,
        val download: Boolean,
    )

    private fun show(vararg rows: Row) {
        compose.setContent {
            MaterialTheme {
                Column {
                    rows.forEachIndexed { index, row ->
                        MangaChapterListItem(
                            title = row.title,
                            date = "today",
                            readProgress = "Page: 2".takeIf { index == 0 },
                            scanlator = "Group",
                            sourceName = "Src".takeIf { index == 0 },
                            read = row.read,
                            bookmark = row.bookmark,
                            selected = false,
                            downloadIndicatorEnabled = true,
                            downloadStateProvider = { row.state },
                            downloadProgressProvider = { 0 },
                            chapterSwipeStartAction = row.start,
                            chapterSwipeEndAction = row.end,
                            onLongClick = {},
                            onClick = {},
                            onDownloadClick = { action: ChapterDownloadAction -> events += "${row.title} $action" }
                                .takeIf { row.download },
                            onChapterSwipe = { events += "${row.title} swipe $it" },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun swipe(title: String) {
        compose.onNodeWithText(title).performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithText(title).performTouchInput { swipeLeft() }
        compose.waitForIdle()
    }

    @Test
    fun bookmarkAndQueuedDownload() {
        val row = Row(
            title = "A",
            read = false,
            bookmark = true,
            state = Download.State.QUEUE,
            start = ChapterSwipeAction.ToggleBookmark,
            end = ChapterSwipeAction.Download,
            download = true,
        )
        show(row)
        swipe("A")
        events shouldContainExactly listOf("A swipe ToggleBookmark", "A swipe Download")
    }

    @Test
    fun readAndDownloading() {
        val row = Row(
            title = "B",
            read = true,
            bookmark = false,
            state = Download.State.DOWNLOADING,
            start = ChapterSwipeAction.ToggleRead,
            end = ChapterSwipeAction.Download,
            download = true,
        )
        show(row)
        swipe("B")
        events shouldContainExactly listOf("B swipe ToggleRead", "B swipe Download")
    }

    @Test
    fun disabledAndDownloaded() {
        val row = Row(
            title = "C",
            read = false,
            bookmark = false,
            state = Download.State.DOWNLOADED,
            start = ChapterSwipeAction.Disabled,
            end = ChapterSwipeAction.Download,
            download = false,
        )
        val error = Row(
            title = "D",
            read = false,
            bookmark = false,
            state = Download.State.ERROR,
            start = ChapterSwipeAction.ToggleRead,
            end = ChapterSwipeAction.Disabled,
            download = true,
        )
        show(row, error)
        swipe("C")
        swipe("D")
        compose.onAllNodesWithContentDescription("Error")[0].performClick()
        events shouldContainExactly listOf("C swipe Download", "D swipe ToggleRead", "D START")
    }

    @Test
    fun unbookmarkedWithoutDownloads() {
        val row = Row(
            title = "E",
            read = false,
            bookmark = false,
            state = Download.State.NOT_DOWNLOADED,
            start = ChapterSwipeAction.ToggleBookmark,
            end = ChapterSwipeAction.Disabled,
            download = false,
        )
        show(row)
        swipe("E")
        compose.onAllNodesWithContentDescription("Download")[0].performClick()
        events shouldContainExactly listOf("E swipe ToggleBookmark")
    }
}
