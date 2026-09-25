package eu.kanade.tachiyomi.util.system

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat.NotificationWithIdAndTag
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class NotificationExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun notificationManagerComesFrom() {
        context.notificationManager shouldBe manager
    }

    @Test
    fun buildsNotificationsWithAnd() {
        context.notify(9, "channel")
        val plain = context.notificationBuilder("channel").build()
        plain.color shouldBe context.getColor(R.color.accent_blue)
        val titled = context.notificationBuilder("channel") { setContentTitle("Title") }.build()
        titled.extras.getString(Notification.EXTRA_TITLE) shouldBe "Title"
    }

    @Test
    fun deniedPermissionPostsNothing() {
        context.notify(1, "channel") { setContentTitle("A") }
        context.notify(2, context.notificationBuilder("channel").build())
        context.notify(listOf(NotificationWithIdAndTag(3, context.notificationBuilder("channel").build())))
        shadowOf(manager).size() shouldBe 0
    }

    @Test
    fun postsAndCancelsNotifications() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.notify(1, "channel") { setContentTitle("A") }
        shadowOf(manager).size() shouldBe 1
        context.notify(2, context.notificationBuilder("channel").build())
        shadowOf(manager).size() shouldBe 2
        context.notify(listOf(NotificationWithIdAndTag(3, context.notificationBuilder("channel").build())))
        shadowOf(manager).size() shouldBe 3
        context.cancelNotification(1)
        shadowOf(manager).size() shouldBe 2
    }

    @Test
    fun buildsChannelsAndGroups() {
        val group = buildNotificationChannelGroup("group") { setName("Group") }
        group.id shouldBe "group"
        group.name shouldBe "Group"
        val channel = buildNotificationChannel("channel", NotificationManager.IMPORTANCE_LOW) { setName("Channel") }
        channel.id shouldBe "channel"
        channel.name shouldBe "Channel"
        channel.importance shouldBe NotificationManager.IMPORTANCE_LOW
    }
}
