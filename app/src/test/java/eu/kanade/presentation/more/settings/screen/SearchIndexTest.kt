package eu.kanade.presentation.more.settings.screen

import eu.kanade.presentation.more.settings.Preference
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SearchIndexTest {
    private val contents = listOf(
        text("Loose item"),
        text("", subtitle = "blank title"),
        text("Hidden loose", enabled = false),
        Preference.PreferenceItem.InfoPreference("Info about things"),
        Preference.PreferenceGroup(
            title = "Group",
            preferenceItems = listOf(text("Inner item", subtitle = "has summary"), text("Off item", enabled = false)),
        ),
        Preference.PreferenceGroup(title = "Disabled group", enabled = false, preferenceItems = listOf(text("x"))),
    )

    private fun text(title: String, subtitle: String? = null, enabled: Boolean = true) =
        Preference.PreferenceItem.TextPreference(title = title, subtitle = subtitle, enabled = enabled)

    @Test
    fun entriesSkipHiddenAndInfo() {
        searchableEntries(contents).map { it.first to it.second.title }.toList() shouldBe listOf(
            null to "Loose item",
            "Group" to "Inner item",
        )
    }

    @Test
    fun searchMatchesTitleAndSummary() {
        val index = listOf(SettingsData(title = "Screen", route = SettingsReaderScreen, contents = contents))
        searchIndex(index, "item", isLtr = true).map { it.breadcrumbs } shouldBe listOf("Screen", "Screen > Group")
        searchIndex(index, "SUMMARY", isLtr = false).single().breadcrumbs shouldBe "Group < Screen"
        searchIndex(index, "nothing", isLtr = true) shouldBe emptyList()
    }

    @Test
    fun resultsAreCapped() {
        val many = List(15) { text("Row $it") }
        val index = listOf(SettingsData(title = "S", route = SettingsReaderScreen, contents = many))
        searchIndex(index, "row", isLtr = true).size shouldBe 10
    }

    @Test
    fun breadcrumbs() {
        getLocalizedBreadcrumb(path = "A", node = null, isLtr = true) shouldBe "A"
        getLocalizedBreadcrumb(path = "A", node = "B", isLtr = true) shouldBe "A > B"
        getLocalizedBreadcrumb(path = "A", node = "B", isLtr = false) shouldBe "B < A"
    }
}
