package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ListGroupHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheText() {
        compose.setContent { MaterialTheme { ListGroupHeader(text = "Group") } }
        compose.onNodeWithText("Group").assertIsDisplayed()
    }

    @Test
    fun acceptsAModifier() {
        compose.setContent { MaterialTheme { ListGroupHeader(text = "Group", modifier = Modifier.testTag("header")) } }
        compose.onNodeWithTag("header").assertIsDisplayed()
    }
}
