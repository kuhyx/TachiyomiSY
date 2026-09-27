package eu.kanade.tachiyomi.extension.util

import android.app.Service
import android.content.Intent
import android.net.Uri
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.installer.PackageInstallerInstaller
import eu.kanade.tachiyomi.extension.installer.ShizukuInstaller
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import rikka.shizuku.Shizuku
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller as Choice

@RunWith(RobolectricTestRunner::class)
internal class ExtensionInstallServiceTest {

    private val controller = Robolectric.buildService(ExtensionInstallService::class.java)
    private val uri = Uri.parse("content://apk/1")

    @Before
    fun setUp() {
        mockkConstructor(PackageInstallerInstaller::class, ShizukuInstaller::class)
        every { anyConstructed<PackageInstallerInstaller>().addToQueue(any(), any()) } just runs
        every { anyConstructed<PackageInstallerInstaller>().onDestroy() } just runs
        every { anyConstructed<ShizukuInstaller>().addToQueue(any(), any()) } just runs
        mockkStatic(Shizuku::class)
        every { Shizuku.addBinderDeadListener(any()) } just runs
        every { Shizuku.pingBinder() } returns false
    }

    @After
    fun tearDown() = unmockkAll()

    private fun service(): ExtensionInstallService = controller.create().get()

    private fun start(service: Service, intent: Intent?) = service.onStartCommand(intent, 0, 1)

    private fun request(installer: Choice, id: Long = 1L): Intent =
        ExtensionInstallService.getIntent(controller.get(), id, uri, installer)

    @Test
    fun createPostsTheForegroundNotice() {
        shadowOf(service()).lastForegroundNotificationId shouldBe Notifications.ID_EXTENSION_INSTALLER
    }

    @Test
    fun incompleteIntentsStop() {
        val service = service()
        start(service, null) shouldBe Service.START_NOT_STICKY
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun missingDataStops() {
        val service = service()
        start(service, Intent().putExtra(EXTRA_ID, 1L))
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun missingIdStops() {
        val service = service()
        start(service, request(Choice.PACKAGEINSTALLER).apply { removeExtra(EXTRA_ID) })
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun missingInstallerStops() {
        val service = service()
        start(service, request(Choice.PACKAGEINSTALLER).apply { removeExtra("EXTRA_INSTALLER") })
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun unsupportedInstallerStops() {
        val service = service()
        start(service, request(Choice.LEGACY))
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun packageInstallerQueues() {
        val service = service()
        start(service, request(Choice.PACKAGEINSTALLER, id = 1L))
        start(service, request(Choice.SHIZUKU, id = 2L))
        verify { anyConstructed<PackageInstallerInstaller>().addToQueue(1L, uri) }
        verify { anyConstructed<PackageInstallerInstaller>().addToQueue(2L, uri) }
        shadowOf(service).isStoppedBySelf shouldBe false
        controller.destroy()
        verify { anyConstructed<PackageInstallerInstaller>().onDestroy() }
    }

    @Test
    fun shizukuQueues() {
        val service = service()
        start(service, request(Choice.SHIZUKU, id = 3L))
        verify { anyConstructed<ShizukuInstaller>().addToQueue(3L, uri) }
        service.onBind(null) shouldBe null
    }

    @Test
    fun destroyWithoutInstaller() {
        service()
        controller.destroy()
        verify(exactly = 0) { anyConstructed<PackageInstallerInstaller>().onDestroy() }
    }

    private companion object {
        const val EXTRA_ID = ExtensionInstaller.EXTRA_DOWNLOAD_ID
    }
}
