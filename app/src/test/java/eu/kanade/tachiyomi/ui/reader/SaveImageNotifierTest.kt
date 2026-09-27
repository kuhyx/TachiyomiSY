package eu.kanade.tachiyomi.ui.reader

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.request.ImageRequest
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The notification shown after a page is saved, or when saving it failed. */
@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SaveImageNotifierTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val uri = Uri.parse("content://pages/1")

    @Before
    fun setUp() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    @After
    fun tearDown() = SingletonImageLoader.reset()

    // A loader that answers every request at once, with [image] or with an error when it is null.
    private fun loadAs(image: Image?) {
        val loader = mockk<ImageLoader>(relaxed = true)
        every { loader.enqueue(any()) } answers {
            val request = firstArg<ImageRequest>()
            if (image != null) request.target?.onSuccess(image) else request.target?.onError(null)
            mockk(relaxed = true)
        }
        SingletonImageLoader.setUnsafe(loader)
    }

    private fun title(): CharSequence? =
        manager.allNotifications.single().extras.getCharSequence(NotificationCompat.EXTRA_TITLE)

    @Test
    fun savedBitmapIsPreviewed() {
        loadAs(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImage())
        SaveImageNotifier(app).onComplete(uri)
        title() shouldBe "Picture saved"
        manager.allNotifications.single().actions.size shouldBe 1
    }

    @Test
    fun nonBitmapHasNoPreview() {
        loadAs(ColorDrawable().asImage())
        SaveImageNotifier(app).onComplete(uri)
        title() shouldBe "Picture saved"
    }

    @Test
    fun loadFailureReportsError() {
        loadAs(null)
        SaveImageNotifier(app).onComplete(uri)
        title() shouldBe "Error"
        manager.allNotifications.single().extras.getCharSequence(NotificationCompat.EXTRA_TEXT) shouldBe
            "Unknown error"
    }

    @Test
    fun errorMessageAndClear() {
        val notifier = SaveImageNotifier(app)
        notifier.onError("disk full")
        manager.allNotifications.single().extras.getCharSequence(NotificationCompat.EXTRA_TEXT) shouldBe
            "disk full"
        notifier.onClear()
        manager.allNotifications.size shouldBe 0
    }
}
