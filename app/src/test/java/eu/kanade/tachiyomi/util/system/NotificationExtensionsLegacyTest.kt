package eu.kanade.tachiyomi.util.system

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat.NotificationWithIdAndTag
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Before Android 13 posting a notification needs no runtime permission. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
internal class NotificationExtensionsLegacyTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun postsWithoutAPermission() {
        context.notify(1, context.notificationBuilder("channel").build())
        context.notify(listOf(NotificationWithIdAndTag(2, context.notificationBuilder("channel").build())))
        shadowOf(manager).size() shouldBe 2
    }
}
