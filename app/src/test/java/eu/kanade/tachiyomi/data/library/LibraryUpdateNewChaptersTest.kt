package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.Looper
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.request.ImageResult
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.manga.model.Manga

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateNewChaptersTest {

    private val harness = LibraryJobHarness()
    private val loader = mockk<ImageLoader>()
    private val result = mockk<ImageResult>()
    private val notifications get() = shadowOf(harness.context.getSystemService(NotificationManager::class.java))
    private val first = libManga(1, title = "A")
    private val second = libManga(2, title = "B")

    @Before
    fun setUp() {
        harness.start()
        every { result.image } returns Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImage()
        coEvery { loader.execute(any()) } returns result
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
        harness.stop()
    }

    private fun notifier() = LibraryUpdateNotifier(harness.context)

    private fun summary(): Notification? = notifications.getNotification(Notifications.ID_NEW_CHAPTERS)

    private fun perManga(manga: Manga): Notification? = notifications.getNotification(manga.id.hashCode())

    private fun chapters(vararg numbers: Double) =
        numbers.mapIndexed { i, n -> libChapter(id = i.toLong(), number = n) }.toTypedArray()

    private fun describe(vararg numbers: Double) = notifier().getNewChaptersDescription(chapters(*numbers))

    @Test
    fun singleUpdateShowsItsTitle() {
        notifier().showUpdateNotifications(listOf(first to chapters(1.0)))
        summary()?.extra(Notification.EXTRA_TEXT) shouldBe "A"
        shadowOf(Looper.getMainLooper()).idle()
        perManga(first)?.extra(Notification.EXTRA_TEXT) shouldBe "Chapter 1"
    }

    @Test
    fun severalUpdatesListTitles() {
        notifier().showUpdateNotifications(listOf(first to chapters(1.0), second to chapters(2.0)))
        summary()?.extra(Notification.EXTRA_TEXT) shouldBe "For 2 entries"
        summary()?.extra(Notification.EXTRA_BIG_TEXT) shouldBe "A\nB"
    }

    @Test
    fun hiddenUpdatesShowACount() {
        harness.securityPreferences.hideNotificationContent.set(true)
        notifier().showUpdateNotifications(listOf(first to chapters(1.0)))
        summary()?.extra(Notification.EXTRA_TEXT) shouldBe "For 1 entry"
        summary()?.extra(Notification.EXTRA_BIG_TEXT) shouldBe null
        shadowOf(Looper.getMainLooper()).idle()
        perManga(first) shouldBe null
    }

    @Test
    fun smallBatchesOfferDownload() = runTest {
        val shown = notifier().createNewChaptersNotification(first, chapters(1.0))
        shown.actions.size shouldBe 3
        shown.getLargeIcon() shouldNotBe null
    }

    @Test
    fun bigBatchesHaveNoDownload() = runTest {
        every { result.image } returns null
        val many = Array(16) { libChapter(id = it.toLong()) }
        val shown = notifier().createNewChaptersNotification(first, many)
        shown.actions.size shouldBe 2
        shown.getLargeIcon() shouldBe null
    }

    @Test
    fun unnumberedChaptersAreCounted() {
        describe(-1.0, -1.0) shouldBe "2 new chapters"
    }

    @Test
    fun oneNumberIsNamed() {
        describe(2.5) shouldBe "Chapter 2.5"
        describe(2.5, -1.0) shouldBe "Chapter 2.5 and 1 more"
    }

    @Test
    fun severalNumbersAreListed() {
        describe(2.0, 1.0, 1.0) shouldBe "Chapters 1, 2"
        describe(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0) shouldBe "Chapters 1, 2, 3, 4, 5 and 2 more"
    }
}
