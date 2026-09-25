package eu.kanade.tachiyomi.data.updater

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release

internal val testRelease = Release(
    version = "v9.9.9",
    info = "notes",
    releaseLink = "https://example.org/release",
    assets = listOf("https://example.org/TachiyomiSY-universal-release.apk"),
)

/** Robolectric denies POST_NOTIFICATIONS until granted, and `notify` drops the notification. */
internal fun Context.allowNotifications() {
    Shadows.shadowOf(this as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
}

internal fun Context.activeNotificationIds(): List<Int> =
    getSystemService(NotificationManager::class.java).activeNotifications.map { it.id }.sorted()

@RunWith(RobolectricTestRunner::class)
internal class AppUpdateCheckerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val getApplicationRelease: GetApplicationRelease = mockk()
    private val arguments = slot<GetApplicationRelease.Arguments>()

    @Before
    fun setUp() {
        context.allowNotifications()
        startKoin { modules(module { single { getApplicationRelease } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun newUpdatePromptsTheUser() = runTest {
        val update = GetApplicationRelease.Result.NewUpdate(testRelease)
        coEvery { getApplicationRelease.await(capture(arguments)) } returns update
        AppUpdateChecker().checkForUpdate(context) shouldBe update
        arguments.captured.forceCheck shouldBe false
        arguments.captured.repository shouldBe "jobobby04/tachiyomiSY"
        context.activeNotificationIds() shouldBe listOf(Notifications.ID_APP_UPDATER)
    }

    @Test
    fun noUpdateStaysQuiet() = runTest {
        coEvery { getApplicationRelease.await(capture(arguments)) } returns GetApplicationRelease.Result.NoNewUpdate
        AppUpdateChecker().checkForUpdate(context = context, forceCheck = true) shouldBe
            GetApplicationRelease.Result.NoNewUpdate
        arguments.captured.forceCheck shouldBe true
        context.activeNotificationIds() shouldBe emptyList()
    }

    @Test
    fun previewBuildsUseThePreviewRepo() {
        GITHUB_REPO shouldBe "jobobby04/tachiyomiSY"
        mockkStatic("eu.kanade.tachiyomi.util.system.BuildConfigKt")
        every { isPreviewBuildType } returns true
        GITHUB_REPO shouldBe "jobobby04/TachiyomiSYPreview"
    }
}
