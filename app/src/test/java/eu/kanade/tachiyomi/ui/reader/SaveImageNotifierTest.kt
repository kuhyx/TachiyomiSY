package eu.kanade.tachiyomi.ui.reader

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import java.io.IOException

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SaveImageNotifierTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val notifier = SaveImageNotifier(app)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val loader = ImageLoader.Builder(app).components {
            add(
                Fetcher.Factory<coil3.Uri> { data, _, _ ->
                    Fetcher {
                        val image = when (data.path) {
                            "/bitmap.png" -> Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImage()
                            "/plain.png" -> ColorDrawable(0).asImage()
                            else -> throw IOException("missing")
                        }
                        ImageFetchResult(image, isSampled = false, dataSource = DataSource.MEMORY)
                    }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
    }

    private fun shown() = shadowOf(manager).getNotification(Notifications.ID_DOWNLOAD_IMAGE)

    private fun complete(path: String) {
        notifier.onComplete(Uri.parse("file://$path"))
        repeat(LOAD_ROUNDS) {
            Thread.sleep(LOAD_WAIT_MILLIS)
            ShadowLooper.idleMainLooper()
        }
    }

    @Test
    fun errorsAreNotified() {
        notifier.onError(null)
        shown().extras.getString(NotificationCompat.EXTRA_TEXT).shouldNotBeNull()
        notifier.onError("full")
        shown().extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString() shouldBe "full"
        notifier.onClear()
        shown().shouldBeNull()
    }

    @Test
    fun savedImageIsPreviewed() {
        complete("/bitmap.png")
        shown().extras.getParcelable(NotificationCompat.EXTRA_PICTURE, Bitmap::class.java).shouldNotBeNull()
        complete("/plain.png")
        shown().actions.size shouldBe 1
        complete("/missing.png")
        shown().extras.getCharSequence(NotificationCompat.EXTRA_TEXT).shouldNotBeNull()
    }

    private companion object {
        const val LOAD_ROUNDS = 10
        const val LOAD_WAIT_MILLIS = 20L
    }
}
