package eu.kanade.presentation.more.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class PreferenceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        SearchableSettings.highlightKey = null
    }

    private fun text(title: String) = Preference.PreferenceItem.TextPreference(title = title)

    private val items = listOf(
        text("Loose"),
        Preference.PreferenceGroup(title = "Hidden group", enabled = false, preferenceItems = listOf(text("Nope"))),
        Preference.PreferenceGroup(title = "First group", preferenceItems = (1..30).map { text("Row $it") }),
        Preference.PreferenceGroup(title = "Last group", preferenceItems = listOf(text("Tail"))),
    )

    private fun show(key: String?) {
        SearchableSettings.highlightKey = key
        compose.setContent { MaterialTheme { PreferenceScreen(items = items) } }
        compose.waitForIdle()
    }

    private fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun rendersGroupsAndItems() {
        show(key = null)
        compose.onNodeWithText("Loose").assertExists()
        compose.onNodeWithText("First group").assertExists()
        count("Hidden group") shouldBe 0
    }

    @Test
    fun scrollsToHighlightedItem() {
        show(key = "Tail")
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithText("Tail").assertExists()
        SearchableSettings.highlightKey.shouldBeNull()
    }

    @Test
    fun unknownKeyIsForgotten() {
        show(key = "Missing")
        compose.waitForIdle()
        SearchableSettings.highlightKey.shouldBeNull()
        compose.onNodeWithText("Loose").assertExists()
    }

    @Test
    fun scaffoldShowsTitleAndBack() {
        var backs = 0
        compose.setContent {
            MaterialTheme {
                PreferenceScaffold(
                    titleRes = MR.strings.label_settings,
                    onBackPressed = { backs++ },
                    itemsProvider = { listOf(text("Inside")) },
                )
            }
        }
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithText("Inside").assertExists()
        backs shouldBe 0
    }

    @Test
    fun scaffoldDefaults() {
        compose.setContent {
            MaterialTheme { PreferenceScaffold(titleRes = MR.strings.label_settings, itemsProvider = { emptyList() }) }
        }
        compose.onNodeWithText("Settings").assertExists()
    }
}
