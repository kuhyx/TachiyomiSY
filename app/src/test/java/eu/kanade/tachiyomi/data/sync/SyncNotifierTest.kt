package eu.kanade.tachiyomi.data.sync

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class SyncNotifierTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val securityPreferences = SecurityPreferences(InMemoryPreferenceStore())
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        startKoin { modules(module { single { securityPreferences } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun posted(id: Int): Notification? = shadowOf(manager).getNotification(id)

    private fun Notification.text(key: String): String? = extras.getCharSequence(key)?.toString()

    @Test
    fun progressWithDefaults() {
        SyncNotifier(context).showSyncProgress()
        val shown = posted(Notifications.ID_RESTORE_PROGRESS).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Syncing library"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe ""
        shown.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX) shouldBe 100
        shown.actions.single().title.toString() shouldBe "Cancel"
        shown.channelId shouldBe Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS
    }

    @Test
    fun progressHidesContent() {
        securityPreferences.hideNotificationContent.set(true)
        val builder = SyncNotifier(context).showSyncProgress(content = "secret", progress = 3, maxAmount = 7)
        builder.build().extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX) shouldBe 7
        posted(Notifications.ID_RESTORE_PROGRESS).shouldNotBeNull().text(NotificationCompat.EXTRA_TEXT).shouldBeNull()
    }

    @Test
    fun progressShowsContent() {
        SyncNotifier(context).showSyncProgress(content = "manga")
        posted(Notifications.ID_RESTORE_PROGRESS).shouldNotBeNull().text(NotificationCompat.EXTRA_TEXT) shouldBe "manga"
    }

    @Test
    fun errorReplacesProgress() {
        val notifier = SyncNotifier(context)
        notifier.showSyncProgress()
        notifier.showSyncError("broken")
        posted(Notifications.ID_RESTORE_PROGRESS).shouldBeNull()
        val shown = posted(Notifications.ID_RESTORE_COMPLETE).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Syncing library failed"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe "broken"
        shown.channelId shouldBe Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE
    }

    @Test
    fun successReplacesProgress() {
        val notifier = SyncNotifier(context)
        notifier.showSyncProgress()
        notifier.showSyncSuccess("done")
        posted(Notifications.ID_RESTORE_PROGRESS).shouldBeNull()
        val shown = posted(Notifications.ID_RESTORE_COMPLETE).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Syncing library complete"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe "done"
    }
}
