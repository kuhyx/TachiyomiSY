package eu.kanade.presentation.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga

/** Composes [LibraryContent] over [items] per category id, recording callbacks in [events]. */
internal class LibraryContentHarness(private val compose: ComposeContentTestRule) {
    val events: MutableList<String> = mutableListOf()
    val store: FlowPreferenceStore = FlowPreferenceStore()
    var refreshStarts: Boolean = true

    fun show(categories: List<Category>, items: Map<Long, List<LibraryItem>>, options: LibraryShow = LibraryShow()) {
        val displayMode = options.displayMode
        val modePref = store.getObjectFromString(
            key = "mode",
            defaultValue = displayMode,
            serializer = { it.serialize() },
            deserializer = { LibraryDisplayMode.deserialize(it) },
        )
        val columnsPref = store.getInt("columns", options.columns)
        val continueReading: (LibraryManga) -> Unit = { events += "continue ${it.id}" }
        compose.setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                LibraryContent(
                    categories = categories,
                    searchQuery = options.searchQuery,
                    selection = options.selection,
                    contentPadding = PaddingValues(),
                    currentPage = options.currentPage,
                    hasActiveFilters = options.hasActiveFilters,
                    showPageTabs = options.showPageTabs,
                    onChangeCurrentPage = { events += "page $it" },
                    onClickManga = { events += "open $it" },
                    onContinueReadingClicked = continueReading.takeIf { options.continueReading },
                    onToggleSelection = { category, manga -> events += "toggle ${category.id} ${manga.id}" },
                    onToggleRangeSelection = { category, manga -> events += "range ${category.id} ${manga.id}" },
                    onRefresh = {
                        events += "refresh"
                        refreshStarts
                    },
                    onGlobalSearchClicked = { events += "global" },
                    getItemCountForCategory = { items[it.id]?.size },
                    getDisplayMode = { PreferenceMutableState(modePref, scope) },
                    getColumnsForOrientation = { PreferenceMutableState(columnsPref, scope) },
                    getItemsForCategory = { items[it.id].orEmpty() },
                )
            }
        }
        compose.waitForIdle()
    }
}

/** The knobs of [LibraryContentHarness.show]; the defaults are a plain compact grid on page 0. */
internal data class LibraryShow(
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.CompactGrid,
    val columns: Int = 2,
    val selection: Set<Long> = emptySet(),
    val searchQuery: String? = null,
    val hasActiveFilters: Boolean = false,
    val showPageTabs: Boolean = true,
    val currentPage: Int = 0,
    val continueReading: Boolean = true,
)
