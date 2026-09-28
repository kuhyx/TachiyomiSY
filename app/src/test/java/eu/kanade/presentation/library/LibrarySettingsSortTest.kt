package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort

/** The sort page of a grouped library, which shows the global sort. */
@RunWith(RobolectricTestRunner::class)
internal class LibrarySettingsSortTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showSorted(sort: LibrarySort) {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        harness.libraryPreferences.groupLibraryBy.set(LibraryGroup.BY_SOURCE)
        harness.libraryPreferences.sortingMode.set(sort)
        compose.setContent { MaterialTheme { Column { SortPage(category = null, screenModel = harness.model) } } }
        compose.waitForIdle()
    }

    @Test
    fun globalRandomSortIsMarked() {
        showSorted(LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending))
        compose.onNodeWithText("Random").assertExists()
    }

    @Test
    fun globalDescendingSortIsMarked() {
        showSorted(LibrarySort(LibrarySort.Type.Alphabetical, LibrarySort.Direction.Descending))
        compose.onNodeWithText("Alphabetically").assertExists()
    }
}
