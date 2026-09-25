package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BrowseSmallComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun fabLabelFollowsVisibility() {
        compose.setContent {
            MaterialTheme {
                Column {
                    BrowseSourceFab(isVisible = true, onFabClick = { events += "fab" })
                    BrowseSourceFab(isVisible = false, onFabClick = {}, modifier = Modifier)
                }
            }
        }
        compose.onNodeWithText("Saved Searches").assertExists()
        compose.onNodeWithText("Filter").performClick()
        events shouldContainExactly listOf("fab")
    }

    @Test
    fun badgesAndLoadingItem() {
        compose.setContent {
            MaterialTheme {
                Column {
                    androidx.compose.foundation.layout.Row {
                        InLibraryBadge(enabled = true)
                        InLibraryBadge(enabled = false)
                    }
                    BrowseSourceLoadingItem()
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun baseItemDefaultsAndCallbacks() {
        compose.setContent {
            MaterialTheme {
                Column {
                    BaseBrowseItem()
                    BaseBrowseItem(
                        modifier = Modifier,
                        onClickItem = { events += "click" },
                        onLongClickItem = { events += "long" },
                        icon = { Text("icon") },
                        action = { Text("action") },
                        content = { Text("content") },
                    )
                }
            }
        }
        compose.onNodeWithText("content").performClick()
        compose.onNodeWithText("content").performTouchInput { longClick() }
        events shouldContainExactly listOf("click", "long")
    }
}
