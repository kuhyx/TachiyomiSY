package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Typing, picking and removing tags in the auto-complete filter. */
@RunWith(RobolectricTestRunner::class)
internal class AutoCompleteItemTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val changes = mutableListOf<List<String>>()
    private val field get() = compose.onNode(hasSetTextAction())

    @Before
    fun setUp() {
        compose.setContent {
            MaterialTheme {
                AutoCompleteItem(
                    name = "Tags",
                    state = listOf("old"),
                    hint = "Type a tag",
                    values = listOf("female:big", "female:small", "male:tall"),
                    skipAutoFillTags = listOf("skip"),
                    validPrefixes = listOf("-", "~"),
                    onChange = { changes += it },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun type(text: String) {
        field.performTextReplacement(text)
        compose.waitForIdle()
    }

    @Test
    fun pickingASuggestionAddsIt() {
        type("sma")
        compose.waitForLabel("female:small")
        compose.onNodeWithText("female:small").performClick()
        compose.waitForIdle()
        changes shouldBe listOf(listOf("old", "female:small"))
    }

    @Test
    fun prefixIsKeptOnSuggestions() {
        type("-tal")
        compose.waitForLabel("-male:tall")
        compose.onNodeWithText("-male:tall").performClick()
        compose.waitForIdle()
        changes shouldBe listOf(listOf("old", "-male:tall"))
    }

    @Test
    fun shortTextShowsNoMenu() {
        type("ma")
        compose.waitForIdle()
        compose.hasLabel("male:tall") shouldBe false
        type("zzz")
        compose.hasLabel("male:tall") shouldBe false
    }

    @Test
    fun sendSubmitsTypedText() {
        type("custom")
        field.performImeAction()
        compose.waitForIdle()
        changes shouldBe listOf(listOf("old", "custom"))
    }

    @Test
    fun enterKeySubmitsToo() {
        type("keyed")
        field.performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        changes shouldBe listOf(listOf("old", "keyed"))
    }

    @Test
    fun skippedTagsAreRefused() {
        type("skip")
        field.performImeAction()
        type("~ skip")
        field.performImeAction()
        compose.waitForIdle()
        changes shouldBe emptyList()
        compose.hasLabel("~ skip") shouldBe true
    }

    @Test
    fun chipRemovesItsTag() {
        compose.onNodeWithContentDescription("old").performClick()
        compose.waitForIdle()
        changes shouldBe listOf(emptyList())
    }

    @Test
    fun backCollapsesMenu() {
        field.performClick()
        field.performTextInput("big")
        compose.waitForLabel("female:big")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.hasLabel("female:big") shouldBe false
    }

    @Test
    fun backClosesSuggestions() {
        type("sma")
        compose.waitForLabel("female:small")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onAllNodes(hasText("female:small")).fetchSemanticsNodes().isEmpty() shouldBe true
    }
}
