package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import exh.source.anyIs
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

@Composable
internal fun BrowseSourceToolbar(
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    source: Source?,
    displayMode: LibraryDisplayMode?,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    navigateUp: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    // Avoid capturing unstable source in actions lambda
    val title = source?.name
    val isLocalSource = source is LocalSource
    val isConfigurableSource = source?.anyIs<ConfigurableSource>() == true

    var selectingDisplayMode by remember { mutableStateOf(false) }

    SearchToolbar(
        navigateUp = navigateUp,
        titleContent = { AppBarTitle(title) },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        onSearch = onSearch,
        onClickCloseSearch = navigateUp,
        actions = {
            AppBarActions(
                actions = toolbarActions(
                    displayMode = displayMode,
                    isLocalSource = isLocalSource,
                    isConfigurableSource = isConfigurableSource,
                    onSelectDisplayMode = { selectingDisplayMode = true },
                    onWebViewClick = onWebViewClick,
                    onHelpClick = onHelpClick,
                    onSettingsClick = onSettingsClick,
                ),
            )
            DisplayModeMenu(
                expanded = selectingDisplayMode,
                displayMode = displayMode,
                onDismissRequest = { selectingDisplayMode = false },
                onDisplayModeChange = onDisplayModeChange,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

// The help (local) or web view (remote) entry moves into the overflow when the toolbar is already full.
@Composable
private fun toolbarActions(
    displayMode: LibraryDisplayMode?,
    isLocalSource: Boolean,
    isConfigurableSource: Boolean,
    onSelectDisplayMode: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
): List<AppBar.AppBarAction> {
    val crowded = isConfigurableSource && displayMode != null
    val (label, icon, onClick) = if (isLocalSource) {
        Triple(MR.strings.label_help, Icons.AutoMirrored.Outlined.Help, onHelpClick)
    } else {
        Triple(MR.strings.action_web_view, Icons.Outlined.Public, onWebViewClick)
    }
    val helpOrWebView = if (crowded) {
        AppBar.OverflowAction(title = stringResource(label), onClick = onClick)
    } else {
        AppBar.Action(title = stringResource(label), icon = icon, onClick = onClick)
    }
    // SY <--
    return listOfNotNull(
        displayMode?.let { displayModeAction(it, onSelectDisplayMode) },
        helpOrWebView,
        AppBar.OverflowAction(title = stringResource(MR.strings.action_settings), onClick = onSettingsClick)
            .takeIf { isConfigurableSource },
    )
}

@Composable
private fun displayModeAction(displayMode: LibraryDisplayMode, onSelectDisplayMode: () -> Unit) = AppBar.Action(
    title = stringResource(MR.strings.action_display_mode),
    icon = if (displayMode == LibraryDisplayMode.List) {
        Icons.AutoMirrored.Filled.ViewList
    } else {
        Icons.Filled.ViewModule
    },
    onClick = onSelectDisplayMode,
)

@Composable
private fun DisplayModeMenu(
    expanded: Boolean,
    displayMode: LibraryDisplayMode?,
    onDismissRequest: () -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        listOf(
            MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
            MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
            MR.strings.action_display_list to LibraryDisplayMode.List,
        ).forEach { (label, mode) ->
            RadioMenuItem(
                isChecked = displayMode == mode,
                onClick = {
                    onDismissRequest()
                    onDisplayModeChange(mode)
                },
            ) {
                Text(text = stringResource(label))
            }
        }
    }
}
