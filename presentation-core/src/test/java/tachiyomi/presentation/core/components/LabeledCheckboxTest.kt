package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Composable
private fun LabeledCheckboxHost(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    LabeledCheckbox(label = "Option", checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
}

@RunWith(RobolectricTestRunner::class)
internal class LabeledCheckboxTest {
    @get:Rule
    val compose = createComposeRule()

    private var received: Boolean? = null

    @Test
    fun uncheckedRowChecksOnClick() {
        compose.setContent {
            MaterialTheme {
                LabeledCheckbox(label = "Option", checked = false, onCheckedChange = { next -> received = next })
            }
        }
        compose.onNodeWithText("Option").performClick()
        compose.runOnIdle { received shouldBe true }
    }

    @Test
    fun checkedRowUnchecksOnClick() {
        compose.setContent {
            MaterialTheme {
                LabeledCheckbox(
                    label = "Option",
                    checked = true,
                    onCheckedChange = { next -> received = next },
                    modifier = Modifier.testTag("row"),
                    enabled = true,
                )
            }
        }
        compose.onNodeWithTag("row").performClick()
        compose.runOnIdle { received shouldBe false }
    }

    @Test
    fun disabledRowIgnoresClicks() {
        compose.setContent {
            MaterialTheme {
                LabeledCheckbox(
                    label = "Option",
                    checked = false,
                    onCheckedChange = { next -> received = next },
                    enabled = false,
                )
            }
        }
        compose.onNodeWithText("Option").performClick()
        compose.runOnIdle { received.shouldBeNull() }
    }

    @Test
    fun changedInputsRebuildTheClick() {
        var checked by mutableStateOf(false)
        var enabled by mutableStateOf(false)
        var handler by mutableStateOf<(Boolean) -> Unit>({ next -> received = next })
        compose.setContent {
            MaterialTheme { LabeledCheckboxHost(checked = checked, enabled = enabled, onCheckedChange = handler) }
        }
        compose.waitForIdle()
        checked = true
        compose.waitForIdle()
        enabled = true
        compose.waitForIdle()
        var latest: Boolean? = null
        handler = { next -> latest = next }
        compose.waitForIdle()
        compose.onNodeWithText("Option").performClick()
        compose.runOnIdle {
            latest shouldBe false
            received.shouldBeNull()
        }
    }
}
