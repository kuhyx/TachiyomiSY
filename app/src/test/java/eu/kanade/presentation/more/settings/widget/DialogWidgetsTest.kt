package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DialogWidgetsTest {
    @get:Rule
    val compose = createComposeRule()

    private var picked: Set<String>? = null

    private fun showMulti() {
        compose.setContent {
            MaterialTheme {
                MultiSelectListPrefWidget(
                    values = setOf("a"),
                    title = "Many",
                    subtitle = null,
                    icon = null,
                    entries = mapOf("a" to "Alpha", "b" to "Beta"),
                    onValuesChange = { picked = it },
                )
            }
        }
        compose.onNodeWithText("Many").performClick()
        compose.waitForIdle()
    }

    private fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun multiTogglesAndConfirms() {
        showMulti()
        compose.onNodeWithText("Alpha").performClick()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        picked shouldBe setOf("b")
        count("Beta") shouldBe 0
    }

    @Test
    fun multiCancelKeepsValues() {
        showMulti()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        picked shouldBe null
        count("Alpha") shouldBe 0
    }

    @Test
    fun multiBackDismisses() {
        showMulti()
        Espresso.pressBackUnconditionally()
        compose.waitForIdle()
        count("Alpha") shouldBe 0
    }
}
