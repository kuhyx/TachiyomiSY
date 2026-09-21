package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.category.biometric.BiometricTimesScreen
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

// SY --> The fork's security rows: CBZ password protection and the biometric lock schedule.

@Composable
internal fun SettingsSecurityScreen.cbzPasswordPreferences(
    securityPreferences: SecurityPreferences,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val scope = rememberCoroutineScope()
    val isCbzPasswordSet by remember { CbzCrypto.isPasswordSetState(scope) }.collectFlowAsState()
    val passwordProtectDownloads by securityPreferences.passwordProtectDownloads.collectAsState()
    return listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = securityPreferences.passwordProtectDownloads,
            title = stringResource(SYMR.strings.password_protect_downloads),
            subtitle = stringResource(SYMR.strings.password_protect_downloads_summary),
            enabled = isCbzPasswordSet,
        ),
        Preference.PreferenceItem.ListPreference(
            preference = securityPreferences.encryptionType,
            title = stringResource(SYMR.strings.encryption_type),
            entries = SecurityPreferences.EncryptionType.entries
                .associateWith { stringResource(it.titleRes) },
            enabled = passwordProtectDownloads,
        ),
        setCbzPasswordPreference(securityPreferences),
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.delete_cbz_archive_password),
            onClick = {
                CbzCrypto.deleteKeyCbz()
                securityPreferences.cbzPassword.set("")
            },
            enabled = isCbzPasswordSet,
        ),
    )
}

@Composable
private fun SettingsSecurityScreen.setCbzPasswordPreference(
    securityPreferences: SecurityPreferences,
): Preference.PreferenceItem<out Any, out Any> {
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        PasswordDialog(
            onDismissRequest = { dialogOpen = false },
            onReturnPassword = { password ->
                dialogOpen = false
                CbzCrypto.deleteKeyCbz()
                securityPreferences.cbzPassword.set(CbzCrypto.encryptCbz(password.replace("\n", "")))
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.set_cbz_zip_password),
        onClick = { dialogOpen = true },
    )
}

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
private fun DayList(selected: SnapshotStateList<SettingsSecurityScreen.DayOption>) {
    LazyColumn {
        SettingsSecurityScreen.DayOption.entries.forEach { day ->
            item {
                val isSelected = selected.contains(day)
                val onSelectionChanged = {
                    when (!isSelected) {
                        true -> selected.add(day)
                        false -> selected.remove(day)
                    }
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

@Composable
internal fun SettingsSecurityScreen.PasswordDialog(
    onDismissRequest: () -> Unit,
    onReturnPassword: (String) -> Unit,
) {
    val password = rememberTextFieldState()
    var passwordVisibility by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(SYMR.strings.cbz_archive_password)) },
        text = {
            SecureTextField(
                state = password,
                placeholder = { Text(text = stringResource(MR.strings.password)) },
                label = { Text(text = stringResource(MR.strings.password)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                        Icon(
                            imageVector =
                            if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                onKeyboardAction = { onReturnPassword(password.text.toString()) },
                modifier = Modifier.onKeyEvent { it.key == Key.Enter },
                textObfuscationMode =
                if (passwordVisibility) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
            )
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmButton = {
            TextButton(onClick = { onReturnPassword(password.text.toString()) }) {
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
// SY <--
