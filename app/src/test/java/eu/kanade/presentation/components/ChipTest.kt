package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChipTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun plainChipDefaults() {
        compose.setContent { MaterialTheme { SuggestionChip(label = { Text("plain") }) } }
        compose.onNodeWithText("plain").assertExists()
    }

    @Test
    fun plainChipWithoutDecorations() {
        compose.setContent {
            MaterialTheme {
                SuggestionChip(
                    label = { Text("bare") },
                    modifier = Modifier,
                    enabled = false,
                    icon = { Text("icon") },
                    interactionSource = MutableInteractionSource(),
                    elevation = null,
                    shape = RectangleShape,
                    border = null,
                    colors = SuggestionChipDefaults.elevatedSuggestionChipColors(),
                )
            }
        }
        compose.onNodeWithText("icon").assertExists()
    }

    @Test
    fun clickableChipForwardsClicks() {
        compose.setContent {
            MaterialTheme {
                SuggestionChip(
                    onClick = { events += "click" },
                    onLongClick = { events += "long" },
                    label = { Text("tap") },
                )
            }
        }
        compose.onNodeWithText("tap").performClick()
        compose.onNodeWithText("tap").performTouchInput { longClick() }
        events shouldContainExactly listOf("click", "long")
    }

    @Test
    fun clickableChipUndecorated() {
        compose.setContent {
            MaterialTheme {
                SuggestionChip(
                    onClick = {},
                    onLongClick = {},
                    label = { Text("off") },
                    modifier = Modifier,
                    enabled = false,
                    icon = { Text("lead") },
                    interactionSource = MutableInteractionSource(),
                    elevation = null,
                    shape = RectangleShape,
                    border = null,
                    colors = SuggestionChipDefaults.suggestionChipColors(),
                )
            }
        }
        compose.onNodeWithText("lead").assertExists()
    }

    @Test
    fun contentPrefersTheAvatar() {
        compose.setContent {
            MaterialTheme {
                ChipContent(
                    label = { Text("label") },
                    labelTextStyle = Typography().labelLarge,
                    labelColor = Color.Black,
                    leadingIcon = { Text("leading") },
                    avatar = { Text("avatar") },
                    trailingIcon = { Text("trailing") },
                    leadingIconColor = Color.Black,
                    trailingIconColor = Color.Black,
                    minHeight = 32.dp,
                    paddingValues = PaddingValues(),
                )
            }
        }
        compose.onNodeWithText("avatar").assertExists()
        compose.onNodeWithText("leading").assertDoesNotExist()
        compose.onNodeWithText("trailing").assertExists()
    }

    @Test
    fun contentWithoutIcons() {
        compose.setContent {
            MaterialTheme {
                ChipContent(
                    label = { Text("alone") },
                    labelTextStyle = Typography().labelLarge,
                    labelColor = Color.Black,
                    leadingIcon = null,
                    avatar = null,
                    trailingIcon = null,
                    leadingIconColor = Color.Black,
                    trailingIconColor = Color.Black,
                    minHeight = 32.dp,
                    paddingValues = PaddingValues(),
                )
            }
        }
        compose.onNodeWithText("alone").assertExists()
    }
}
