@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Returns a [PendingIntent] that resumes the download of a chapter.
 *
 * @param context context of application
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.resumeDownloadsBroadcast(context: Context): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_RESUME_DOWNLOADS
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that pauses the download queue.
 *
 * @param context context of application
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.pauseDownloadsPendingBroadcast(context: Context): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_PAUSE_DOWNLOADS
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns a [PendingIntent] that clears the download queue.
 *
 * @param context context of application
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.clearDownloadsPendingBroadcast(context: Context): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_CLEAR_DOWNLOADS
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that downloads chapters.
 *
 * @param context context of application
 * @param manga manga of chapter
 * @param chapters chapters to download
 * @param groupId id of the notification group to dismiss
 */
internal fun NotificationReceiver.Companion.downloadChaptersBroadcast(
    context: Context,
    manga: Manga,
    chapters: Array<Chapter>,
    groupId: Int,
): PendingIntent {
    val newIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_DOWNLOAD_CHAPTER
        putExtra(EXTRA_CHAPTER_URL, chapters.map { it.url }.toTypedArray())
        putExtra(EXTRA_MANGA_ID, manga.id)
        putExtra(EXTRA_NOTIFICATION_ID, manga.id.hashCode())
        putExtra(EXTRA_GROUP_ID, groupId)
    }
    return PendingIntent.getBroadcast(
        context,
        manga.id.hashCode(),
        newIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that starts the [AppUpdateDownloadJob] to download an app update.
 *
 * @param context context of application
 * @param url download url of the update
 * @param title release title shown in the notification
 * @return [PendingIntent]
 */
internal fun NotificationReceiver.Companion.downloadAppUpdateBroadcast(
    context: Context,
    url: String,
    title: String? = null,
): PendingIntent {
    return Intent(context, NotificationReceiver::class.java).run {
        action = ACTION_START_APP_UPDATE
        putExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL, url)
        title?.let { putExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE, it) }
        PendingIntent.getBroadcast(
            context,
            0,
            this,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 *
 */
internal fun NotificationReceiver.Companion.cancelAppUpdateBroadcast(context: Context): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_CANCEL_APP_UPDATE_DOWNLOAD
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
