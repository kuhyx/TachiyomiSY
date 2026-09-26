package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

private const val RE_READING_INDEX = 5

@Composable
internal fun SyncMangaDexDialog(
    onDismissRequest: () -> Unit,
    onSelectionConfirmed: (List<String>) -> Unit,
) {
    val resources = LocalResources.current
    val items = remember {
        resources.getStringArray(R.array.md_follows_options)
            .drop(1)
    }
    val selection = remember {
        List(items.size) { index ->
            index == 0 || index == RE_READING_INDEX
        }.toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(SYMR.strings.mangadex_sync_follows_to_library))
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEachIndexed { index, followOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // One selection entry per item, so the index is always in range.
                            .clickable { selection[index] = !selection[index] },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selection[index],
                            onCheckedChange = null,
                        )

                        Text(
                            text = followOption,
                            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelectionConfirmed(items.filterIndexed { index, _ -> selection[index] }) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
internal fun syncMangaDexIntoThis(sourcePreferences: SourcePreferences): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        SyncMangaDexDialog(
            onDismissRequest = { dialogOpen = false },
            onSelectionConfirmed = { items ->
                dialogOpen = false
                sourcePreferences.mangadexSyncToLibraryIndexes.set(
                    List(items.size) { index -> (index + 1).toString() }.toSet(),
                )
                LibraryUpdateJob.startNow(
                    context,
                    target = LibraryUpdateJob.Target.SYNC_FOLLOWS,
                )
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.mangadex_sync_follows_to_library),
        subtitle = stringResource(SYMR.strings.mangadex_sync_follows_to_library_summary),
        onClick = { dialogOpen = true },
    )
}

@Composable
internal fun syncLibraryToMangaDex(): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.mangadex_push_favorites_to_mangadex),
        subtitle = stringResource(SYMR.strings.mangadex_push_favorites_to_mangadex_summary),
        onClick = {
            LibraryUpdateJob.startNow(
                context,
                target = LibraryUpdateJob.Target.PUSH_FAVORITES,
            )
        },
    )
}
