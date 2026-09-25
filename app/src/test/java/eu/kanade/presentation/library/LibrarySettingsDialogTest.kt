package eu.kanade.presentation.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup

@RunWith(RobolectricTestRunner::class)
internal class LibrarySettingsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(harness: LibrarySettingsHarness, category: Category? = null, hasCategories: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                LibrarySettingsDialog(
                    onDismissRequest = {},
                    screenModel = harness.model,
                    category = category,
                    hasCategories = hasCategories,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun tab(title: String) {
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    @Test
    fun filterPageTogglesPreferences() {
        val harness = LibrarySettingsHarness(trackerCount = 1)
        show(harness)
        listOf("Downloaded", "Unread", "Started", "Bookmarked", "Completed", "Lewd", "Tracked")
            .forEach { compose.onNodeWithText(it).performClick() }
        compose.waitForIdle()
        harness.libraryPreferences.filterDownloaded.get() shouldBe TriState.ENABLED_IS
        harness.libraryPreferences.filterLewd.get() shouldBe TriState.ENABLED_IS
        harness.libraryPreferences.filterTracking(1).get() shouldBe TriState.ENABLED_IS
    }

    @Test
    fun downloadedOnlyLocksTheFilter() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        harness.model.preferences.downloadedOnly.set(true)
        harness.libraryPreferences.autoUpdateMangaRestrictions.set(emptySet())
        show(harness)
        compose.onNodeWithText("Tracked").assertDoesNotExist()
        compose.onNodeWithText("Customized update frequency").assertDoesNotExist()
    }

    @Test
    fun severalTrackersGetAHeading() {
        val harness = LibrarySettingsHarness(trackerCount = 2)
        show(harness)
        compose.onNodeWithText("Customized update frequency").performClick()
        compose.onNodeWithText("Tracker 2").performClick()
        compose.waitForIdle()
        harness.libraryPreferences.filterTracking(2).get() shouldBe TriState.ENABLED_IS
    }

    @Test
    fun sortPageUsesTheCategorySort() {
        val harness = LibrarySettingsHarness(trackerCount = 1)
        show(harness, category = Category(id = 1L, name = "c", order = 0L, flags = 0L))
        tab("Sort")
        compose.onNodeWithText("Alphabetically").performClick()
        compose.onNodeWithText("Tracker score").performClick()
        compose.onNodeWithText("Random").performClick()
        compose.waitForIdle()
        harness.sorts.size shouldBe 3
    }

    @Test
    fun sortPageGlobalWhenGrouped() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        harness.libraryPreferences.groupLibraryBy.set(LibraryGroup.BY_SOURCE)
        harness.libraryPreferences.sortTagsForLibrary.set(setOf("tag"))
        show(harness)
        tab("Sort")
        compose.onNodeWithText("Tag sorting").performClick()
        compose.onNodeWithText("Tracker score").assertDoesNotExist()
        compose.waitForIdle()
        harness.sorts.size shouldBe 1
    }

    @Test
    fun displayPageSetsTheMode() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        show(harness)
        tab("Display")
        compose.onNodeWithText("Items per row").assertExists()
        compose.onNodeWithText("List").performClick()
        compose.waitForIdle()
        harness.libraryPreferences.displayMode.get() shouldBe LibraryDisplayMode.List
        compose.onNodeWithText("Items per row").assertDoesNotExist()
        compose.onNodeWithText("Show category tabs").performClick()
    }

    @Test
    fun groupPageSetsTheGrouping() {
        val harness = LibrarySettingsHarness(trackerCount = 1)
        show(harness)
        tab("Group")
        compose.onNodeWithText("Tracking status").assertExists()
        compose.onNodeWithText("Ungrouped").performClick()
        compose.onNodeWithText("Sources").performClick()
        compose.mainClock.advanceTimeBy(500L)
        compose.waitForIdle()
        val grouping = harness.libraryPreferences.groupLibraryBy.get()
        listOf(LibraryGroup.UNGROUPED, LibraryGroup.BY_SOURCE) shouldContain grouping
    }

    @Test
    fun groupPageWithoutExtras() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        show(harness, hasCategories = false)
        tab("Group")
        compose.onNodeWithText("Tracking status").assertDoesNotExist()
        compose.onNodeWithText("Ungrouped").assertExists()
        compose.onNodeWithText("Status").performClick()
    }

    @Test
    fun drawablesPerGroupType() {
        listOf(
            LibraryGroup.BY_STATUS,
            LibraryGroup.BY_TRACK_STATUS,
            LibraryGroup.BY_SOURCE,
            LibraryGroup.UNGROUPED,
            LibraryGroup.BY_DEFAULT,
        ).map(::groupTypeDrawableRes).toSet().size shouldBe 5
    }
}
