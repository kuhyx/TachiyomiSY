package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
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
