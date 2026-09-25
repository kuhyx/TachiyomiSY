package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.core.common.Constants

internal fun PendingIntent.intent(): Intent = Shadows.shadowOf(this).savedIntent

internal const val RECEIVER = "eu.kanade.tachiyomi.data.notification.NotificationReceiver"

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverActionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun shareImageCarriesTheUri() {
        val intent = NotificationReceiver.shareImagePendingBroadcast(
            context = context,
            uri = "content://images/9".toUri(),
        ).intent()
        intent.component!!.className shouldBe RECEIVER
        intent.action shouldBe NotificationReceiver.ACTION_SHARE_IMAGE
        intent.getStringExtra(NotificationReceiver.EXTRA_URI) shouldBe "content://images/9"
    }

    @Test
    fun cancelLibraryUpdateHasNoExtras() {
        val intent = NotificationReceiver.cancelLibraryUpdateBroadcast(context).intent()
        intent.action shouldBe NotificationReceiver.ACTION_CANCEL_LIBRARY_UPDATE
        intent.extras shouldBe null
    }

    @Test
    fun openExtensionsTargetsMain() {
        val intent = NotificationReceiver.openExtensionsPendingActivity(context).intent()
        intent.action shouldBe Constants.SHORTCUT_EXTENSIONS
        intent.component!!.className shouldBe "eu.kanade.tachiyomi.ui.main.MainActivity"
        (intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) shouldBe Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    @Test
    fun shareBackupSendsTheFile() {
        val intent = NotificationReceiver.shareBackupPendingActivity(
            context = context,
            uri = "content://backups/b.proto.gz".toUri(),
        ).intent()
        // `toShareIntent` wraps the send intent in a chooser.
        intent.action shouldBe Intent.ACTION_CHOOSER
        (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
        val send = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)!!
        send.action shouldBe Intent.ACTION_SEND
        send.type shouldBe "application/x-protobuf+gzip"
    }

    @Test
    fun openErrorLogViewsPlainText() {
        val uri = "content://logs/error.txt".toUri()
        val intent = NotificationReceiver.openErrorLogPendingActivity(context = context, uri = uri).intent()
        intent.action shouldBe Intent.ACTION_VIEW
        intent.data shouldBe uri
        intent.type shouldBe "text/plain"
    }

    @Test
    fun cancelRestoreCarriesTheId() {
        val intent = NotificationReceiver.cancelRestorePendingBroadcast(
            context = context,
            notificationId = 31,
        ).intent()
        intent.action shouldBe NotificationReceiver.ACTION_CANCEL_RESTORE
        intent.getIntExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, -1) shouldBe 31
    }

    @Test
    fun cancelSyncCarriesTheId() {
        val intent = NotificationReceiver.cancelSyncPendingBroadcast(
            context = context,
            notificationId = 44,
        ).intent()
        intent.action shouldBe NotificationReceiver.ACTION_CANCEL_SYNC
        intent.getIntExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, -1) shouldBe 44
    }
}
