package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/*
 * The Google Drive sync preferences of the data settings screen, including the purge dialog.
 * Part of [SettingsDataScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun getGoogleDrivePreferences(): List<Preference> {
    val context = LocalContext.current
    val googleDriveSync = Injekt.get<GoogleDriveService>()
    return listOf(
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.pref_google_drive_sign_in),
            onClick = {
                val intent = googleDriveSync.getSignInIntent()
                context.startActivity(intent)
            },
        ),
        getGoogleDrivePurge(),
    )
}

@Composable
internal fun getGoogleDrivePurge(): Preference.PreferenceItem.TextPreference {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleDriveSync = remember { GoogleDriveSyncService(context) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    if (showPurgeDialog) {
        PurgeConfirmationDialog(
            onConfirm = {
                showPurgeDialog = false
                scope.launch {
                    val result = googleDriveSync.deleteSyncDataFromGoogleDrive()
                    when (result) {
                        GoogleDriveSyncService.DeleteSyncDataStatus.NOT_INITIALIZED -> context.toast(
                            SYMR.strings.google_drive_not_signed_in,
                            duration = 5000,
                        )
                        GoogleDriveSyncService.DeleteSyncDataStatus.NO_FILES -> context.toast(
                            SYMR.strings.google_drive_sync_data_not_found,
                            duration = 5000,
                        )
                        GoogleDriveSyncService.DeleteSyncDataStatus.SUCCESS -> context.toast(
                            SYMR.strings.google_drive_sync_data_purged,
                            duration = 5000,
                        )
                        GoogleDriveSyncService.DeleteSyncDataStatus.ERROR -> context.toast(
                            SYMR.strings.google_drive_sync_data_purge_error,
                            duration = 10_000,
                        )
                    }
                }
            },
            onDismissRequest = { showPurgeDialog = false },
        )
    }

    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.pref_google_drive_purge_sync_data),
        onClick = { showPurgeDialog = true },
    )
}

@Composable
internal fun PurgeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(SYMR.strings.pref_purge_confirmation_title)) },
        text = { Text(text = stringResource(SYMR.strings.pref_purge_confirmation_message)) },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}
