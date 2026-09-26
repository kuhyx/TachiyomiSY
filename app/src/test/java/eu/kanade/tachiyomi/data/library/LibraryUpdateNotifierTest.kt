package eu.kanade.tachiyomi.data.library

import android.net.Uri
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateNotifierTest : LibraryUpdateTestBase() {

    private val notifier by lazy { LibraryUpdateNotifier(context) }

    private fun bigText(id: Int): CharSequence? = shown(id)?.extras?.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)

    private fun queue(count: Int, source: Long = 1L) = List(count) { libraryManga(manga(it.toLong(), source)) }

    @Test
    fun progressListsTheTitles() {
        notifier.showProgressNotification(listOf(manga(1L), manga(2L)), current = 1, total = 4)
        bigText(Notifications.ID_LIBRARY_PROGRESS).toString() shouldBe "Manga 1\nManga 2"
        shown(Notifications.ID_LIBRARY_PROGRESS)!!.extras.getString(NotificationCompat.EXTRA_TITLE)!! shouldContain "25"
    }

    @Test
    fun hiddenContentHidesTitles() {
        securityPreferences.hideNotificationContent.set(true)
        notifier.showProgressNotification(listOf(manga(1L)), current = 0, total = 1)
        shown(Notifications.ID_LIBRARY_PROGRESS) shouldNotBe null
        bigText(Notifications.ID_LIBRARY_PROGRESS) shouldBe null
    }

    @Test
    fun bigQueuesWarn() {
        every { sourceManager.get(any()) } returns mockk<Source>()
        notifier.showQueueSizeWarningIfNeeded(queue(60) + queue(2, source = 3L))
        shown(Notifications.ID_LIBRARY_SIZE_WARNING) shouldBe null
        notifier.showQueueSizeWarningIfNeeded(queue(2, source = 3L) + queue(61))
        shown(Notifications.ID_LIBRARY_SIZE_WARNING) shouldNotBe null
    }

    @Test
    fun unmeteredQueuesDoNotWarn() {
        every { sourceManager.get(2L) } returns mockk<Source>(moreInterfaces = arrayOf(UnmeteredSource::class))
        notifier.showQueueSizeWarningIfNeeded(queue(61, source = 2L))
        notifier.showQueueSizeWarningIfNeeded(emptyList())
        shown(Notifications.ID_LIBRARY_SIZE_WARNING) shouldBe null
    }

    @Test
    fun errorsAreReported() {
        notifier.showUpdateErrorNotification(0, Uri.EMPTY)
        shown(Notifications.ID_LIBRARY_ERROR) shouldBe null
        notifier.showUpdateErrorNotification(2, Uri.parse("file:///errors.txt"))
        shown(Notifications.ID_LIBRARY_ERROR) shouldNotBe null
    }

    @Test
    fun progressIsCancelled() {
        notifier.showProgressNotification(emptyList(), current = 0, total = 1)
        notifier.cancelProgressNotification()
        shown(Notifications.ID_LIBRARY_PROGRESS) shouldBe null
    }

    @Test
    fun tapOpensTheUpdates() {
        notifier.getNotificationIntent() shouldNotBe null
    }
}
