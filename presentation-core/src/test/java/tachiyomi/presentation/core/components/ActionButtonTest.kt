package tachiyomi.presentation.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ActionButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clickRunsTheAction() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { ActionButton(title = "Retry", icon = Icons.Default.Refresh, onClick = { clicks++ }) }
        }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun acceptsAModifier() {
        compose.setContent {
            MaterialTheme {
                ActionButton(
                    title = "Retry",
                    icon = Icons.Default.Refresh,
                    onClick = {},
                    modifier = Modifier.testTag("action"),
                )
            }
        }
        compose.onNodeWithTag("action").assertExists()
    }
}
