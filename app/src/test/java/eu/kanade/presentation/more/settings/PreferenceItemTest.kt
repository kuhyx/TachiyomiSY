package eu.kanade.presentation.more.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PreferenceItemTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = MapPreferenceStore()

    private fun show(item: Preference.PreferenceItem<*, *>, highlightKey: String? = null) {
        compose.setContent { MaterialTheme { PreferenceItem(item = item, highlightKey = highlightKey) } }
        compose.waitForIdle()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun switchStoresWhenAccepted() {
        val pref = store.getBoolean("s", false)
        show(Preference.PreferenceItem.SwitchPreference(preference = pref, title = "Sw"), highlightKey = "Sw")
        click("Sw")
        pref.get() shouldBe true
    }

    @Test
    fun switchKeepsWhenVetoed() {
        val pref = store.getBoolean("s", false)
        show(Preference.PreferenceItem.SwitchPreference(preference = pref, title = "Sw", onValueChanged = { false }))
        click("Sw")
        pref.get() shouldBe false
    }

    @Test
    fun sliderReportsChange() {
        val seen = mutableListOf<Int>()
        show(
            Preference.PreferenceItem.SliderPreference(
                value = 1,
                title = "Slide",
                valueString = "one",
                valueRange = 0..4,
                onValueChanged = { seen += it },
            ),
        )
        compose.onNodeWithText("one").assertExists()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        compose.waitForIdle()
        seen shouldBe listOf(3)
    }

    @Test
    fun sliderFallsBackToValue() {
        show(Preference.PreferenceItem.SliderPreference(value = 1, title = "Slide", valueString = ""))
        compose.onNodeWithText("1").assertExists()
    }

    @Test
    fun listStoresWhenAccepted() {
        val pref = store.getString("l", "a")
        show(Preference.PreferenceItem.ListPreference(pref, mapOf("a" to "Alpha", "b" to "Beta"), "List"))
        compose.onNodeWithText("Alpha").assertExists()
        click("List")
        click("Beta")
        pref.get() shouldBe "b"
    }

    @Test
    fun listKeepsWhenVetoed() {
        val pref = store.getString("l", "a")
        show(
            Preference.PreferenceItem.ListPreference(
                preference = pref,
                entries = mapOf("a" to "Alpha", "b" to "Beta"),
                title = "List",
                subtitle = null,
                onValueChanged = { false },
            ),
        )
        click("List")
        click("Beta")
        pref.get() shouldBe "a"
    }

    @Test
    fun basicListReportsChange() {
        val seen = mutableListOf<String>()
        show(
            Preference.PreferenceItem.BasicListPreference(
                value = "a",
                entries = mapOf("a" to "Alpha", "b" to "Beta"),
                title = "Basic",
                onValueChanged = { seen += it },
            ),
        )
        click("Basic")
        click("Beta")
        seen shouldBe listOf("b")
    }

    @Test
    fun basicListNullSubtitle() {
        show(
            Preference.PreferenceItem.BasicListPreference(
                value = "a",
                entries = mapOf("a" to "Alpha"),
                title = "Basic",
                subtitle = null,
            ),
        )
        click("Basic")
        click("Cancel")
    }

    @Test
    fun editTextStoresWhenAccepted() {
        val pref = store.getString("e", "old")
        show(Preference.PreferenceItem.EditTextPreference(preference = pref, title = "Edit"))
        click("Edit")
        compose.onNode(hasSetTextAction()).performTextReplacement("new")
        click("OK")
        pref.get() shouldBe "new"
    }

    @Test
    fun editTextKeepsWhenVetoed() {
        val pref = store.getString("e", "old")
        show(Preference.PreferenceItem.EditTextPreference(pref, "Edit", onValueChanged = { false }))
        click("Edit")
        compose.onNode(hasSetTextAction()).performTextReplacement("new")
        click("OK")
        pref.get() shouldBe "old"
    }
}
