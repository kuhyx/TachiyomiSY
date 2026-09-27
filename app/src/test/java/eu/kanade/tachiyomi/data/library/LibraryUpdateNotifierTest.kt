package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.Constants

/** A source the queue-size warning never counts. */
private class FreeSource :
    Source by mockk(),
    UnmeteredSource

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateNotifierTest {

    private val harness = LibraryJobHarness()
    private val notifications get() = shadowOf(harness.context.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun notifier() = LibraryUpdateNotifier(harness.context)

    private fun posted(id: Int): Notification? = notifications.getNotification(id)

    private fun manyFrom(source: Long) = (1L..61L).map { libraryEntry(libManga(it, source = source)) }

    @Test
    fun progressListsTitles() {
        notifier().showProgressNotification(listOf(libManga(1, title = "One")), current = 1, total = 4)
        val shown = posted(Notifications.ID_LIBRARY_PROGRESS)!!
        shown.extra(Notification.EXTRA_TITLE) shouldBe "Updating library… (25%)"
        shown.extra(Notification.EXTRA_BIG_TEXT) shouldBe "One"
    }

    @Test
    fun hiddenProgressHasNoTitles() {
        harness.securityPreferences.hideNotificationContent.set(true)
        notifier().showProgressNotification(listOf(libManga(1, title = "One")), current = 0, total = 1)
        posted(Notifications.ID_LIBRARY_PROGRESS)!!.extra(Notification.EXTRA_BIG_TEXT) shouldBe null
    }

    @Test
    fun smallQueueHasNoWarning() {
        notifier().showQueueSizeWarningIfNeeded(emptyList())
        notifier().showQueueSizeWarningIfNeeded(manyFrom(1).take(60))
        posted(Notifications.ID_LIBRARY_SIZE_WARNING) shouldBe null
    }

    @Test
    fun unmeteredSourcesAreExempt() {
        every { harness.sourceManager.get(1L) } returns FreeSource()
        notifier().showQueueSizeWarningIfNeeded(manyFrom(1))
        posted(Notifications.ID_LIBRARY_SIZE_WARNING) shouldBe null
    }

    @Test
    fun bigQueueWarns() {
        notifier().showQueueSizeWarningIfNeeded(manyFrom(2).take(3) + manyFrom(1))
        posted(Notifications.ID_LIBRARY_SIZE_WARNING)!!.extra(Notification.EXTRA_TITLE) shouldBe "Warning"
    }

    @Test
    fun errorsNeedAFailure() {
        notifier().showUpdateErrorNotification(0, Uri.EMPTY)
        posted(Notifications.ID_LIBRARY_ERROR) shouldBe null
        notifier().showUpdateErrorNotification(2, Uri.EMPTY)
        posted(Notifications.ID_LIBRARY_ERROR)!!.extra(Notification.EXTRA_TITLE) shouldBe "2 updates failed"
    }

    @Test
    fun cancelDropsProgress() {
        val notifier = notifier()
        notifier.showProgressNotification(emptyList(), current = 0, total = 1)
        notifier.cancelProgressNotification()
        posted(Notifications.ID_LIBRARY_PROGRESS) shouldBe null
    }

    @Test
    fun tapOpensUpdates() {
        val intent: Intent = shadowOf(notifier().getNotificationIntent()).savedIntent
        intent.action shouldBe Constants.SHORTCUT_UPDATES
        notifier().notificationBitmap shouldNotBe null
    }
}
