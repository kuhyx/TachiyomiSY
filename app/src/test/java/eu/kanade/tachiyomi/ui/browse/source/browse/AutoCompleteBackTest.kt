package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Back on an expanded field is consumed before it reaches the activity, and the bare field without a label. */
@RunWith(RobolectricTestRunner::class)
internal class AutoCompleteBackTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun firstBackStaysOnScreen() {
        compose.setContent {
            MaterialTheme {
                AutoCompleteItem("Tags", emptyList(), "hint", listOf("male:tall"), emptyList(), emptyList()) {}
            }
        }
        // Two letters expand the field without offering suggestions yet.
        compose.onNode(hasSetTextAction()).performTextReplacement("ma")
        compose.waitForIdle()
        back()
        compose.activity.isFinishing shouldBe false
        back()
        compose.activity.isFinishing shouldBe true
    }

    @Test
    fun bareFieldSubmits() {
        val submitted = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                AutoCompleteTextField(
                    values = listOf("tall"),
                    onValueFilter = { text -> Pair({ it.contains(text) }, null) },
                    onSubmit = {
                        submitted += it
                        true
                    },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("typed")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        submitted shouldBe listOf("typed")
    }
}
