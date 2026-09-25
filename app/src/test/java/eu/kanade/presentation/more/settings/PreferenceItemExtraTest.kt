package eu.kanade.presentation.more.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PreferenceItemExtraTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = MapPreferenceStore()
    private val events = mutableListOf<String>()

    private fun show(item: Preference.PreferenceItem<*, *>) {
        compose.setContent { MaterialTheme { PreferenceItem(item = item, highlightKey = null) } }
        compose.waitForIdle()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun tracker(loggedIn: Boolean): Tracker = mockk {
        every { name } returns "Tracker X"
        every { getLogo() } returns R.drawable.ic_tachi
        every { getDisplayUsername() } returns ""
        every { isLoggedIn } returns loggedIn
        every { isLoggedInFlow } returns flowOf(loggedIn)
    }

    private fun trackerItem(loggedIn: Boolean) = Preference.PreferenceItem.TrackerPreference(
        tracker = tracker(loggedIn),
        login = { events += "login" },
        logout = { events += "logout" },
    )

    @Test
    fun multiEmptyShowsNone() {
        val pref = store.getStringSet("m", emptySet())
        show(Preference.PreferenceItem.MultiSelectListPreference(pref, mapOf("a" to "Alpha"), "Multi"))
        compose.onNodeWithText("None").assertExists()
        click("Multi")
        click("Alpha")
        click("OK")
        pref.get() shouldBe setOf("a")
        compose.onNodeWithText("Alpha").assertExists()
    }

    @Test
    fun multiVetoedNullSubtitle() {
        val pref = store.getStringSet("m", setOf("a"))
        show(
            Preference.PreferenceItem.MultiSelectListPreference(
                preference = pref,
                entries = mapOf("a" to "Alpha"),
                title = "Multi",
                subtitle = null,
                onValueChanged = { false },
            ),
        )
        click("Multi")
        click("Alpha")
        click("OK")
        pref.get() shouldBe setOf("a")
    }

    @Test
    fun trackerLogsIn() {
        show(trackerItem(loggedIn = false))
        click("Tracker X")
        events shouldBe listOf("login")
    }

    @Test
    fun trackerLogsOut() {
        show(trackerItem(loggedIn = true))
        click("Tracker X")
        events shouldBe listOf("logout")
    }

    @Test
    fun textInfoAndCustomRender() {
        compose.setContent {
            MaterialTheme {
                PreferenceItem(Preference.PreferenceItem.TextPreference(title = "Plain"), highlightKey = null)
                PreferenceItem(Preference.PreferenceItem.InfoPreference(title = "Info"), highlightKey = null)
                PreferenceItem(
                    item = Preference.PreferenceItem.CustomPreference(title = "Custom") { Text("Inside") },
                    highlightKey = null,
                )
            }
        }
        compose.onNodeWithText("Plain").assertExists()
        compose.onNodeWithText("Info").assertExists()
        compose.onNodeWithText("Inside").assertExists()
    }

    @Test
    fun disabledItemIsHidden() {
        show(Preference.PreferenceItem.TextPreference(title = "Gone", enabled = false))
        compose.onAllNodesWithText("Gone").fetchSemanticsNodes().size shouldBe 0
    }
}
