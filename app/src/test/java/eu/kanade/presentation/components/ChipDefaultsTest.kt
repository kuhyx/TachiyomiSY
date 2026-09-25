package eu.kanade.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChipDefaultsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaultsAndExplicitValuesAgree() {
        val seen = mutableListOf<Any>()
        compose.setContent {
            MaterialTheme {
                seen += SuggestionChipDefaults.suggestionChipColors()
                seen += SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.Red,
                    labelColor = Color.Red,
                    iconContentColor = Color.Red,
                    disabledContainerColor = Color.Red,
                    disabledLabelColor = Color.Red,
                    disabledIconContentColor = Color.Red,
                )
                seen += SuggestionChipDefaults.elevatedSuggestionChipColors()
                seen += SuggestionChipDefaults.elevatedSuggestionChipColors(
                    containerColor = Color.Red,
                    labelColor = Color.Red,
                    iconContentColor = Color.Red,
                    disabledContainerColor = Color.Red,
                    disabledLabelColor = Color.Red,
                    disabledIconContentColor = Color.Red,
                )
                seen += SuggestionChipDefaults.suggestionChipElevation()
                seen += SuggestionChipDefaults.suggestionChipElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 2.dp,
                    focusedElevation = 3.dp,
                    hoveredElevation = 4.dp,
                    draggedElevation = 5.dp,
                    disabledElevation = 6.dp,
                )
                seen += SuggestionChipDefaults.elevatedChipElevation()
                seen += SuggestionChipDefaults.elevatedChipElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 2.dp,
                    focusedElevation = 3.dp,
                    hoveredElevation = 4.dp,
                    draggedElevation = 5.dp,
                    disabledElevation = 6.dp,
                )
                seen += SuggestionChipDefaults.suggestionChipBorder()
                seen += SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = Color.Red,
                    disabledBorderColor = Color.Red,
                    borderWidth = 2.dp,
                )
            }
        }
        compose.waitForIdle()
        seen.size shouldBe 10
        seen[5] shouldBe seen[7]
        seen[0] shouldNotBe seen[1]
    }
}
