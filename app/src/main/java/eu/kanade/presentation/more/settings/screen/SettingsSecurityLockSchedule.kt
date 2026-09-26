package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.category.biometric.BiometricTimesScreen
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun SettingsSecurityScreen.lockSchedulePreferences(
    securityPreferences: SecurityPreferences,
    useAuth: Boolean,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val navigator = LocalNavigator.currentOrThrow
    val count by securityPreferences.authenticatorTimeRanges.collectAsState()
    val selection by securityPreferences.authenticatorDays.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        SetLockedDaysDialog(
            onDismissRequest = { dialogOpen = false },
            initialSelection = selection,
            onDaysSelected = {
                dialogOpen = false
                securityPreferences.authenticatorDays.set(it)
            },
        )
    }
    return listOf(
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.action_edit_biometric_lock_times),
            subtitle = pluralStringResource(SYMR.plurals.num_lock_times, count.size, count.size),
            onClick = { navigator.push(BiometricTimesScreen()) },
            enabled = useAuth,
        ),
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.biometric_lock_days),
            subtitle = stringResource(SYMR.strings.biometric_lock_days_summary),
            onClick = { dialogOpen = true },
            enabled = useAuth,
        ),
    )
}

@Composable
internal fun SettingsSecurityScreen.SetLockedDaysDialog(
    onDismissRequest: () -> Unit,
    initialSelection: Int,
    onDaysSelected: (Int) -> Unit,
) {
    val selected = remember(initialSelection) {
        SettingsSecurityScreen.DayOption.entries.filter { it.day and initialSelection == it.day }
            .toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(SYMR.strings.biometric_lock_days)) },
        text = { DayList(selected) },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    onDaysSelected(selected.fold(0) { i, day -> i or day.day })
                },
            ) {
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
internal fun DayList(selected: SnapshotStateList<SettingsSecurityScreen.DayOption>) {
    LazyColumn {
        SettingsSecurityScreen.DayOption.entries.forEach { day ->
            item {
                val isSelected = selected.contains(day)
                // An if, not a when (Boolean): that keeps a default arm JaCoCo counts as missed.
                val onSelectionChanged = {
                    if (isSelected) selected.remove(day) else selected.add(day)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectionChanged() },
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionChanged() },
                    )
                    Text(
                        text = stringResource(day.stringRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
