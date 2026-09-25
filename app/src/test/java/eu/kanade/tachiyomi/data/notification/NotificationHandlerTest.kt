package eu.kanade.tachiyomi.data.notification

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.core.common.Constants

@RunWith(RobolectricTestRunner::class)
internal class NotificationHandlerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun savedIntent(pendingIntent: android.app.PendingIntent): Intent =
        Shadows.shadowOf(pendingIntent).savedIntent

    @Test
    fun downloadIntentTargetsMain() {
        val intent = savedIntent(NotificationHandler.openDownloadManagerActivity(context))
        intent.action shouldBe Constants.SHORTCUT_DOWNLOADS
        intent.component!!.className shouldBe "eu.kanade.tachiyomi.ui.main.MainActivity"
    }

    @Test
    fun downloadIntentClearsTop() {
        val intent = savedIntent(NotificationHandler.openDownloadManagerActivity(context))
        val expected = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.flags shouldBe expected
    }

    @Test
    fun imageIntentViewsTheUri() {
        val uri = "content://images/1".toUri()
        val intent = savedIntent(NotificationHandler.openImagePendingActivity(context = context, uri = uri))
        intent.action shouldBe Intent.ACTION_VIEW
        intent.data shouldBe uri
        intent.type shouldBe "image/*"
    }

    @Test
    fun imageIntentGrantsRead() {
        val uri = "content://images/2".toUri()
        val intent = savedIntent(NotificationHandler.openImagePendingActivity(context = context, uri = uri))
        val expected = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.flags shouldBe expected
    }

    @Test
    fun apkIntentUsesApkMimeType() {
        val uri = "content://downloads/app.apk".toUri()
        val intent = savedIntent(NotificationHandler.installApkPendingActivity(context = context, uri = uri))
        intent.action shouldBe Intent.ACTION_VIEW
        intent.data shouldBe uri
        intent.type shouldBe "application/vnd.android.package-archive"
    }

    @Test
    fun urlIntentViewsTheUrl() {
        val intent = savedIntent(NotificationHandler.openUrl(context = context, url = "https://example.org/x"))
        intent.action shouldBe Intent.ACTION_VIEW
        intent.data.toString() shouldBe "https://example.org/x"
    }
}
