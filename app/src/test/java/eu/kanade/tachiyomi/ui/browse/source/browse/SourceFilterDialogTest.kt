package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.source.model.EXHSavedSearch

/** Every kind of filter in one sheet, with its header buttons, the MangaDex row and the saved searches. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h3000dp")
internal class SourceFilterDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val random: () -> Unit = { events += "random" }
    private val follows: () -> Unit = { events += "follows" }
    private val saved = EXHSavedSearch(id = 1L, name = "Kept", query = "q", filterList = null)

    private fun everyKind(): FilterList = FilterList(
        object : Filter.AutoComplete("Tags", "hint", listOf("alpha"), state = listOf("beta")) {},
        object : Filter.Header("Heading") {},
        object : Filter.Separator() {},
        object : Filter.CheckBox("Check") {},
        object : Filter.TriState("Tri") {},
        object : Filter.Text("Words") {},
        object : Filter.Select<String>("Pick", arrayOf("One", "Two")) {},
        object : Filter.Sort("Order", arrayOf("Name", "Date")) {},
        object : Filter.Group<Any>("Nested", listOf(object : Filter.CheckBox("Inner") {}, "not a filter")) {},
    )

    private fun show(
        filters: FilterList = everyKind(),
        startExpanded: Boolean = true,
        searches: List<EXHSavedSearch> = listOf(saved),
        mangaDex: Boolean = true,
        followsOnly: Boolean = false,
        randomOnly: Boolean = false,
    ) {
        compose.setContent {
            MaterialTheme {
                SourceFilterDialog(
                    onDismissRequest = { events += "dismiss" },
                    filters = filters,
                    onReset = { events += "reset" },
                    onFilter = { events += "filter" },
                    onUpdate = { events += "update" },
                    startExpanded = startExpanded,
                    savedSearches = searches,
                    onSave = { events += "save" },
                    onSavedSearch = { events += "search:${it.name}" },
                    onSavedSearchPress = { events += "press:${it.name}" },
                    openMangaDexRandom = random.takeIf { mangaDex && !followsOnly },
                    openMangaDexFollows = follows.takeIf { mangaDex && !randomOnly },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun headerButtonsReport() {
        show()
        tap("Reset")
        compose.onNodeWithContentDescription("Save").performClick()
        tap("Filter")
        events shouldBe listOf("reset", "save", "filter", "dismiss")
    }

    @Test
    fun everyKindIsShown() {
        show()
        listOf("Tags", "Heading", "Check", "Tri", "Words", "Pick", "Order", "Nested", "Inner").forEach {
            compose.hasLabel(it) shouldBe true
        }
    }

    @Test
    fun leavesReportUpdates() {
        show()
        tap("Check")
        tap("Inner")
        compose.onNodeWithContentDescription("beta").performClick()
        compose.waitForIdle()
        events shouldBe listOf("update", "update", "update")
    }

    @Test
    fun mangaDexRowOpensBoth() {
        show()
        tap("Random")
        tap("MangaDex follows")
        events shouldBe listOf("random", "follows")
    }

    @Test
    fun mangaDexRowNeedsBoth() {
        show(followsOnly = true)
        compose.hasLabel("MangaDex follows") shouldBe false
    }

    @Test
    fun randomAloneIsNotEnough() {
        show(randomOnly = true)
        compose.hasLabel("Random") shouldBe false
    }

    @Test
    fun savedSearchChipsReport() {
        show(mangaDex = false)
        compose.hasLabel("Random") shouldBe false
        tap("Kept")
        compose.onNodeWithText("Kept").performTouchInput { longClick() }
        compose.waitForIdle()
        events shouldBe listOf("search:Kept", "press:Kept")
    }

    @Test
    fun noSavedSearchesNoRow() {
        show(searches = emptyList(), startExpanded = false)
        compose.hasLabel("Saved Searches") shouldBe false
        compose.hasLabel("Inner") shouldBe false
    }
}
