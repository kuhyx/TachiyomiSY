package eu.kanade.presentation.more.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.more.settings.screen.stubTracker
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.Tracker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Recomposes [PreferenceItem] with the same item, an equal copy and a different item of every type, so the
 * compiler's change checks and cached callbacks inside each item composable take both arms.
 */
@RunWith(RobolectricTestRunner::class)
internal class PreferenceItemRecomposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = MapPreferenceStore()
    private var tick by mutableIntStateOf(0)
    private var current by mutableStateOf<Preference.PreferenceItem<*, *>>(text("start"))
    private var highlight by mutableStateOf<String?>(null)

    // Each entry builds a fresh (equal for the same key) item; a different key builds a different one.
    private val factories: List<(String) -> Preference.PreferenceItem<*, *>> = listOf(
        { Preference.PreferenceItem.SwitchPreference(preference = store.getBoolean(it, false), title = it) },
        { Preference.PreferenceItem.SliderPreference(value = 1, title = it, valueRange = 0..4) },
        { Preference.PreferenceItem.ListPreference(store.getString(it, "a"), mapOf("a" to "A"), it) },
        { Preference.PreferenceItem.BasicListPreference("a", mapOf("a" to "A"), it) },
        {
            val pref = store.getStringSet(it, emptySet())
            Preference.PreferenceItem.MultiSelectListPreference(pref, mapOf("a" to "A"), it)
        },
        ::text,
        { Preference.PreferenceItem.EditTextPreference(preference = store.getString(it, "v"), title = it) },
        { Preference.PreferenceItem.TrackerPreference(stubTracker<Tracker>(it), login = {}, logout = {}) },
        { Preference.PreferenceItem.InfoPreference(title = it) },
        { Preference.PreferenceItem.CustomPreference(title = it) { Text(it) } },
    )

    private fun text(title: String) = Preference.PreferenceItem.TextPreference(title = title, onClick = {})

    private fun settle() {
        compose.waitForIdle()
    }

    @Test
    fun everyTypeRecomposes() {
        compose.setContent {
            MaterialTheme {
                check(tick >= 0)
                PreferenceItem(item = current, highlightKey = highlight)
            }
        }
        settle()
        factories.forEach { build ->
            val first = build("one")
            current = first
            settle()
            tick++
            settle()
            current = build("one")
            settle()
            highlight = "one"
            settle()
            highlight = null
            current = build("two")
            settle()
        }
        compose.onNodeWithText("two").assertExists()
    }
}
