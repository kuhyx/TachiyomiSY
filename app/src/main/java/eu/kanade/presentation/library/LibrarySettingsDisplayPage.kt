package eu.kanade.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import kotlinx.coroutines.flow.map
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BaseSortItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    MR.strings.action_display_list to LibraryDisplayMode.List,
)

@Composable
internal fun ColumnScope.SortPage(
    category: Category?,
    screenModel: LibrarySettingsScreenModel,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    // SY -->
    val globalSortMode by screenModel.libraryPreferences.sortingMode.collectAsState()
    val sortingMode = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        category.sort.type
    } else {
        globalSortMode.type
    }
    val sortDescending = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        !category.sort.isAscending
    } else {
        !globalSortMode.isAscending
    }
    val hasSortTags by remember {
        screenModel.libraryPreferences.sortTagsForLibrary.changes()
            .map { it.isNotEmpty() }
    }.collectAsState(initial = screenModel.libraryPreferences.sortTagsForLibrary.get().isNotEmpty())
    // SY <--

    val options = remember(trackers.isEmpty()/* SY --> */, hasSortTags/* SY <-- */) {
        sortOptions(hasTrackers = trackers.isNotEmpty(), hasSortTags = hasSortTags)
    }

    options.map { (titleRes, mode) ->
        if (mode == LibrarySort.Type.Random) {
            BaseSortItem(
                label = stringResource(titleRes),
                icon = Icons.Default.Refresh
                    .takeIf { sortingMode == LibrarySort.Type.Random },
                onClick = {
                    screenModel.setSort(category, mode, LibrarySort.Direction.Ascending)
                },
            )
        } else {
            SortItem(
                label = stringResource(titleRes),
                sortDescending = sortDescending.takeIf { sortingMode == mode },
                onClick = {
                    val direction = nextDirection(isTogglingDirection = sortingMode == mode, sortDescending)
                    screenModel.setSort(category, mode, direction)
                },
            )
        }
    }
}

@Composable
internal fun ColumnScope.DisplayPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val displayMode by screenModel.libraryPreferences.displayMode.collectAsState()
    SettingsChipRow(MR.strings.action_display_mode) {
        displayModes.map { (titleRes, mode) ->
            FilterChip(
                selected = displayMode == mode,
                onClick = { screenModel.setDisplayMode(mode) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List) {
        ColumnsSlider(screenModel)
    }

    HeadingItem(MR.strings.overlay_header)
    listOf(
        MR.strings.action_display_download_badge to screenModel.libraryPreferences.downloadBadge,
        MR.strings.action_display_unread_badge to screenModel.libraryPreferences.unreadBadge,
        MR.strings.action_display_local_badge to screenModel.libraryPreferences.localBadge,
        MR.strings.action_display_language_badge to screenModel.libraryPreferences.languageBadge,
        MR.strings.action_display_show_continue_reading_button to
            screenModel.libraryPreferences.showContinueReadingButton,
    ).forEach { (label, pref) ->
        CheckboxItem(label = stringResource(label), pref = pref)
    }

    HeadingItem(MR.strings.tabs_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_tabs),
        pref = screenModel.libraryPreferences.categoryTabs,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_number_of_items),
        pref = screenModel.libraryPreferences.categoryNumberOfItems,
    )
}

// Grid columns for the current orientation; 0 means "auto".
@Composable
internal fun ColumnsSlider(screenModel: LibrarySettingsScreenModel) {
    val configuration = LocalConfiguration.current
    val columnPreference = remember {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenModel.libraryPreferences.landscapeColumns
        } else {
            screenModel.libraryPreferences.portraitColumns
        }
    }
    val columns by columnPreference.collectAsState()
    SliderItem(
        value = columns,
        valueRange = 0..10,
        label = stringResource(MR.strings.pref_library_columns),
        valueString = if (columns > 0) {
            columns.toString()
        } else {
            stringResource(MR.strings.label_auto)
        },
        onChange = columnPreference::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}
