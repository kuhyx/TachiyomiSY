package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun LanguageDialogRowCheckbox(
    columnState: LanguageDialogState.ColumnState,
    onStateChange: (LanguageDialogState.ColumnState) -> Unit,
) {
    if (columnState != LanguageDialogState.ColumnState.Unavailable) {
        Checkbox(
            checked = columnState == LanguageDialogState.ColumnState.Enabled,
            onCheckedChange = {
                if (it) {
                    onStateChange(LanguageDialogState.ColumnState.Enabled)
                } else {
                    onStateChange(LanguageDialogState.ColumnState.Disabled)
                }
            },
        )
    } else {
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
internal fun LanguageDialogRow(
    language: String,
    row: LanguageDialogState.RowState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language,
            modifier = Modifier
                .padding(4.dp)
                .width(80.dp),
            maxLines = 1,
        )
        LanguageDialogRowCheckbox(row.original, onStateChange = { row.original = it })
        LanguageDialogRowCheckbox(row.translated, onStateChange = { row.translated = it })
        LanguageDialogRowCheckbox(row.rewrite, onStateChange = { row.rewrite = it })
    }
}

@Composable
internal fun LanguagesDialog(
    onDismissRequest: () -> Unit,
    initialValue: String,
    onValueChange: (String) -> Unit,
) {
    val state = remember(initialValue) { LanguageDialogState(initialValue) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(SYMR.strings.language_filtering)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(SYMR.strings.language_filtering_summary))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Language", modifier = Modifier.padding(4.dp))
                    Text(text = "Original", modifier = Modifier.padding(4.dp))
                    Text(text = "Translated", modifier = Modifier.padding(4.dp))
                    Text(text = "Rewrite", modifier = Modifier.padding(4.dp))
                }
                LanguageDialogRow(language = "Japanese", row = state.japanese)
                LanguageDialogRow(language = "English", row = state.english)
                LanguageDialogRow(language = "Chinese", row = state.chinese)
                LanguageDialogRow(language = "Dutch", row = state.dutch)
                LanguageDialogRow(language = "French", row = state.french)
                LanguageDialogRow(language = "German", row = state.german)
                LanguageDialogRow(language = "Hungarian", row = state.hungarian)
                LanguageDialogRow(language = "Italian", row = state.italian)
                LanguageDialogRow(language = "Korean", row = state.korean)
                LanguageDialogRow(language = "Polish", row = state.polish)
                LanguageDialogRow(language = "Portuguese", row = state.portuguese)
                LanguageDialogRow(language = "Russian", row = state.russian)
                LanguageDialogRow(language = "Spanish", row = state.spanish)
                LanguageDialogRow(language = "Thai", row = state.thai)
                LanguageDialogRow(language = "Vietnamese", row = state.vietnamese)
                LanguageDialogRow(language = "N/A", row = state.notAvailable)
                LanguageDialogRow(language = "Other", row = state.other)
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueChange(state.toPreference()) }) {
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
internal fun settingsLanguages(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.TextPreference {
    val value by exhPreferences.exhSettingsLanguages.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        LanguagesDialog(
            onDismissRequest = { dialogOpen = false },
            initialValue = value,
            onValueChange = {
                dialogOpen = false
                exhPreferences.exhSettingsLanguages.set(it)
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.language_filtering),
        subtitle = stringResource(SYMR.strings.language_filtering_summary),
        onClick = {
            dialogOpen = true
        },
        enabled = exhentaiEnabled,
    )
}
