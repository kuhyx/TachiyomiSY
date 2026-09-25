package eu.kanade.tachiyomi.extension.api

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.shouldBe
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

@RunWith(RobolectricTestRunner::class)
internal class ExtensionUpdateNotifierTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val preferences = SecurityPreferences(InMemoryPreferenceStore())

    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private fun notifier(): ExtensionUpdateNotifier =
        ExtensionUpdateNotifier(context = context, securityPreferences = preferences)

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager.createNotificationChannel(
            NotificationChannel(
                Notifications.CHANNEL_EXTENSIONS_UPDATE,
                "Extension updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun thePromptNamesEveryExtension() {
        notifier().promptUpdates(listOf("One", "Two"))
        val posted = shadowOf(manager).allNotifications.single()
        shadowOf(posted).contentTitle shouldBe "2 extension updates available"
        shadowOf(posted).contentText shouldBe "One, Two"
    }

    @Test
    fun hiddenContentDropsTheNames() {
        preferences.hideNotificationContent.set(true)
        notifier().promptUpdates(listOf("One"))
        val posted = shadowOf(manager).allNotifications.single()
        shadowOf(posted).contentTitle shouldBe "Extension update available"
        shadowOf(posted).contentText shouldBe null
    }

    @Test
    fun dismissingCancelsIt() {
        notifier().promptUpdates(listOf("One"))
        notifier().dismiss()
        shadowOf(manager).allNotifications.isEmpty() shouldBe true
        shadowOf(manager).getNotification(Notifications.ID_UPDATES_TO_EXTS) shouldBe null
    }

    @Test
    fun thePreferencesComeFromInjekt() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
        ExtensionUpdateNotifier(context).promptUpdates(listOf("One"))
        shadowOf(manager).allNotifications.size shouldBe 1
    }
}
