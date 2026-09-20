package eu.kanade.presentation.more.settings.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.SyncSettingsSelector
import eu.kanade.presentation.more.settings.screen.data.SyncTriggerOptionsScreen
import eu.kanade.presentation.more.settings.widget.EditTextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrailingWidgetBuffer
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get

private const val HALF_HOUR_MINUTES = 30
private const val ONE_HOUR_MINUTES = 60
private const val THREE_HOURS_MINUTES = 180
private const val SIX_HOURS_MINUTES = 360
private const val TWELVE_HOURS_MINUTES = 720
private const val ONE_DAY_MINUTES = 1440
private const val TWO_DAYS_MINUTES = 2880
private const val ONE_WEEK_MINUTES = 10_080

/*
 * The sync groups of the data settings screen: service choice, Google Drive, self-hosted, automatic sync.
 * Part of [SettingsDataScreen]; same package, so its getPreferences() calls them as before.
 */

// SY -->
@Composable
internal fun getSyncPreferences(syncPreferences: SyncPreferences, syncService: Int): List<Preference> {
    return listOf(
        Preference.PreferenceGroup(
            title = stringResource(SYMR.strings.pref_sync_service_category),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = syncPreferences.syncService,
                    title = stringResource(SYMR.strings.pref_sync_service),
                    entries = mapOf(
                        SyncManager.SyncService.NONE.value to stringResource(MR.strings.off),
                        SyncManager.SyncService.SYNCYOMI.value to stringResource(SYMR.strings.syncyomi),
                        SyncManager.SyncService.GOOGLE_DRIVE.value to stringResource(SYMR.strings.google_drive),
                    ),
                    onValueChanged = { true },
                ),
            ),
        ),
    ) + getSyncServicePreferences(syncPreferences, syncService)
}

@Composable
internal fun getSyncServicePreferences(syncPreferences: SyncPreferences, syncService: Int): List<Preference> {
    val syncServiceType = SyncManager.SyncService.fromInt(syncService)

    val basePreferences = getBasePreferences(syncServiceType, syncPreferences)

    return if (syncServiceType != SyncManager.SyncService.NONE) {
        basePreferences + getAdditionalPreferences(syncPreferences)
    } else {
        basePreferences
    }
}

@Composable
internal fun getBasePreferences(
    syncServiceType: SyncManager.SyncService,
    syncPreferences: SyncPreferences,
): List<Preference> {
    val navigator = LocalNavigator.currentOrThrow
    val preferences = when (syncServiceType) {
        SyncManager.SyncService.NONE -> emptyList()
        SyncManager.SyncService.SYNCYOMI -> getSelfHostPreferences(syncPreferences)
        SyncManager.SyncService.GOOGLE_DRIVE -> getGoogleDrivePreferences()
    }

    return if (syncServiceType != SyncManager.SyncService.NONE) {
        preferences + Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.pref_choose_what_to_sync),
            onClick = {
                navigator.push(SyncSettingsSelector())
            },
        )
    } else {
        preferences
    }
}

@Composable
internal fun getAdditionalPreferences(syncPreferences: SyncPreferences): List<Preference> =
    listOf(getSyncNowPref(), getAutomaticSyncGroup(syncPreferences))

@Composable
internal fun getSelfHostPreferences(syncPreferences: SyncPreferences): List<Preference> {
    val scope = rememberCoroutineScope()

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) {
        if (it.contents != null && it.contents.isNotEmpty()) {
            syncPreferences.clientAPIKey.set(it.contents)
        }
    }
    val context = LocalContext.current
    val scanOptions = remember {
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setOrientationLocked(false)
            setPrompt(SYMR.strings.scan_qr_code.getString(context))
            addExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN)
        }
    }

    return listOf(
        Preference.PreferenceItem.EditTextPreference(
            title = stringResource(SYMR.strings.pref_sync_host),
            subtitle = stringResource(SYMR.strings.pref_sync_host_summ),
            preference = syncPreferences.clientHost,
            onValueChanged = { newValue ->
                scope.launch {
                    // Trim spaces at the beginning and end, then remove trailing slash if present
                    val trimmedValue = newValue.trim()
                    val modifiedValue = trimmedValue.trimEnd { it == '/' }
                    syncPreferences.clientHost.set(modifiedValue)
                }
                true
            },
        ),
        Preference.PreferenceItem.CustomPreference(
            title = stringResource(SYMR.strings.pref_sync_api_key),
        ) {
            val values by syncPreferences.clientAPIKey.collectAsState()
            EditTextPreferenceWidget(
                title = stringResource(SYMR.strings.pref_sync_api_key),
                subtitle = stringResource(SYMR.strings.pref_sync_api_key_summ),
                onConfirm = {
                    syncPreferences.clientAPIKey.set(it)
                    true
                },
                icon = null,
                value = values,
                content = {
                    IconButton(
                        onClick = { qrScanLauncher.launch(scanOptions) },
                        modifier = Modifier.padding(start = TrailingWidgetBuffer),
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(SYMR.strings.scan_qr_code),
                        )
                    }
                },
            )
        },
    )
}

@Composable
internal fun getSyncNowPref(): Preference.PreferenceGroup {
    val context = LocalContext.current
    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.pref_sync_now_group_title),
        preferenceItems = listOf(
            getSyncOptionsPref(),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(SYMR.strings.pref_sync_now),
                subtitle = stringResource(SYMR.strings.pref_sync_now_subtitle),
                onClick = {
                    if (!SyncDataJob.isRunning(context)) {
                        SyncDataJob.startNow(context, manual = true)
                    } else {
                        context.toast(SYMR.strings.sync_in_progress)
                    }
                },
            ),
        ),
    )
}

@Composable
internal fun getSyncOptionsPref(): Preference.PreferenceItem.TextPreference {
    val navigator = LocalNavigator.currentOrThrow
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.pref_sync_options),
        subtitle = stringResource(SYMR.strings.pref_sync_options_summ),
        onClick = { navigator.push(SyncTriggerOptionsScreen()) },
    )
}

@Composable
internal fun getAutomaticSyncGroup(syncPreferences: SyncPreferences): Preference.PreferenceGroup {
    val context = LocalContext.current
    val syncIntervalPref = syncPreferences.syncInterval
    val lastSync by syncPreferences.lastSyncTimestamp.collectAsState()

    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.pref_sync_automatic_category),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = syncIntervalPref,
                title = stringResource(SYMR.strings.pref_sync_interval),
                entries = mapOf(
                    0 to stringResource(MR.strings.off),
                    HALF_HOUR_MINUTES to stringResource(SYMR.strings.update_30min),
                    ONE_HOUR_MINUTES to stringResource(SYMR.strings.update_1hour),
                    THREE_HOURS_MINUTES to stringResource(SYMR.strings.update_3hour),
                    SIX_HOURS_MINUTES to stringResource(MR.strings.update_6hour),
                    TWELVE_HOURS_MINUTES to stringResource(MR.strings.update_12hour),
                    ONE_DAY_MINUTES to stringResource(MR.strings.update_24hour),
                    TWO_DAYS_MINUTES to stringResource(MR.strings.update_48hour),
                    ONE_WEEK_MINUTES to stringResource(MR.strings.update_weekly),
                ),
                onValueChanged = {
                    SyncDataJob.setupTask(context, it)
                    true
                },
            ),
            Preference.PreferenceItem.InfoPreference(
                stringResource(SYMR.strings.last_synchronization, relativeTimeSpanString(lastSync)),
            ),
        ),
    )
}
// SY <--
