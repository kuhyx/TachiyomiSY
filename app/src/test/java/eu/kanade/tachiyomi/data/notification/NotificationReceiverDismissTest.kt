package eu.kanade.tachiyomi.data.notification

import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverDismissTest : ReceiverTestBase() {

    @Test
    fun ungroupedCancelsOnlyItself() {
        postNotification(context = context, id = 1)
        postNotification(context = context, id = 2)
        NotificationReceiver.dismissNotification(context = context, notificationId = 1)
        activeIds(context) shouldBe listOf(2)
    }

    @Test
    fun lastInGroupCancelsSummary() {
        postNotification(context = context, id = 10, group = "g")
        postNotification(context = context, id = 11, group = "g")
        NotificationReceiver.dismissNotification(context = context, notificationId = 11, groupId = 10)
        activeIds(context) shouldBe listOf(11)
    }

    @Test
    fun busyGroupKeepsSummary() {
        postNotification(context = context, id = 10, group = "g")
        postNotification(context = context, id = 11, group = "g")
        postNotification(context = context, id = 12, group = "g")
        NotificationReceiver.dismissNotification(context = context, notificationId = 11, groupId = 10)
        activeIds(context) shouldBe listOf(10, 12)
    }

    @Test
    fun zeroGroupIsNoGroup() {
        postNotification(context = context, id = 10, group = "g")
        postNotification(context = context, id = 11, group = "g")
        NotificationReceiver.dismissNotification(context = context, notificationId = 11, groupId = 0)
        activeIds(context) shouldBe listOf(10)
    }

    @Test
    fun unknownNotificationHasNoGroup() {
        postNotification(context = context, id = 10, group = "g")
        NotificationReceiver.dismissNotification(context = context, notificationId = 99, groupId = 10)
        activeIds(context) shouldBe listOf(10)
    }

    /** Android never reports an empty group key; a stubbed manager can, and it counts as no group. */
    @Test
    fun emptyGroupKeyIsNoGroup() {
        val active = mockk<StatusBarNotification>()
        every { active.id } returns 11
        every { active.groupKey } returns ""
        val manager = mockk<NotificationManager>(relaxed = true)
        every { manager.activeNotifications } returns arrayOf(active)
        val stubbed = mockk<Context>(relaxed = true)
        every { stubbed.getSystemService(NotificationManager::class.java) } returns manager
        every { stubbed.getSystemService(Context.NOTIFICATION_SERVICE) } returns manager
        NotificationReceiver.dismissNotification(context = stubbed, notificationId = 11, groupId = 10)
        verify { manager.cancel(null, 11) }
    }
}
