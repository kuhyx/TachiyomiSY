package eu.kanade.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.DownloadAction
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadDropdownMenuTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun everyOptionIsListed() {
        compose.setContent {
            MaterialTheme {
                DownloadDropdownMenu(
                    expanded = true,
                    onDismissRequest = { events += "dismiss" },
                    onDownloadClicked = { events += it.name },
                )
            }
        }
        listOf("Next chapter", "Next 5 chapters", "Next 10 chapters", "Next 25 chapters", "Unread", "Bookmarked")
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Bookmarked").performClick()
        events shouldContainExactly listOf(DownloadAction.BOOKMARKED_CHAPTERS.name, "dismiss")
    }

    @Test
    fun offsetMenuForwardsToo() {
        compose.setContent {
            MaterialTheme {
                DownloadDropdownMenu(
                    modifier = Modifier,
                    expanded = true,
                    onDismissRequest = { events += "dismiss" },
                    onDownloadClicked = { events += it.name },
                    offset = DpOffset(1.dp, 1.dp),
                )
            }
        }
        compose.onNodeWithText("Unread").performClick()
        events shouldContainExactly listOf(DownloadAction.UNREAD_CHAPTERS.name, "dismiss")
    }
}
