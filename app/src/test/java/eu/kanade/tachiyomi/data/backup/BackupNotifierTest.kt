package eu.kanade.tachiyomi.data.backup

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.storage.getUriCompat
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import tachiyomi.core.common.storage.displayablePath
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class BackupNotifierTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val securityPreferences = SecurityPreferences(InMemoryPreferenceStore())
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier by lazy { BackupNotifier(context) }

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        startKoin { modules(module { single { securityPreferences } }) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun posted(id: Int): Notification? = shadowOf(manager).getNotification(id)

    private fun Notification.text(key: String): String? = extras.getCharSequence(key)?.toString()

    private fun complete(): Notification = posted(Notifications.ID_RESTORE_COMPLETE).shouldNotBeNull()

    private fun Notification.actionCount(): Int = actions?.size ?: 0

    @Test
    fun backupProgressThenError() {
        notifier.showBackupProgress()
        posted(Notifications.ID_BACKUP_PROGRESS).shouldNotBeNull().text(NotificationCompat.EXTRA_TITLE) shouldBe
            "Creating backup"
        notifier.showBackupError("disk full")
        posted(Notifications.ID_BACKUP_PROGRESS).shouldBeNull()
        val shown = posted(Notifications.ID_BACKUP_COMPLETE).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Backup failed"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe "disk full"
    }

    @Test
    fun backupCompleteOffersSharing() {
        val file = UniFile.fromFile(File(context.cacheDir, "b.tachibk").apply { writeText("x") }).shouldNotBeNull()
        notifier.showBackupComplete(file)
        val shown = posted(Notifications.ID_BACKUP_COMPLETE).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Backup created"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe file.displayablePath
        shown.actions.single().title.toString() shouldBe "Share"
    }

    @Test
    fun restoreProgressDefaults() {
        notifier.showRestoreProgress()
        val shown = posted(Notifications.ID_RESTORE_PROGRESS).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Restoring backup"
        shown.text(NotificationCompat.EXTRA_TEXT) shouldBe ""
        shown.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX) shouldBe 100
        shown.actions.single().title.toString() shouldBe "Cancel"
    }

    @Test
    fun syncProgressHidesContent() {
        securityPreferences.hideNotificationContent.set(true)
        notifier.showRestoreProgress(content = "Title", progress = 2, maxAmount = 9, sync = true)
        val shown = posted(Notifications.ID_RESTORE_PROGRESS).shouldNotBeNull()
        shown.text(NotificationCompat.EXTRA_TITLE) shouldBe "Syncing library"
        shown.text(NotificationCompat.EXTRA_TEXT).shouldBeNull()
        shown.extras.getInt(NotificationCompat.EXTRA_PROGRESS) shouldBe 2
    }

    @Test
    fun restoreErrorReplacesProgress() {
        notifier.showRestoreProgress(content = "Manga")
        notifier.showRestoreError("broken")
        posted(Notifications.ID_RESTORE_PROGRESS).shouldBeNull()
        complete().text(NotificationCompat.EXTRA_TITLE) shouldBe "Restoring backup failed"
        complete().text(NotificationCompat.EXTRA_TEXT) shouldBe "broken"
    }

    @Test
    fun cleanRestoreHasNoLog() {
        notifier.showRestoreComplete(time = 65_000, errorCount = 0, path = null, file = null, sync = false)
        complete().text(NotificationCompat.EXTRA_TITLE) shouldBe "Restore completed"
        complete().text(NotificationCompat.EXTRA_TEXT) shouldBe "Done in 01 min, 05 sec with 0 errors"
        complete().actionCount() shouldBe 0
    }

    @Test
    fun failedSyncLinksTheLog() {
        // FileProvider caches its roots per authority for the whole sandbox; a real lookup here would
        // pin them to this test's data dir and break later FileProvider tests.
        mockkStatic("eu.kanade.tachiyomi.util.storage.FileExtensionsKt")
        every { any<File>().getUriCompat(any()) } returns Uri.parse("content://logs/mihon_restore_error.txt")
        val log = File(context.cacheDir, "mihon_restore_error.txt").apply { writeText("e") }
        notifier.showRestoreComplete(time = 1_000, errorCount = 1, path = log.parent, file = log.name, sync = true)
        complete().text(NotificationCompat.EXTRA_TITLE) shouldBe "Library sync complete"
        complete().text(NotificationCompat.EXTRA_TEXT) shouldBe "Done in 00 min, 01 sec with 1 error"
        complete().actions.single().title.toString() shouldBe "Tap to see details"
        complete().contentIntent.shouldNotBeNull()
    }

    @Test
    fun errorsWithoutALogFile() {
        notifier.showRestoreComplete(time = 0, errorCount = 2, path = null, file = "log.txt", sync = false)
        complete().actionCount() shouldBe 0
        notifier.showRestoreComplete(time = 0, errorCount = 2, path = "", file = "log.txt", sync = false)
        complete().actionCount() shouldBe 0
        notifier.showRestoreComplete(time = 0, errorCount = 2, path = context.cacheDir.path, file = null, sync = false)
        complete().actionCount() shouldBe 0
        notifier.showRestoreComplete(time = 0, errorCount = 2, path = context.cacheDir.path, file = "", sync = false)
        complete().actionCount() shouldBe 0
    }
}
