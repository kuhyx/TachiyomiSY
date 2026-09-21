package eu.kanade.presentation.manga.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
internal fun MangaToolbar(
    title: String,
    hasFilters: Boolean,
    navigateUp: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickEditNotes: () -> Unit,
    // SY -->
    onClickEditInfo: (() -> Unit)?,
    onClickRecommend: (() -> Unit)?,
    onClickMerge: (() -> Unit)?,
    onClickMergedSettings: (() -> Unit)?,
    // SY <--

    // For action mode
    actionModeCounter: Int,
    onCancelActionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,

    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val isActionMode = actionModeCounter > 0
    val overflow = OverflowCallbacks(
        onClickRefresh = onClickRefresh,
        onClickEditCategory = onClickEditCategory,
        onClickMigrate = onClickMigrate,
        onClickShare = onClickShare,
        onClickEditNotes = onClickEditNotes,
        // SY -->
        sy = SyOverflowCallbacks(onClickMerge, onClickEditInfo, onClickRecommend, onClickMergedSettings),
        // SY <--
    )
    AppBar(
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, modifier = Modifier.alpha(titleAlphaProvider()))
            }
        },
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(3.dp)
            .copy(alpha = if (isActionMode) 1f else backgroundAlphaProvider()),
        navigateUp = navigateUp,
        actions = {
            if (isActionMode) {
                AppBarActions(selectionActions(onSelectAll, onInvertSelection))
            } else {
                MangaToolbarActions(
                    hasFilters = hasFilters,
                    onClickFilter = onClickFilter,
                    onClickDownload = onClickDownload,
                    overflow = overflow,
                )
            }
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
    )
}

@Composable
private fun selectionActions(
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
): List<AppBar.AppBarAction> = listOf(
    AppBar.Action(
        title = stringResource(MR.strings.action_select_all),
        icon = Icons.Outlined.SelectAll,
        onClick = onSelectAll,
    ),
    AppBar.Action(
        title = stringResource(MR.strings.action_select_inverse),
        icon = Icons.Outlined.FlipToBack,
        onClick = onInvertSelection,
    ),
)

// Download (with its dropdown), filter, then the overflow entries.
@Composable
private fun MangaToolbarActions(
    hasFilters: Boolean,
    onClickFilter: () -> Unit,
    onClickDownload: ((DownloadAction) -> Unit)?,
    overflow: OverflowCallbacks,
) {
    var downloadExpanded by remember { mutableStateOf(false) }
    if (onClickDownload != null) {
        DownloadDropdownMenu(
            expanded = downloadExpanded,
            onDismissRequest = { downloadExpanded = false },
            onDownloadClicked = onClickDownload,
        )
    }

    val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
    AppBarActions(
        actions = listOfNotNull(
            AppBar.Action(
                title = stringResource(MR.strings.manga_download),
                icon = Icons.Outlined.Download,
                onClick = { downloadExpanded = !downloadExpanded },
            ).takeIf { onClickDownload != null },
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.FilterList,
                iconTint = filterTint,
                onClick = onClickFilter,
            ),
        ) + overflow.entries(),
    )
}

// The overflow entries, in menu order; a null callback hides its entry.
private data class OverflowCallbacks(
    val onClickRefresh: () -> Unit,
    val onClickEditCategory: (() -> Unit)?,
    val onClickMigrate: (() -> Unit)?,
    val onClickShare: (() -> Unit)?,
    val onClickEditNotes: () -> Unit,
    // SY -->
    val sy: SyOverflowCallbacks,
    // SY <--
)

// SY -->
private data class SyOverflowCallbacks(
    val onClickMerge: (() -> Unit)?,
    val onClickEditInfo: (() -> Unit)?,
    val onClickRecommend: (() -> Unit)?,
    val onClickMergedSettings: (() -> Unit)?,
)
// SY <--

@Composable
private fun OverflowCallbacks.entries(): List<AppBar.OverflowAction> {
    val ordered = listOf(
        MR.strings.action_webview_refresh to onClickRefresh,
        MR.strings.action_edit_categories to onClickEditCategory,
        MR.strings.action_migrate to onClickMigrate,
        MR.strings.action_share to onClickShare,
        MR.strings.action_notes to onClickEditNotes,
        // SY -->
        SYMR.strings.merge to sy.onClickMerge,
        SYMR.strings.action_edit_info to sy.onClickEditInfo,
        SYMR.strings.az_recommends to sy.onClickRecommend,
        SYMR.strings.merge_settings to sy.onClickMergedSettings,
        // SY <--
    )
    return ordered.mapNotNull { (label, onClick) ->
        onClick?.let { AppBar.OverflowAction(title = stringResource(label), onClick = it) }
    }
}
