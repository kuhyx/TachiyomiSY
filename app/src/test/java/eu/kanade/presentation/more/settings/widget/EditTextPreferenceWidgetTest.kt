package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EditTextPreferenceWidgetTest {
    @get:Rule
    val compose = createComposeRule()

    private val confirmed = mutableListOf<String>()

    private fun show(accept: Boolean) {
        compose.setContent {
            MaterialTheme {
                EditTextPreferenceWidget(
                    title = "Name",
                    subtitle = "Now %s",
                    icon = null,
                    value = "old",
                    onConfirm = {
                        confirmed += it
                        accept
                    },
                    content = { Text("Trailing") },
                )
            }
        }
        compose.onNodeWithText("Now old").performClick()
        compose.waitForIdle()
    }

    private fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun acceptedValueClosesDialog() {
        show(accept = true)
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("new")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        confirmed shouldBe listOf("new")
        count("OK") shouldBe 0
    }

    @Test
    fun rejectedValueKeepsDialog() {
        show(accept = false)
        compose.onNode(hasSetTextAction()).performTextReplacement("new")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        confirmed shouldBe listOf("new")
        count("OK") shouldBe 1
    }

    @Test
    fun clearIconBlanksField() {
        show(accept = true)
        compose.onNode(hasClickAction() and hasAnyAncestor(hasSetTextAction())).performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assert(hasText(""))
        compose.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun cancelAndBackClose() {
        show(accept = true)
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        count("Cancel") shouldBe 0
        compose.onNodeWithText("Now old").performClick()
        compose.waitForIdle()
        Espresso.pressBackUnconditionally()
        compose.waitForIdle()
        count("Cancel") shouldBe 0
    }
}
