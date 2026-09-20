@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.Constants
import uy.kohesive.injekt.api.get

/**
 * Returns [PendingIntent] that starts a share activity.
 *
 * @param context context of application
 * @param uri location path of file
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.shareImagePendingBroadcast(context: Context, uri: Uri): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_SHARE_IMAGE
        putExtra(EXTRA_URI, uri.toString())
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that starts a service which stops the library update.
 *
 * @param context context of application
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.cancelLibraryUpdateBroadcast(context: Context): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_CANCEL_LIBRARY_UPDATE
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that opens the extensions controller.
 *
 * @param context context of application
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.openExtensionsPendingActivity(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Constants.SHORTCUT_EXTENSIONS
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that directly launches a share activity for a backup file.
 *
 * @param context context of application
 * @param uri uri of backup file
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.shareBackupPendingActivity(context: Context, uri: Uri): PendingIntent {
    val intent = uri.toShareIntent(context, "application/x-protobuf+gzip").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that opens the error log file in an external viewer.
 *
 * @param context context of application
 * @param uri uri of error log file
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.openErrorLogPendingActivity(context: Context, uri: Uri): PendingIntent {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, "text/plain")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}

/**
 * Returns [PendingIntent] that cancels a backup restore job.
 *
 * @param context context of application
 * @param notificationId id of notification
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.cancelRestorePendingBroadcast(
    context: Context,
    notificationId: Int,
): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_CANCEL_RESTORE
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that cancels a sync restore job.
 *
 * @param context context of application
 * @param notificationId id of notification
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.cancelSyncPendingBroadcast(
    context: Context,
    notificationId: Int,
): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_CANCEL_SYNC
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
