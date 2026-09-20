package eu.kanade.presentation.more.settings.screen

import android.widget.Toast
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
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.library.components.SyncFavoritesWarningDialog
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.toast
import exh.eh.EHentaiUpdateWorker
import exh.eh.EHentaiUpdateWorkerConstants
import exh.source.ExhPreferences
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private const val THREE_HOURS = 3
private const val SIX_HOURS = 6
private const val TWELVE_HOURS = 12
private const val ONE_DAY_HOURS = 24
private const val TWO_DAYS_HOURS = 48

/*
 * The favourites-sync and update-checker preferences of the E-Hentai settings screen.
 * Part of [SettingsEhScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun readOnlySync(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.exhReadOnlySync,
        title = stringResource(SYMR.strings.disable_favorites_uploading),
        subtitle = stringResource(SYMR.strings.disable_favorites_uploading_summary),
    )
}

@Composable
internal fun syncFavoriteNotes(): Preference.PreferenceItem.TextPreference {
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        SyncFavoritesWarningDialog(
            onDismissRequest = { dialogOpen = false },
            onAccept = { dialogOpen = false },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.show_favorite_sync_notes),
        subtitle = stringResource(SYMR.strings.show_favorite_sync_notes_summary),
        onClick = { dialogOpen = true },
    )
}

@Composable
internal fun lenientSync(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.exhLenientSync,
        title = stringResource(SYMR.strings.ignore_sync_errors),
        subtitle = stringResource(SYMR.strings.ignore_sync_errors_summary),
    )
}

@Composable
internal fun SyncResetDialog(
    onDismissRequest: () -> Unit,
    onStartReset: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(SYMR.strings.favorites_sync_reset))
        },
        text = {
            Text(text = stringResource(SYMR.strings.favorites_sync_reset_message))
        },
        confirmButton = {
            TextButton(onClick = onStartReset) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}

@Composable
internal fun forceSyncReset(deleteFavoriteEntries: DeleteFavoriteEntries): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        SyncResetDialog(
            onDismissRequest = { dialogOpen = false },
            onStartReset = {
                dialogOpen = false
                scope.launchNonCancellable {
                    try {
                        deleteFavoriteEntries.await()
                        withUIContext {
                            context.toast(context.stringResource(SYMR.strings.sync_state_reset), Toast.LENGTH_LONG)
                        }
                    } catch (expected: Exception) {
                        // Logged whatever the cause; the caller carries on.
                        SettingsEhScreen.logcat(LogPriority.ERROR, expected)
                    }
                }
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.force_sync_state_reset),
        subtitle = stringResource(SYMR.strings.force_sync_state_reset_summary),
        onClick = {
            dialogOpen = true
        },
    )
}

@Composable
internal fun updateCheckerFrequency(
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.ListPreference<Int> {
    val value by exhPreferences.exhAutoUpdateFrequency.collectAsState()
    val context = LocalContext.current
    return Preference.PreferenceItem.ListPreference(
        preference = exhPreferences.exhAutoUpdateFrequency,
        title = stringResource(SYMR.strings.time_between_batches),
        subtitle = if (value == 0) {
            stringResource(SYMR.strings.time_between_batches_summary_1, stringResource(MR.strings.app_name))
        } else {
            stringResource(
                SYMR.strings.time_between_batches_summary_2,
                stringResource(MR.strings.app_name),
                value,
                EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION,
            )
        },
        entries = mapOf(
            0 to stringResource(SYMR.strings.time_between_batches_never),
            1 to stringResource(SYMR.strings.time_between_batches_1_hour),
            2 to stringResource(SYMR.strings.time_between_batches_2_hours),
            THREE_HOURS to stringResource(SYMR.strings.time_between_batches_3_hours),
            SIX_HOURS to stringResource(SYMR.strings.time_between_batches_6_hours),
            TWELVE_HOURS to stringResource(SYMR.strings.time_between_batches_12_hours),
            ONE_DAY_HOURS to stringResource(SYMR.strings.time_between_batches_24_hours),
            TWO_DAYS_HOURS to stringResource(SYMR.strings.time_between_batches_48_hours),
        ),
        onValueChanged = { interval ->
            EHentaiUpdateWorker.scheduleBackground(context, prefInterval = interval)
            true
        },
    )
}

@Composable
internal fun autoUpdateRequirements(
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.MultiSelectListPreference<String> {
    val value by exhPreferences.exhAutoUpdateRequirements.collectAsState()
    val context = LocalContext.current
    return Preference.PreferenceItem.MultiSelectListPreference(
        preference = exhPreferences.exhAutoUpdateRequirements,
        title = stringResource(SYMR.strings.auto_update_restrictions),
        subtitle = remember(value) {
            context.stringResource(
                MR.strings.restrictions,
                value.sorted()
                    .map {
                        when (it) {
                            DEVICE_ONLY_ON_WIFI -> context.stringResource(MR.strings.connected_to_wifi)
                            DEVICE_CHARGING -> context.stringResource(MR.strings.charging)
                            else -> it
                        }
                    }
                    .ifEmpty {
                        listOf(context.stringResource(MR.strings.none))
                    }
                    .joinToString(),
            )
        },
        entries = mapOf(
            DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
            DEVICE_CHARGING to stringResource(MR.strings.charging),
        ),
        onValueChanged = { restrictions ->
            EHentaiUpdateWorker.scheduleBackground(context, prefRestrictions = restrictions)
            true
        },
    )
}
