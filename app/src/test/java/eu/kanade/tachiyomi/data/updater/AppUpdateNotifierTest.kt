package eu.kanade.tachiyomi.data.updater

import android.app.NotificationManager
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
internal class AppUpdateNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifier = AppUpdateNotifier(context)

    @Before
    fun setUp() {
        context.allowNotifications()
    }

    private fun actionsOf(id: Int): Int {
        val manager = Shadows.shadowOf(context.getSystemService(NotificationManager::class.java))
        return manager.getNotification(id).actions.size
    }

    @Test
    fun promptUpdateOffersTwoActions() {
        notifier.promptUpdate(testRelease)
        actionsOf(Notifications.ID_APP_UPDATER) shouldBe 2
    }

    @Test
    fun downloadStartOffersCancel() {
        notifier.onDownloadStarted()
        notifier.onDownloadStarted(title = "Title").build().extras.getString("android.title") shouldBe "Title"
        notifier.onProgressChange(40)
        actionsOf(Notifications.ID_APP_UPDATER) shouldBe 1
    }

    @Test
    fun promptInstallUsesItsOwnId() {
        notifier.promptInstall("content://downloads/update.apk".toUri())
        actionsOf(Notifications.ID_APP_UPDATE_PROMPT) shouldBe 2
    }

    @Test
    fun downloadErrorUsesItsOwnId() {
        notifier.onDownloadError("https://example.org/app.apk")
        actionsOf(Notifications.ID_APP_UPDATE_ERROR) shouldBe 2
    }

    @Test
    fun cancelDismissesTheProgress() {
        notifier.onDownloadStarted()
        notifier.cancel()
        context.activeNotificationIds() shouldBe emptyList()
    }
}
