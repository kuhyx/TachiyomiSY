package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.bookmarkedFilter
import tachiyomi.domain.manga.model.displayMode
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.sorting
import tachiyomi.domain.manga.model.unreadFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun ChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    manga: Manga? = null,
    onDownloadFilterChanged: (TriState) -> Unit,
    onUnreadFilterChanged: (TriState) -> Unit,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: (() -> Unit),
    onSortModeChanged: (Long) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: (applyToExistingManga: Boolean) -> Unit,
    onResetToDefault: () -> Unit,
) {
    var showSetAsDefaultDialog by rememberSaveable { mutableStateOf(false) }
    if (showSetAsDefaultDialog) {
        SetAsDefaultDialog(
            onDismissRequest = { showSetAsDefaultDialog = false },
            onConfirmed = onSetAsDefault,
        )
    }

    val downloadedOnly = remember { Injekt.get<BasePreferences>().downloadedOnly.get() }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(MR.strings.action_filter),
            stringResource(MR.strings.action_sort),
            stringResource(MR.strings.action_display),
        ),
        tabOverflowMenuContent = { closeMenu ->
            DefaultsMenu(
                closeMenu = closeMenu,
                onSetAsDefault = { showSetAsDefaultDialog = true },
                onResetToDefault = onResetToDefault,
            )
        },
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    downloadFilter = manga?.downloadedFilter ?: TriState.DISABLED,
                    onDownloadFilterChanged = onDownloadFilterChanged
                        .takeUnless { downloadedOnly },
                    unreadFilter = manga?.unreadFilter ?: TriState.DISABLED,
                    onUnreadFilterChanged = onUnreadFilterChanged,
                    bookmarkedFilter = manga?.bookmarkedFilter ?: TriState.DISABLED,
                    onBookmarkedFilterChanged = onBookmarkedFilterChanged,
                    scanlatorFilterActive = scanlatorFilterActive,
                    onScanlatorFilterClicked = onScanlatorFilterClicked,
                )
                1 -> SortPage(
                    sortingMode = manga?.sorting ?: 0,
                    sortDescending = manga?.sortDescending() ?: false,
                    onItemSelected = onSortModeChanged,
                )
                2 -> DisplayPage(
                    displayMode = manga?.displayMode ?: 0,
                    onItemSelected = onDisplayModeChanged,
                )
            }
        }
    }
}

@Composable
private fun DefaultsMenu(closeMenu: () -> Unit, onSetAsDefault: () -> Unit, onResetToDefault: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(MR.strings.set_chapter_settings_as_default)) },
        onClick = {
            onSetAsDefault()
            closeMenu()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(MR.strings.action_reset)) },
        onClick = {
            onResetToDefault()
            closeMenu()
        },
    )
}

@Composable
private fun SetAsDefaultDialog(
    onDismissRequest: () -> Unit,
    onConfirmed: (optionalChecked: Boolean) -> Unit,
) {
    var optionalChecked by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.chapter_settings)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(MR.strings.confirm_set_chapter_settings))

                LabeledCheckbox(
                    label = stringResource(MR.strings.also_set_chapter_settings_for_library),
                    checked = optionalChecked,
                    onCheckedChange = { optionalChecked = it },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmed(optionalChecked)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}
