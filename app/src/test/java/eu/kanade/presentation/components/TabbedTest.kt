package eu.kanade.presentation.components

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.tapOutsidePopup
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class TabbedTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun dialogSwitchesTabs() {
        compose.setContent {
            MaterialTheme {
                TabbedDialog(onDismissRequest = {}, tabTitles = listOf("One", "Two")) { page -> Text("page $page") }
            }
        }
        compose.onNodeWithText("page 0").assertExists()
        compose.onNodeWithText("Two").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("page 1").assertExists()
    }

    @Test
    fun dialogOverflowMenu() {
        compose.setContent {
            MaterialTheme {
                TabbedDialog(
                    onDismissRequest = {},
                    tabTitles = listOf("One"),
                    modifier = Modifier,
                    tabOverflowMenuContent = { close ->
                        DropdownMenuItem(
                            text = { Text("Act") },
                            onClick = {
                                events += "act"
                                close()
                            },
                        )
                    },
                    pagerState = rememberPagerState { 1 },
                ) { Text("body") }
            }
        }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Act").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Act").assertDoesNotExist()
        compose.onNodeWithContentDescription("More").performClick()
        compose.tapOutsidePopup()
        compose.onNodeWithText("Act").assertDoesNotExist()
        events shouldContainExactly listOf("act")
    }

    @Test
    fun screenSwitchesTabs() {
        compose.setContent {
            MaterialTheme {
                TabbedScreen(
                    titleRes = MR.strings.label_more,
                    tabs = listOf(
                        TabContent(titleRes = MR.strings.history) { _, _ -> Text("history page") },
                        TabContent(
                            titleRes = MR.strings.label_downloaded,
                            badgeNumber = 3,
                            searchEnabled = true,
                            actions = listOf(AppBar.Action(title = "Star", icon = Icons.Outlined.Star, onClick = {})),
                        ) { _, _ -> Text("second page") },
                    ),
                )
            }
        }
        compose.onNodeWithText("history page").assertExists()
        compose.onNodeWithContentDescription("Search").assertDoesNotExist()
        compose.onNodeWithText("Downloaded").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("second page").assertExists()
        compose.onNodeWithContentDescription("Search").performClick()
    }

    @Test
    fun screenWithSearchQuery() {
        compose.setContent {
            MaterialTheme {
                TabbedScreen(
                    titleRes = MR.strings.label_more,
                    tabs = listOf(TabContent(titleRes = MR.strings.history, searchEnabled = true) { _, _ -> }),
                    state = rememberPagerState { 1 },
                    searchQuery = "abc",
                    onChangeSearchQuery = { events += "query $it" },
                )
            }
        }
        compose.onNodeWithText("abc").assertExists()
        compose.onNodeWithContentDescription("Reset").performClick()
        events shouldContainExactly listOf("query ")
    }
}
