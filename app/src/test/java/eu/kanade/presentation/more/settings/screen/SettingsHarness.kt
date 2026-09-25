package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceItem
import eu.kanade.presentation.more.settings.internalOnValueChanged
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Composes one settings screen's [SearchableSettings.getPreferences] with a relaxed [Navigator], renders
 * every item eagerly (a plain column, so no item is left out by laziness) and hands the list back so a
 * test can drive each callback directly.
 */
internal class SettingsHarness(private val compose: ComposeContentTestRule) {
    val navigator: Navigator = mockk(relaxed = true)
    val registry: FakeResultRegistry = FakeResultRegistry()
    val uriHandler: UriHandler = mockk(relaxed = true)
    var prefs: List<Preference> = emptyList()
        private set

    fun show(screen: SearchableSettings, context: Context? = null) {
        compose.setContent {
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalContext provides (context ?: LocalContext.current),
                LocalActivityResultRegistryOwner provides registry.owner(),
                LocalUriHandler provides uriHandler,
            ) {
                MaterialTheme {
                    val current = screen.getPreferences()
                    prefs = current
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        items(current).forEach { PreferenceItem(item = it, highlightKey = null) }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    fun items(from: List<Preference> = prefs): List<Preference.PreferenceItem<*, *>> = from.flatMap {
        when (it) {
            is Preference.PreferenceGroup -> it.preferenceItems
            is Preference.PreferenceItem<*, *> -> listOf(it)
        }
    }

    /** The item titled [title], inside the group titled [group] when titles repeat across groups. */
    fun item(title: String, group: String? = null): Preference.PreferenceItem<*, *> {
        val groups = prefs.filterIsInstance<Preference.PreferenceGroup>()
        val scope = group?.let { name -> groups.first { it.title == name } }
        return items(scope?.let(::listOf) ?: prefs).first { it.title == title }
    }

    fun click(title: String, group: String? = null) {
        val text = item(title, group) as Preference.PreferenceItem.TextPreference
        compose.runOnIdle { text.onClick!!.invoke() }
        compose.waitForIdle()
    }

    fun switch(title: String, value: Boolean, group: String? = null): Boolean {
        val item = item(title, group) as Preference.PreferenceItem.SwitchPreference
        return settle { item.onValueChanged(value) }
    }

    fun list(title: String, value: Any, group: String? = null): Boolean {
        val item = item(title, group) as Preference.PreferenceItem.ListPreference<*>
        return settle { item.internalOnValueChanged(value) }
    }

    fun multi(title: String, value: Set<Any?>): Boolean {
        val item = item(title) as Preference.PreferenceItem.MultiSelectListPreference<*>
        return settle { item.internalOnValueChanged(value) }
    }

    fun slide(title: String, value: Int, group: String? = null) {
        val item = item(title, group) as Preference.PreferenceItem.SliderPreference
        settle { item.onValueChanged(value) }
    }

    fun edit(title: String, value: String): Boolean {
        val item = item(title) as Preference.PreferenceItem.EditTextPreference
        return settle { item.onValueChanged(value) }
    }

    fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun <R> settle(block: suspend () -> R): R {
        val result = compose.runOnIdle { runBlocking { block() } }
        compose.waitForIdle()
        return result
    }
}
