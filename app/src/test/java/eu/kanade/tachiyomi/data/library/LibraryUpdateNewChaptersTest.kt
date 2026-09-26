package eu.kanade.tachiyomi.data.library

import android.graphics.Bitmap
import android.os.Looper
import androidx.core.app.NotificationCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.domain.manga.model.Manga

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateNewChaptersTest : LibraryUpdateTestBase() {

    private val notifier by lazy { LibraryUpdateNotifier(context) }

    @After
    fun resetLoader() {
        SingletonImageLoader.reset()
    }

    private fun coversLoad(ok: Boolean) {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val loader = ImageLoader.Builder(context).components {
            add(
                Fetcher.Factory<Manga> { _, _, _ ->
                    Fetcher {
                        check(ok) { "no cover" }
                        ImageFetchResult(bitmap.asImage(), isSampled = false, dataSource = DataSource.MEMORY)
                    }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    private fun describe(vararg numbers: Double): String =
        notifier.getNewChaptersDescription(numbers.mapIndexed { i, n -> chapter(i.toLong(), n) }.toTypedArray())

    private fun awaitShown(id: Int) {
        repeat(200) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (shown(id) != null) return
            Thread.sleep(20)
        }
    }

    @Test
    fun descriptionsByNumbers() {
        describe(-1.0, -1.0) shouldBe "2 new chapters"
        describe(2.5) shouldBe "Chapter 2.5"
        describe(2.5, -1.0, -1.0) shouldBe "Chapter 2.5 and 2 more"
        describe(3.0, 1.0) shouldBe "Chapters 1, 3"
        describe(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0) shouldBe "Chapters 1, 2, 3, 4, 5 and 2 more"
    }

    @Test
    fun oneUpdateNamesTheManga() {
        coversLoad(ok = true)
        val one = manga(1L, title = "Only")
        notifier.showUpdateNotifications(listOf(one to arrayOf(chapter(1L))))
        val summary = shown(Notifications.ID_NEW_CHAPTERS)!!
        summary.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString() shouldBe "Only"
        awaitShown(one.id.hashCode())
        shown(one.id.hashCode())!!.getLargeIcon() shouldNotBe null
    }

    @Test
    fun manyUpdatesAreSummarised() {
        coversLoad(ok = false)
        val updates = listOf(manga(1L) to arrayOf(chapter(1L)), manga(2L) to arrayOf(chapter(2L)))
        notifier.showUpdateNotifications(updates)
        val summary = shown(Notifications.ID_NEW_CHAPTERS)!!
        summary.extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).toString() shouldBe "Manga 1\nManga 2"
        awaitShown(2L.hashCode())
        shown(2L.hashCode())!!.getLargeIcon() shouldBe null
    }

    @Test
    fun hiddenContentOnlySummarises() {
        securityPreferences.hideNotificationContent.set(true)
        notifier.showUpdateNotifications(listOf(manga(1L) to arrayOf(chapter(1L))))
        val summary = shown(Notifications.ID_NEW_CHAPTERS)!!
        summary.extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT) shouldBe null
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        shown(1L.hashCode()) shouldBe null
    }

    @Test
    fun bigBatchesCannotDownload() = runTest {
        coversLoad(ok = false)
        val few = notifier.createNewChaptersNotification(manga(1L), Array(15) { chapter(it.toLong()) })
        few.actions.size shouldBe 3
        val many = notifier.createNewChaptersNotification(manga(1L), Array(16) { chapter(it.toLong()) })
        many.actions.size shouldBe 2
    }
}
