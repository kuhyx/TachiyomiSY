package eu.kanade.tachiyomi.data.library

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.Downloader
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.notification.cancelLibraryUpdateBroadcast
import eu.kanade.tachiyomi.data.notification.openErrorLogPendingActivity
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.math.RoundingMode
import java.text.NumberFormat

private const val TITLE_CHARS = 40

@OptIn(DelicateCoroutinesApi::class)
internal class LibraryUpdateNotifier(
    internal val context: Context,

    internal val securityPreferences: SecurityPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    // Pending intent of action that cancels the library update.
    private val cancelIntent by lazy {
        NotificationReceiver.cancelLibraryUpdateBroadcast(context)
    }

    // Bitmap of the app for notifications.
    internal val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setContentTitle(context.stringResource(MR.strings.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_24dp, context.stringResource(MR.strings.action_cancel), cancelIntent)
        }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that are being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(manga: List<Manga>, current: Int, total: Int) {
        progressNotificationBuilder
            .setContentTitle(
                context.stringResource(
                    MR.strings.notification_updating_progress,
                    percentFormatter.format(current.toFloat() / total),
                ),
            )

        if (!securityPreferences.hideNotificationContent.get()) {
            val updatingText = manga.joinToString("\n") { it.title.chop(TITLE_CHARS) }
            progressNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))
        }

        context.notify(
            Notifications.ID_LIBRARY_PROGRESS,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    /**
     * Warn when excessively checking any single source.
     */
    fun showQueueSizeWarningIfNeeded(mangaToUpdate: List<LibraryManga>) {
        val maxUpdatesFromSource = mangaToUpdate
            .groupBy { it.manga.source }
            .filterKeys { sourceManager.get(it) !is UnmeteredSource }
            .maxOfOrNull { it.value.size }
            ?: 0

        if (maxUpdatesFromSource <= MANGA_PER_SOURCE_QUEUE_WARNING_THRESHOLD) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_SIZE_WARNING,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
        ) {
            setContentTitle(context.stringResource(MR.strings.label_warning))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(context.stringResource(MR.strings.notification_size_warning)),
            )
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setTimeoutAfter(Downloader.WARNING_NOTIF_TIMEOUT_MS)
            setContentIntent(NotificationHandler.openUrl(context, HELP_WARNING_URL))
        }
    }

    /**
     * Shows notification containing update entries that failed with action to open full log.
     *
     * @param failed Number of entries that failed to update.
     * @param uri Uri for error log file containing all titles that failed.
     */
    fun showUpdateErrorNotification(failed: Int, uri: Uri) {
        if (failed == 0) {
            return
        }

        context.notify(
            Notifications.ID_LIBRARY_ERROR,
            Notifications.CHANNEL_LIBRARY_ERROR,
        ) {
            setContentTitle(context.pluralStringResource(MR.plurals.notification_update_error, failed, failed))
            setContentText(context.stringResource(MR.strings.action_show_errors))
            setSmallIcon(R.drawable.ic_tachi)

            setContentIntent(NotificationReceiver.openErrorLogPendingActivity(context, uri))
        }
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        context.cancelNotification(Notifications.ID_LIBRARY_PROGRESS)
    }

    // Returns an intent to open the main activity.
    internal fun getNotificationIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Constants.SHORTCUT_UPDATES
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val HELP_WARNING_URL =
            "https://mihon.app/docs/faq/library#why-am-i-warned-about-large-bulk-updates-and-downloads"
    }
}

private const val MANGA_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60
