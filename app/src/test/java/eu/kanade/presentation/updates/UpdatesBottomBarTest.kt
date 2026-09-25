package eu.kanade.presentation.updates

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpdatesBottomBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(selected: List<UpdatesItem>) {
        compose.setContent {
            MaterialTheme {
                UpdatesBottomBar(
                    selected = selected,
                    onDownloadChapter = { items, action -> events += "download ${items.size} $action" },
                    onMultiBookmarkClicked = { items, bookmark -> events += "bookmark ${items.size} $bookmark" },
                    onMultiMarkAsReadClicked = { items, read -> events += "read ${items.size} $read" },
                    onMultiDeleteClicked = { events += "delete ${it.size}" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun freshChaptersForwardActions() {
        show(listOf(updatesItem()))
        compose.onNodeWithContentDescription("Unbookmark chapter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark as unread").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        compose.onNodeWithContentDescription("Bookmark chapter").performClick()
        compose.onNodeWithContentDescription("Mark as read").performClick()
        compose.onNodeWithContentDescription("Download").performClick()
        events shouldContainExactly listOf("bookmark 1 true", "read 1 true", "download 1 START")
    }

    @Test
    fun finishedChaptersOfferUndo() {
        show(listOf(updatesItem(read = true, bookmark = true, state = Download.State.DOWNLOADED)))
        compose.onNodeWithContentDescription("Bookmark chapter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark as read").assertDoesNotExist()
        compose.onNodeWithContentDescription("Download").assertDoesNotExist()
        compose.onNodeWithContentDescription("Unbookmark chapter").performClick()
        compose.onNodeWithContentDescription("Mark as unread").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        events shouldContainExactly listOf("bookmark 1 false", "read 1 false", "delete 1")
    }

    @Test
    fun startedChaptersMarkUnread() {
        show(listOf(updatesItem(lastPageRead = 3L)))
        compose.onNodeWithContentDescription("Mark as unread").assertExists()
    }

    @Test
    fun emptySelectionHidesTheBar() {
        show(emptyList())
        compose.onNodeWithContentDescription("Bookmark chapter").assertDoesNotExist()
    }
}
