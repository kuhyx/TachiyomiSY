package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NotificationsTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManagerCompat

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = NotificationManagerCompat.from(context)
        Notifications.createChannels(context)
    }

    @Test
    fun everyChannelIsCreated() {
        val ids = manager.notificationChannelsCompat.map { it.id }
        val expected = listOf(
            Notifications.CHANNEL_COMMON,
            Notifications.CHANNEL_LIBRARY_PROGRESS,
            Notifications.CHANNEL_LIBRARY_ERROR,
            Notifications.CHANNEL_NEW_CHAPTERS,
            Notifications.CHANNEL_DOWNLOADER_PROGRESS,
            Notifications.CHANNEL_DOWNLOADER_ERROR,
            Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS,
            Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE,
            Notifications.CHANNEL_INCOGNITO_MODE,
            Notifications.CHANNEL_APP_UPDATE,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
            Notifications.CHANNEL_LIBRARY_EHENTAI,
        )
        expected.all { it in ids } shouldBe true
    }

    @Test
    fun groupedChannelsKeepTheirGroup() {
        val library = manager.getNotificationChannelCompat(Notifications.CHANNEL_LIBRARY_ERROR)
        library.shouldNotBeNull()
        library.group shouldBe "group_library"
        val download = manager.getNotificationChannelCompat(Notifications.CHANNEL_DOWNLOADER_ERROR)
        download.shouldNotBeNull()
        download.group shouldBe "group_downloader"
    }

    @Test
    fun ungroupedChannelHasNoGroup() {
        val common = manager.getNotificationChannelCompat(Notifications.CHANNEL_COMMON)
        common.shouldNotBeNull()
        common.group shouldBe null
    }

    @Test
    fun literalNameIsUsedAsIs() {
        val ehentai = manager.getNotificationChannelCompat(Notifications.CHANNEL_LIBRARY_EHENTAI)
        ehentai.shouldNotBeNull()
        ehentai.name shouldBe "EHentai"
    }

    @Test
    fun resourceNamesAreResolved() {
        val common = manager.getNotificationChannelCompat(Notifications.CHANNEL_COMMON)
        common.shouldNotBeNull()
        common.name.isNullOrBlank() shouldBe false
    }

    @Test
    fun badgeAndSilenceFollowTheSpec() {
        val progress = manager.getNotificationChannelCompat(Notifications.CHANNEL_LIBRARY_PROGRESS)
        progress.shouldNotBeNull()
        progress.canShowBadge() shouldBe false
        val complete = manager.getNotificationChannelCompat(Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE)
        complete.shouldNotBeNull()
        complete.sound shouldBe null
        manager.getNotificationChannelCompat(Notifications.CHANNEL_COMMON)!!.canShowBadge() shouldBe true
    }

    @Test
    fun fourGroupsAreCreated() {
        manager.notificationChannelGroupsCompat.size shouldBe 4
        manager.notificationChannelGroupsCompat.all { it.name.isNullOrBlank().not() } shouldBe true
    }

    @Test
    fun deprecatedChannelsAreDeleted() {
        val ids = manager.notificationChannelsCompat.map { it.id }
        ("downloader_channel" in ids) shouldBe false
        ("crash_logs_channel" in ids) shouldBe false
    }

    @Test
    fun idsAreStable() {
        Notifications.ID_DOWNLOAD_IMAGE shouldBe 2
        Notifications.ID_LIBRARY_PROGRESS shouldBe -101
        Notifications.ID_NEW_CHAPTERS shouldBe -301
        Notifications.GROUP_NEW_CHAPTERS shouldBe "eu.kanade.tachiyomi.NEW_CHAPTERS"
    }
}
