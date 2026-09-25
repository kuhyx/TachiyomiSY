package eu.kanade.tachiyomi.data.notification

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequest
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverReceiveTest : ReceiverTestBase() {

    private fun startedActivity(): Intent? =
        Shadows.shadowOf(context as Application).nextStartedActivity

    @Test
    fun downloadActionsReachTheManager() {
        receive(NotificationReceiver.ACTION_RESUME_DOWNLOADS)
        receive(NotificationReceiver.ACTION_PAUSE_DOWNLOADS)
        receive(NotificationReceiver.ACTION_CLEAR_DOWNLOADS)
        val downloader = downloadManager.downloader
        verify { downloader.pause() }
        verify { downloader.clearQueue() }
        verify(exactly = 2) { downloader.stop(any()) }
    }

    @Test
    fun cancelActionsStopTheirJobs() {
        receive(NotificationReceiver.ACTION_CANCEL_RESTORE)
        receive(NotificationReceiver.ACTION_CANCEL_SYNC)
        receive(NotificationReceiver.ACTION_CANCEL_LIBRARY_UPDATE)
        receive(NotificationReceiver.ACTION_CANCEL_APP_UPDATE_DOWNLOAD)
        verify { workManager.cancelUniqueWork("BackupRestore") }
        verify(exactly = 2) { workManager.getWorkInfos(any()) }
        verify { workManager.cancelUniqueWork("AppUpdateDownload") }
    }

    @Test
    fun appUpdateNeedsAUrl() {
        receive(NotificationReceiver.ACTION_START_APP_UPDATE)
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        receive(NotificationReceiver.ACTION_START_APP_UPDATE) {
            putExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL, "https://example.org/app.apk")
        }
        verify { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun shareImageStartsAChooser() {
        receive(NotificationReceiver.ACTION_SHARE_IMAGE) {
            putExtra(NotificationReceiver.EXTRA_URI, "content://images/1")
        }
        startedActivity()!!.action shouldBe Intent.ACTION_CHOOSER
    }

    @Test
    fun shareBackupStartsAChooser() {
        receive(receiverAction("SEND_BACKUP")) {
            putExtra(NotificationReceiver.EXTRA_URI, "content://backups/b.proto.gz".toUri())
        }
        val send = startedActivity()!!.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)!!
        send.type shouldBe "application/x-protobuf+gzip"
    }

    @Test
    fun dismissCancelsTheNotification() {
        postNotification(context = context, id = 5)
        receive(receiverAction("ACTION_DISMISS_NOTIFICATION")) {
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, 5)
        }
        activeIds(context) shouldBe emptyList()
    }

    @Test
    fun unknownActionIsIgnored() {
        receive("something.else")
        startedActivity() shouldBe null
    }

    @Test
    fun dismissBroadcastCarriesTheId() {
        val intent = NotificationReceiver.dismissNotificationBroadcast(context = context, notificationId = 9).intent()
        intent.action shouldBe receiverAction("ACTION_DISMISS_NOTIFICATION")
        intent.getIntExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, -1) shouldBe 9
    }
}
