package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.SyncSettingsSelector
import eu.kanade.tachiyomi.data.sync.SyncManager
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun getBasePreferences(
    syncServiceType: SyncManager.SyncService,
    syncPreferences: SyncPreferences,
): List<Preference> {
    val navigator = LocalNavigator.currentOrThrow
    val preferences = servicePreferences(syncServiceType, syncPreferences)()

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

// The chosen service's own settings; plain so the exhaustive `when` stays out of Compose.
private fun servicePreferences(
    syncServiceType: SyncManager.SyncService,
    syncPreferences: SyncPreferences,
): @Composable () -> List<Preference> = when (syncServiceType) {
    SyncManager.SyncService.NONE -> {
        { emptyList() }
    }
    SyncManager.SyncService.SYNCYOMI -> {
        { getSelfHostPreferences(syncPreferences) }
    }
    SyncManager.SyncService.GOOGLE_DRIVE -> {
        { getGoogleDrivePreferences() }
    }
}
