package eu.kanade.tachiyomi

import android.app.PendingIntent
import android.content.Intent
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.core.firebase.FirebaseConfig
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// The process-lifetime preference observers [App.onCreate] installs.

/** Show notification to disable Incognito Mode when it's enabled. */
internal fun App.observeIncognitoMode(scope: CoroutineScope) {
    basePreferences.incognitoMode.changes()
        .onEach { enabled ->
            if (enabled) {
                disableIncognitoReceiver.register()
                notify(Notifications.ID_INCOGNITO_MODE, Notifications.CHANNEL_INCOGNITO_MODE) {
                    setContentTitle(stringResource(MR.strings.pref_incognito_mode))
                    setContentText(stringResource(MR.strings.notification_incognito_text))
                    setSmallIcon(R.drawable.ic_glasses_24dp)
                    setOngoing(true)
                    val pendingIntent = PendingIntent.getBroadcast(
                        this@observeIncognitoMode,
                        0,
                        Intent(ACTION_DISABLE_INCOGNITO_MODE).setPackage(BuildConfig.APPLICATION_ID),
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    setContentIntent(pendingIntent)
                }
            } else {
                disableIncognitoReceiver.unregister()
                cancelNotification(Notifications.ID_INCOGNITO_MODE)
            }
        }
        .launchIn(scope)
}

/** Firebase opt-ins and the hardware-bitmap threshold follow their preferences. */
internal fun App.observeRuntimePreferences(scope: CoroutineScope) {
    privacyPreferences.analytics
        .changes()
        .onEach(FirebaseConfig::setAnalyticsEnabled)
        .launchIn(scope)
    privacyPreferences.crashlytics
        .changes()
        .onEach(FirebaseConfig::setCrashlyticsEnabled)
        .launchIn(scope)
    basePreferences.hardwareBitmapThreshold.let { preference ->
        if (!preference.isSet()) preference.set(GLUtil.DEVICE_TEXTURE_LIMIT)
    }
    basePreferences.hardwareBitmapThreshold.changes()
        .onEach { ImageUtil.hardwareBitmapThreshold = it }
        .launchIn(scope)
}

internal fun App.startSyncIfEnabledOnAppStart() {
    val syncPreferences: SyncPreferences = Injekt.get()
    val syncTriggerOpt = syncPreferences.getSyncTriggerOptions()
    if (syncPreferences.isSyncEnabled() && syncTriggerOpt.syncOnAppStart) {
        SyncDataJob.startNow(this)
    }
}
