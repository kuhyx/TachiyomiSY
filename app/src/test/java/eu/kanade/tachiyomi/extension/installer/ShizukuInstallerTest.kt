package eu.kanade.tachiyomi.extension.installer

import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import eu.kanade.tachiyomi.extension.model.InstallStep
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import rikka.shizuku.Shizuku

@RunWith(RobolectricTestRunner::class)
internal class ShizukuInstallerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val harness = ShizukuHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun apk(): Uri = Uri.fromFile(tmp.newFile("a.apk").apply { writeText("apk") })

    @Test
    fun deadShizukuStopsTheService() {
        harness.alive = false
        harness.installer().ready shouldBe false
        harness.stopped() shouldBe true
        ShadowToast.shownToastCount() shouldBe 1
    }

    @Test
    fun grantedBindsAndInstalls() {
        val installer = harness.installer()
        harness.connect()
        installer.ready shouldBe true
        harness.stopped() shouldBe true
        installer.addToQueue(1L, apk())
        verify { harness.shell.install(any()) }
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installing) }
        harness.report(PackageInstaller.STATUS_SUCCESS)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installed) }
    }

    @Test
    fun failedSessionIsAnError() {
        val installer = harness.installer()
        harness.connect()
        installer.addToQueue(2L, apk())
        harness.report(PackageInstaller.STATUS_FAILURE)
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Error) }
        harness.logged.any { it == "Failed to install extension pkg: null" } shouldBe true
    }

    @Test
    fun unreadableApkIsAnError() {
        val installer = harness.installer()
        harness.connect()
        installer.addToQueue(3L, Uri.parse("content://missing/3"))
        verify { harness.extensionInstaller.updateInstallStep(3L, InstallStep.Error) }
    }

    @Test
    fun disconnectedShellSkipsInstall() {
        val installer = harness.installer()
        harness.connect()
        harness.connection.captured.onServiceDisconnected(null)
        installer.initShizuku()
        installer.addToQueue(4L, apk())
        verify(exactly = 0) { harness.shell.install(any()) }
    }

    @Test
    fun permissionIsRequested() {
        harness.granted = false
        harness.installer()
        verify { Shizuku.requestPermission(any()) }
        harness.permissionListener.captured.onRequestPermissionResult(1, GRANTED)
        verify(exactly = 0) { Shizuku.bindUserService(any(), any()) }
        harness.permissionListener.captured.onRequestPermissionResult(PERMISSION_CODE, GRANTED)
        verify { Shizuku.bindUserService(any(), any()) }
        verify { Shizuku.removeRequestPermissionResultListener(any()) }
    }

    @Test
    fun deniedPermissionStops() {
        harness.granted = false
        harness.installer()
        harness.permissionListener.captured.onRequestPermissionResult(PERMISSION_CODE, DENIED)
        harness.stopped() shouldBe true
    }

    @Test
    fun deadBinderStops() {
        harness.installer()
        harness.deadListener.captured.onBinderDead()
        harness.stopped() shouldBe true
    }

    @Test
    fun onlyTheActiveEntryStays() {
        val installer = harness.installer()
        harness.connect()
        val uri = apk()
        installer.addToQueue(5L, uri)
        installer.cancelEntry(Installer.Entry(5L, uri)) shouldBe false
        installer.cancelEntry(Installer.Entry(6L, uri)) shouldBe true
    }

    @Test
    fun destroyUnbindsWhenAlive() {
        harness.installer().onDestroy()
        verify { Shizuku.unbindUserService(any(), any(), true) }
        every { Shizuku.unbindUserService(any(), any(), any()) } throws IllegalStateException("gone")
        harness.installer().onDestroy()
        harness.logged.any { it.startsWith("Failed to unbind shizuku service") } shouldBe true
        harness.alive = false
        harness.installer().onDestroy()
        verify(exactly = 2) { Shizuku.unbindUserService(any(), any(), any()) }
    }

    private companion object {
        const val PERMISSION_CODE = 14_045
        const val GRANTED = PackageManager.PERMISSION_GRANTED
        const val DENIED = PackageManager.PERMISSION_DENIED
    }
}
