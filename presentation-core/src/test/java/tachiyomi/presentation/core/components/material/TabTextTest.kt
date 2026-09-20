package tachiyomi.presentation.core.components.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TabTextTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withoutBadge() {
        compose.setContent {
            MaterialTheme { TabText(text = "Library") }
        }
        compose.onNodeWithText("Library").assertIsDisplayed()
        compose.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun withBadge() {
        compose.setContent {
            MaterialTheme { TabText(text = "Library", badgeCount = 3) }
        }
        compose.onNodeWithText("Library").assertIsDisplayed()
        compose.onNodeWithText("3").assertIsDisplayed()
    }
}
