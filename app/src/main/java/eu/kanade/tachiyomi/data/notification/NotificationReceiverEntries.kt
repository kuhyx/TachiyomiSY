@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.Constants
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Returns [PendingIntent] that starts a reader activity containing chapter.
 *
 * @param context context of application
 * @param manga manga of chapter
 * @param chapter chapter that needs to be opened
 */
internal fun NotificationReceiver.Companion.openChapterPendingActivity(
    context: Context,
    manga: Manga,
    chapter: Chapter,
): PendingIntent {
    val newIntent = ReaderActivity.newIntent(context, manga.id, chapter.id)
    return PendingIntent.getActivity(
        context,
        manga.id.hashCode(),
        newIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that opens the manga info controller.
 *
 * @param context context of application
 * @param manga manga of chapter
 * @param groupId id of the notification group to dismiss
 */
internal fun NotificationReceiver.Companion.openChapterPendingActivity(
    context: Context,
    manga: Manga,
    groupId: Int,
): PendingIntent {
    val newIntent =
        Intent(context, MainActivity::class.java).setAction(Constants.SHORTCUT_MANGA)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.MANGA_EXTRA, manga.id)
            .putExtra("notificationId", manga.id.hashCode())
            .putExtra("groupId", groupId)
    return PendingIntent.getActivity(
        context,
        manga.id.hashCode(),
        newIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Returns [PendingIntent] that marks a chapter as read and deletes it if preferred.
 *
 * @param context context of application
 * @param manga manga of chapter
 * @param chapters chapters to mark as read
 * @param groupId id of the notification group to dismiss
 */
internal fun NotificationReceiver.Companion.markAsReadPendingBroadcast(
    context: Context,
    manga: Manga,
    chapters: Array<Chapter>,
    groupId: Int,
): PendingIntent {
    val newIntent = Intent(context, NotificationReceiver::class.java).apply {
        action = ACTION_MARK_AS_READ
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
 * Returns [PendingIntent] that opens the manga info controller.
 *
 * @param context context of application
 * @param mangaId id of the entry to open
 */
internal fun NotificationReceiver.Companion.openEntryPendingActivity(context: Context, mangaId: Long): PendingIntent {
    val newIntent = Intent(context, MainActivity::class.java).setAction(Constants.SHORTCUT_MANGA)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(Constants.MANGA_EXTRA, mangaId)
        .putExtra("notificationId", mangaId.hashCode())

    return PendingIntent.getActivity(
        context,
        mangaId.hashCode(),
        newIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
