package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Composable
private fun SliderHost(value: Int, onChange: (Int) -> Unit) {
    BaseSliderItem(value = value, valueRange = 0..10, title = "Title", onChange = onChange)
}

@RunWith(RobolectricTestRunner::class)
internal class SettingsSliderItemsTest {
    @get:Rule
    val compose = createComposeRule()

    private var value by mutableIntStateOf(5)
    private var changes = 0

    private fun setProgress(target: Float) {
        compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { setter -> setter(target) }
        compose.waitForIdle()
    }

    private fun onChange(next: Int) {
        changes++
        value = next
    }

    @Test
    fun sliderItemDefaults() {
        compose.setContent {
            MaterialTheme { SliderItem(value = value, valueRange = 0..10, label = "Rows", onChange = ::onChange) }
        }
        compose.onNodeWithText("Rows").assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun sliderItemAllOptions() {
        compose.setContent {
            MaterialTheme {
                SliderItem(
                    value = value,
                    valueRange = 0..10,
                    label = "Rows",
                    onChange = ::onChange,
                    steps = 4,
                    valueString = "five",
                    labelStyle = MaterialTheme.typography.titleMedium,
                    pillColor = Color.Red,
                )
            }
        }
        compose.onNodeWithText("five").assertIsDisplayed()
    }

    @Test
    fun steppedChangeReportsNewValue() {
        compose.setContent {
            MaterialTheme { SliderItem(value = value, valueRange = 0..10, label = "Rows", onChange = ::onChange) }
        }
        setProgress(8f)
        compose.runOnIdle {
            value shouldBe 8
            changes shouldBe 1
        }
        compose.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun sameRoundedValueIsIgnored() {
        compose.setContent {
            MaterialTheme {
                SliderItem(value = value, valueRange = 0..10, label = "Rows", onChange = ::onChange, steps = 0)
            }
        }
        // A continuous slider reports 5.2, which rounds back to the current 5: no callback.
        setProgress(5.2f)
        compose.runOnIdle {
            value shouldBe 5
            changes shouldBe 0
        }
    }

    @Test
    fun baseSliderItemMinimal() {
        compose.setContent {
            MaterialTheme { BaseSliderItem(value = value, valueRange = 0..10, title = "Title", onChange = ::onChange) }
        }
        compose.onNodeWithText("Title").assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed()
        setProgress(2f)
        compose.runOnIdle { value shouldBe 2 }
    }

    @Test
    fun baseSliderItemSubtitleDefault() {
        compose.setContent {
            MaterialTheme {
                BaseSliderItem(
                    value = value,
                    valueRange = 0..10,
                    title = "Title",
                    onChange = ::onChange,
                    subtitle = "Subtitle",
                )
            }
        }
        compose.onNodeWithText("Subtitle").assertIsDisplayed()
    }

    @Test
    fun baseSliderItemAllOptions() {
        compose.setContent {
            MaterialTheme {
                BaseSliderItem(
                    value = value,
                    valueRange = 0..10,
                    title = "Title",
                    onChange = ::onChange,
                    modifier = Modifier.testTag("slider"),
                    subtitle = "Subtitle",
                    steps = 4,
                    valueString = "custom",
                    titleStyle = MaterialTheme.typography.titleSmall,
                    subtitleStyle = MaterialTheme.typography.labelSmall,
                    pillColor = Color.Blue,
                )
            }
        }
        compose.onNodeWithTag("slider").assertExists()
        compose.onNodeWithText("Subtitle").assertIsDisplayed()
        compose.onNodeWithText("custom").assertIsDisplayed()
    }

    @Test
    fun changedInputsRebuildOnChange() {
        var handler by mutableStateOf<(Int) -> Unit>(::onChange)
        compose.setContent { MaterialTheme { SliderHost(value = value, onChange = handler) } }
        compose.waitForIdle()
        value = 6
        compose.waitForIdle()
        var latest = -1
        handler = { next -> latest = next }
        compose.waitForIdle()
        setProgress(9f)
        compose.runOnIdle {
            latest shouldBe 9
            changes shouldBe 0
        }
    }

    @Test
    @Config(qualifiers = "notnight")
    fun previewInLightTheme() {
        compose.setContent { SliderItemPreview() }
        compose.onNodeWithText("Auto").assertIsDisplayed()
        setProgress(3f)
        compose.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "night")
    fun previewInDarkTheme() {
        compose.setContent { SliderItemPreview() }
        compose.onNodeWithText("Auto").assertIsDisplayed()
    }
}
