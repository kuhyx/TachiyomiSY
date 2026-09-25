package exh.eh

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class EHentaiUpdateNotifierTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val securityPreferences = SecurityPreferences(InMemoryPreferenceStore())
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val manga = Manga.create().copy(ogTitle = "A rather long gallery title that gets chopped for notifications")

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        stopKoin()
        startKoin { modules(module { single { securityPreferences } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun posted(id: Int) = shadowOf(manager).getNotification(id)

    @Test
    fun progressShowsTitleUnlessHidden() {
        val notifier = EHentaiUpdateNotifier(context)
        notifier.progressNotificationBuilder shouldBeSameInstanceAs notifier.progressNotificationBuilder
        notifier.showProgressNotification(manga, current = 1, total = 4)
        val shown = posted(Notifications.ID_EHENTAI_PROGRESS).shouldNotBeNull()
        shown.extras.getString(NotificationCompat.EXTRA_TITLE).shouldNotBeNull() shouldContain "25"
        val bigText = shown.extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).shouldNotBeNull()
        bigText.toString() shouldContain "A rather"
        securityPreferences.hideNotificationContent.set(true)
        EHentaiUpdateNotifier(context).showProgressNotification(manga, current = 2, total = 4)
        val hidden = posted(Notifications.ID_EHENTAI_PROGRESS).shouldNotBeNull()
        hidden.extras.getString(NotificationCompat.EXTRA_TITLE).shouldNotBeNull() shouldContain "50"
    }

    @Test
    fun errorNotificationOnlyOnFailure() {
        val notifier = EHentaiUpdateNotifier(context)
        notifier.showUpdateErrorNotification(0, Uri.parse("file:///log.txt"))
        posted(Notifications.ID_EHENTAI_ERROR).shouldBeNull()
        notifier.showUpdateErrorNotification(3, Uri.parse("file:///log.txt"))
        val shown = posted(Notifications.ID_EHENTAI_ERROR).shouldNotBeNull()
        shown.extras.getString(NotificationCompat.EXTRA_TITLE).shouldNotBeNull() shouldContain "3"
        shown.contentIntent.shouldNotBeNull()
    }

    @Test
    fun cancelRemovesProgress() {
        val notifier = EHentaiUpdateNotifier(context)
        notifier.showProgressNotification(manga, current = 1, total = 1)
        posted(Notifications.ID_EHENTAI_PROGRESS).shouldNotBeNull()
        notifier.cancelProgressNotification()
        posted(Notifications.ID_EHENTAI_PROGRESS).shouldBeNull()
        shadowOf(manager).size() shouldBe 0
    }
}
