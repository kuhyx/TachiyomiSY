@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.Downloader
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.notification.downloadChaptersBroadcast
import eu.kanade.tachiyomi.data.notification.markAsReadPendingBroadcast
import eu.kanade.tachiyomi.data.notification.openChapterPendingActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get

private const val NOTIF_MAX_CHAPTERS = 5
private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192

/**
 * Shows the notification containing the result of the update done by the service.
 *
 * @param updates a list of manga with new updates.
 */
internal fun LibraryUpdateNotifier.showUpdateNotifications(updates: List<Pair<Manga, Array<Chapter>>>) {
    // Parent group notification
    context.notify(
        Notifications.ID_NEW_CHAPTERS,
        Notifications.CHANNEL_NEW_CHAPTERS,
    ) {
        setContentTitle(context.stringResource(MR.strings.notification_new_chapters))
        if (updates.size == 1 && !securityPreferences.hideNotificationContent.get()) {
            setContentText(updates.first().first.title.chop(NOTIF_TITLE_MAX_LEN))
        } else {
            setContentText(
                context.pluralStringResource(
                    MR.plurals.notification_new_chapters_summary,
                    updates.size,
                    updates.size,
                ),
            )

            if (!securityPreferences.hideNotificationContent.get()) {
                setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        updates.joinToString("\n") {
                            it.first.title.chop(NOTIF_TITLE_MAX_LEN)
                        },
                    ),
                )
            }
        }

        setSmallIcon(R.drawable.ic_tachi)
        setLargeIcon(notificationBitmap)

        setGroup(Notifications.GROUP_NEW_CHAPTERS)
        setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        setGroupSummary(true)
        priority = NotificationCompat.PRIORITY_HIGH

        setContentIntent(getNotificationIntent())
        setAutoCancel(true)
    }

    // Per-manga notification
    if (!securityPreferences.hideNotificationContent.get()) {
        launchUI {
            context.notify(
                updates.map { (manga, chapters) ->
                    NotificationManagerCompat.NotificationWithIdAndTag(
                        manga.id.hashCode(),
                        createNewChaptersNotification(manga, chapters),
                    )
                },
            )
        }
    }
}

internal suspend fun LibraryUpdateNotifier.createNewChaptersNotification(
    manga: Manga,
    chapters: Array<Chapter>,
): Notification {
    val icon = getMangaIcon(manga)
    return context.notificationBuilder(Notifications.CHANNEL_NEW_CHAPTERS) {
        setContentTitle(manga.title)

        val description = getNewChaptersDescription(chapters)
        setContentText(description)
        setStyle(NotificationCompat.BigTextStyle().bigText(description))

        setSmallIcon(R.drawable.ic_tachi)

        if (icon != null) {
            setLargeIcon(icon)
        }

        setGroup(Notifications.GROUP_NEW_CHAPTERS)
        setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        priority = NotificationCompat.PRIORITY_HIGH

        // Open first chapter on tap
        setContentIntent(NotificationReceiver.openChapterPendingActivity(context, manga, chapters.first()))
        setAutoCancel(true)

        // Mark chapters as read action
        addAction(
            R.drawable.ic_done_24dp,
            context.stringResource(MR.strings.action_mark_as_read),
            NotificationReceiver.markAsReadPendingBroadcast(
                context,
                manga,
                chapters,
                Notifications.ID_NEW_CHAPTERS,
            ),
        )
        // View chapters action
        addAction(
            R.drawable.ic_book_24dp,
            context.stringResource(MR.strings.action_view_chapters),
            NotificationReceiver.openChapterPendingActivity(
                context,
                manga,
                Notifications.ID_NEW_CHAPTERS,
            ),
        )
        // Download chapters action
        // Only add the action when chapters is within threshold
        if (chapters.size <= Downloader.CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            addAction(
                android.R.drawable.stat_sys_download_done,
                context.stringResource(MR.strings.action_download),
                NotificationReceiver.downloadChaptersBroadcast(
                    context,
                    manga,
                    chapters,
                    Notifications.ID_NEW_CHAPTERS,
                ),
            )
        }
    }.build()
}

internal suspend fun LibraryUpdateNotifier.getMangaIcon(manga: Manga): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(manga)
        .transformations(CircleCropTransformation())
        .size(NOTIF_ICON_SIZE)
        .build()
    val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources)
    return drawable?.getBitmapOrNull()
}

internal fun LibraryUpdateNotifier.getNewChaptersDescription(chapters: Array<Chapter>): String {
    val displayableChapterNumbers = chapters
        .filter { it.isRecognizedNumber }
        .sortedBy { it.chapterNumber }
        .map { formatChapterNumber(it.chapterNumber) }
        .toSet()

    return when (displayableChapterNumbers.size) {
        // No sensible chapter numbers to show (i.e. no chapters have parsed chapter number)
        0 -> {
            // "1 new chapter" or "5 new chapters"
            context.pluralStringResource(
                MR.plurals.notification_chapters_generic,
                chapters.size,
                chapters.size,
            )
        }
        // Only 1 chapter has a parsed chapter number
        1 -> {
            val remaining = chapters.size - displayableChapterNumbers.size
            if (remaining == 0) {
                // "Chapter 2.5"
                context.stringResource(
                    MR.strings.notification_chapters_single,
                    displayableChapterNumbers.first(),
                )
            } else {
                // "Chapter 2.5 and 10 more"
                context.stringResource(
                    MR.strings.notification_chapters_single_and_more,
                    displayableChapterNumbers.first(),
                    remaining,
                )
            }
        }
        // Everything else (i.e. multiple parsed chapter numbers)
        else -> {
            val shouldTruncate = displayableChapterNumbers.size > NOTIF_MAX_CHAPTERS
            if (shouldTruncate) {
                // "Chapters 1, 2.5, 3, 4, 5 and 10 more"
                val remaining = displayableChapterNumbers.size - NOTIF_MAX_CHAPTERS
                val joinedChapterNumbers = displayableChapterNumbers
                    .take(NOTIF_MAX_CHAPTERS)
                    .joinToString(", ")
                context.pluralStringResource(
                    MR.plurals.notification_chapters_multiple_and_more,
                    remaining,
                    joinedChapterNumbers,
                    remaining,
                )
            } else {
                // "Chapters 1, 2.5, 3"
                context.stringResource(
                    MR.strings.notification_chapters_multiple,
                    displayableChapterNumbers.joinToString(", "),
                )
            }
        }
    }
}
