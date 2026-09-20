package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PullRefreshTest {
    @get:Rule
    val compose = createComposeRule()

    private var refreshes = 0

    @Test
    fun idleWithDefaults() {
        compose.setContent {
            MaterialTheme {
                PullRefresh(refreshing = false, enabled = true, onRefresh = { refreshes += 1 }) {
                    Text("list")
                }
            }
        }
        compose.onNodeWithText("list").assertIsDisplayed()
        refreshes shouldBe 0
    }

    @Test
    fun refreshingWithEveryParameter() {
        compose.setContent {
            MaterialTheme {
                PullRefresh(
                    refreshing = true,
                    enabled = false,
                    onRefresh = { refreshes += 1 },
                    modifier = Modifier.testTag("refresh"),
                    indicatorPadding = PaddingValues(top = 12.dp),
                ) {
                    Text("list")
                }
            }
        }
        compose.onNodeWithTag("refresh").assertIsDisplayed()
        compose.onNodeWithText("list").assertIsDisplayed()
        refreshes shouldBe 0
    }
}
