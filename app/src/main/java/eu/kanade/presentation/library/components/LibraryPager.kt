package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    selection: Set<Long>,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    getCategoryForPage: (Int) -> Category,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    onClickManga: (Category, LibraryManga) -> Unit,
    onLongClickManga: (Category, LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalAlignment = Alignment.Top,
    ) { page ->
        if (page !in state.currentPage - 1..(state.currentPage + 1)) {
            // To make sure only one offscreen page is being composed
        } else {
            val category = getCategoryForPage(page)
            val items = getItemsForCategory(category)

            if (items.isEmpty()) {
                LibraryPagerEmptyScreen(
                    searchQuery = searchQuery,
                    hasActiveFilters = hasActiveFilters,
                    contentPadding = contentPadding,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            } else {
                val displayMode by getDisplayMode(page)
                LibraryPage(
                    displayMode = displayMode,
                    items = items,
                    contentPadding = contentPadding,
                    selection = selection,
                    searchQuery = searchQuery,
                    getColumnsForOrientation = getColumnsForOrientation,
                    onClick = { onClickManga(category, it) },
                    onLongClick = { onLongClickManga(category, it) },
                    onClickContinueReading = onClickContinueReading,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            }
        }
    }
}

// One category rendered in its display mode; grids read the column count for the current orientation.
@Composable
private fun LibraryPage(
    displayMode: LibraryDisplayMode,
    items: List<LibraryItem>,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    searchQuery: String?,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onGlobalSearchClicked: () -> Unit,
) {
    val columns by if (displayMode != LibraryDisplayMode.List) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        remember(isLandscape) { getColumnsForOrientation(isLandscape) }
    } else {
        remember { mutableIntStateOf(0) }
    }
    pageContent(
        displayMode = displayMode,
        list = {
            LibraryList(
                items = items,
                contentPadding = contentPadding,
                selection = selection,
                onClick = onClick,
                onLongClick = onLongClick,
                onClickContinueReading = onClickContinueReading,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
            )
        },
        compactGrid = { showTitle ->
            LibraryCompactGrid(
                items = items,
                showTitle = showTitle,
                columns = columns,
                contentPadding = contentPadding,
                selection = selection,
                onClick = onClick,
                onLongClick = onLongClick,
                onClickContinueReading = onClickContinueReading,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
            )
        },
        comfortableGrid = {
            LibraryComfortableGrid(
                items = items,
                columns = columns,
                contentPadding = contentPadding,
                selection = selection,
                onClick = onClick,
                onLongClick = onLongClick,
                onClickContinueReading = onClickContinueReading,
                searchQuery = searchQuery,
                onGlobalSearchClicked = onGlobalSearchClicked,
            )
        },
    )()
}

// Picks the layout for a display mode; plain so the exhaustive `when` stays out of Compose. The two
// compact modes share one call site, so switching between them keeps the grid's state.
private fun pageContent(
    displayMode: LibraryDisplayMode,
    list: @Composable () -> Unit,
    compactGrid: @Composable (showTitle: Boolean) -> Unit,
    comfortableGrid: @Composable () -> Unit,
): @Composable () -> Unit = when (displayMode) {
    LibraryDisplayMode.List -> {
        list
    }
    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
        { compactGrid(displayMode is LibraryDisplayMode.CompactGrid) }
    }
    LibraryDisplayMode.ComfortableGrid -> {
        comfortableGrid
    }
}

@Composable
private fun LibraryPagerEmptyScreen(
    searchQuery: String?,
    hasActiveFilters: Boolean,
    contentPadding: PaddingValues,
    onGlobalSearchClicked: () -> Unit,
) {
    val msg = when {
        !searchQuery.isNullOrEmpty() -> MR.strings.no_results_found
        hasActiveFilters -> MR.strings.error_no_match
        else -> MR.strings.information_no_manga_category
    }

    Column(
        modifier = Modifier
            .padding(contentPadding + PaddingValues(8.dp))
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                searchQuery = searchQuery,
                onClick = onGlobalSearchClicked,
            )
        }

        EmptyScreen(
            stringRes = msg,
            modifier = Modifier.weight(1f),
        )
    }
}
