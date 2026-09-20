package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.clearQueue
import eu.kanade.tachiyomi.data.download.pauseDownloads
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.manga.interactor.GetManga
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Global [BroadcastReceiver] that runs on UI thread
 * Pending Broadcasts should be made from here.
 * NOTE: Use local broadcasts if possible.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class NotificationReceiver : BroadcastReceiver() {

    internal val getManga: GetManga by injectLazy()
    internal val getChapter: GetChapter by injectLazy()
    internal val updateChapter: UpdateChapter by injectLazy()
    internal val downloadManager: DownloadManager by injectLazy()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Dismiss notification
            ACTION_DISMISS_NOTIFICATION -> {
                dismissNotification(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            }
            // Resume the download service
            ACTION_RESUME_DOWNLOADS -> {
                downloadManager.startDownloads()
            }
            // Pause the download service
            ACTION_PAUSE_DOWNLOADS -> {
                downloadManager.pauseDownloads()
            }
            // Clear the download queue
            ACTION_CLEAR_DOWNLOADS -> {
                downloadManager.clearQueue()
            }
            // Launch share activity and dismiss notification
            ACTION_SHARE_IMAGE -> {
                shareImage(
                    context,
                    intent.getStringExtra(EXTRA_URI)!!.toUri(),
                )
            }
            // Share backup file
            ACTION_SHARE_BACKUP -> {
                shareFile(
                    context,
                    intent.getParcelableExtraCompat(EXTRA_URI)!!,
                    "application/x-protobuf+gzip",
                )
            }
            ACTION_CANCEL_RESTORE -> {
                cancelRestore(context)
            }

            ACTION_CANCEL_SYNC -> {
                cancelSync(context)
            }
            // Cancel library update and dismiss notification
            ACTION_CANCEL_LIBRARY_UPDATE -> {
                cancelLibraryUpdate(context)
            }
            // Start downloading app update
            ACTION_START_APP_UPDATE -> {
                startDownloadAppUpdate(context, intent)
            }
            // Cancel downloading app update
            ACTION_CANCEL_APP_UPDATE_DOWNLOAD -> {
                cancelDownloadAppUpdate(context)
            }
            // Open reader activity
            ACTION_OPEN_CHAPTER -> {
                openChapter(
                    context,
                    intent.getLongExtra(EXTRA_MANGA_ID, -1),
                    intent.getLongExtra(EXTRA_CHAPTER_ID, -1),
                )
            }
            // Mark updated manga chapters as read
            ACTION_MARK_AS_READ -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                if (notificationId > -1) {
                    dismissNotification(context, notificationId, intent.getIntExtra(EXTRA_GROUP_ID, 0))
                }
                val urls = intent.getStringArrayExtra(EXTRA_CHAPTER_URL) ?: return
                val mangaId = intent.getLongExtra(EXTRA_MANGA_ID, -1)
                if (mangaId > -1) {
                    markAsRead(urls, mangaId)
                }
            }
            // Download manga chapters
            ACTION_DOWNLOAD_CHAPTER -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                if (notificationId > -1) {
                    dismissNotification(context, notificationId, intent.getIntExtra(EXTRA_GROUP_ID, 0))
                }
                val urls = intent.getStringArrayExtra(EXTRA_CHAPTER_URL) ?: return
                val mangaId = intent.getLongExtra(EXTRA_MANGA_ID, -1)
                if (mangaId > -1) {
                    downloadChapters(urls, mangaId)
                }
            }
        }
    }

    // Dismiss the notification.
    // @param notificationId the id of the notification
    private fun dismissNotification(context: Context, notificationId: Int) {
        context.cancelNotification(notificationId)
    }

    // Method called when user wants to stop a backup restore job.
    // @param context context of application
    private fun cancelRestore(context: Context) {
        BackupRestoreJob.stop(context)
    }

    // Method called when user wants to stop a library update.
    // @param context context of application
    private fun cancelLibraryUpdate(context: Context) {
        LibraryUpdateJob.stop(context)
    }

    private fun startDownloadAppUpdate(context: Context, intent: Intent) {
        val url = intent.getStringExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL) ?: return
        AppUpdateDownloadJob.start(context, url)
    }

    private fun cancelDownloadAppUpdate(context: Context) {
        AppUpdateDownloadJob.stop(context)
    }

    // Method called when user wants to stop a backup restore job.
    // @param context context of application
    private fun cancelSync(context: Context) {
        SyncDataJob.stop(context)
    }

    companion object {
        private const val NAME = "NotificationReceiver"

        internal const val ACTION_SHARE_IMAGE = "$ID.$NAME.SHARE_IMAGE"

        private const val ACTION_SHARE_BACKUP = "$ID.$NAME.SEND_BACKUP"

        internal const val ACTION_CANCEL_RESTORE = "$ID.$NAME.CANCEL_RESTORE"

        internal const val ACTION_CANCEL_SYNC = "$ID.$NAME.CANCEL_SYNC"

        internal const val ACTION_CANCEL_LIBRARY_UPDATE = "$ID.$NAME.CANCEL_LIBRARY_UPDATE"

        internal const val ACTION_START_APP_UPDATE = "$ID.$NAME.ACTION_START_APP_UPDATE"
        internal const val ACTION_CANCEL_APP_UPDATE_DOWNLOAD = "$ID.$NAME.CANCEL_APP_UPDATE_DOWNLOAD"

        internal const val ACTION_MARK_AS_READ = "$ID.$NAME.MARK_AS_READ"
        private const val ACTION_OPEN_CHAPTER = "$ID.$NAME.ACTION_OPEN_CHAPTER"
        internal const val ACTION_DOWNLOAD_CHAPTER = "$ID.$NAME.ACTION_DOWNLOAD_CHAPTER"

        internal const val ACTION_RESUME_DOWNLOADS = "$ID.$NAME.ACTION_RESUME_DOWNLOADS"
        internal const val ACTION_PAUSE_DOWNLOADS = "$ID.$NAME.ACTION_PAUSE_DOWNLOADS"
        internal const val ACTION_CLEAR_DOWNLOADS = "$ID.$NAME.ACTION_CLEAR_DOWNLOADS"

        private const val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.ACTION_DISMISS_NOTIFICATION"

        internal const val EXTRA_URI = "$ID.$NAME.URI"
        internal const val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"
        internal const val EXTRA_GROUP_ID = "$ID.$NAME.EXTRA_GROUP_ID"
        internal const val EXTRA_MANGA_ID = "$ID.$NAME.EXTRA_MANGA_ID"
        private const val EXTRA_CHAPTER_ID = "$ID.$NAME.EXTRA_CHAPTER_ID"
        internal const val EXTRA_CHAPTER_URL = "$ID.$NAME.EXTRA_CHAPTER_URL"

        /**
         * Returns [PendingIntent] that starts a service which dismissed the notification.
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun dismissNotificationBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
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
         * Dismisses the notification, and its summary once the group is empty.
         *
         * @param context context of application
         * @param notificationId id of notification
         * @param groupId id of the notification group, if any
         */
        internal fun dismissNotification(context: Context, notificationId: Int, groupId: Int? = null) {
            /*
            Group notifications always have at least 2 notifications:
            - Group summary notification
            - Single manga notification

            If the single notification is dismissed by the system, ie by a user swipe or tapping on the notification,
            it will auto dismiss the group notification if there's no other single updates.

            When programmatically dismissing this notification, the group notification is not automatically dismissed.
             */
            val groupKey = context.notificationManager.activeNotifications.find {
                it.id == notificationId
            }?.groupKey

            if (groupId != null && groupId != 0 && !groupKey.isNullOrEmpty()) {
                val notifications = context.notificationManager.activeNotifications.filter {
                    it.groupKey == groupKey
                }

                if (notifications.size == 2) {
                    context.cancelNotification(groupId)
                    return
                }
            }

            context.cancelNotification(notificationId)
        }
    }
}
