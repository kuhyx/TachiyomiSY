package eu.kanade.tachiyomi.extension.installer

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import eu.kanade.tachiyomi.extension.model.InstallStep
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import mihon.test.CapturingLogcat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class PackageInstallerInstallerTest {
    private val harness = InstallerHarness()
    private val logcat = CapturingLogcat()

    @Before
    fun setUp() {
        logcat.install()
        harness.install()
        every { harness.service.packageManager } returns harness.application.packageManager
        every { harness.service.contentResolver } returns harness.application.contentResolver
        every { harness.service.cacheDir } returns harness.application.cacheDir
    }

    @After
    fun tearDown() {
        harness.uninstall()
        logcat.uninstall()
    }

    /** A real apk file the installer can read, as a uri with a readable stream. */
    private fun apkUri(name: String, exists: Boolean = true): Uri {
        val file = File(harness.application.cacheDir, name)
        if (exists) {
            file.writeText("payload")
        } else {
            file.delete()
        }
        val uri = Uri.fromFile(file)
        shadowOf(harness.application.contentResolver).registerInputStream(uri, "payload".byteInputStream())
        return uri
    }

    private fun installer(): PackageInstallerInstaller = PackageInstallerInstaller(harness.service)

    /** The receiver the installer registered for the package installer's status broadcasts. */
    private fun receiver(): BroadcastReceiver {
        val captured = slot<BroadcastReceiver>()
        verify { harness.service.registerReceiver(capture(captured), any<IntentFilter>(), any<Int>()) }
        return captured.captured
    }

    private fun deliver(status: Int, extra: Intent? = null) {
        val intent = Intent("PackageInstallerInstaller.INSTALL_ACTION")
            .putExtra(PackageInstaller.EXTRA_STATUS, status)
        extra?.let { intent.putExtra(Intent.EXTRA_INTENT, it) }
        receiver().onReceive(harness.application, intent)
    }

    @Test
    fun anApkIsWrittenToASession() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        installer.ready shouldBe true
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
    }

    @Test
    @Config(sdk = [30])
    fun anApkOnAnOlderAndroid() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
    }

    @Test
    fun aMissingApkIsAnError() {
        val installer = installer()
        installer.addToQueue(downloadId = 2L, uri = apkUri("gone.apk", exists = false))
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Error) }
        logcat.messages.single().startsWith("Failed to install extension 2") shouldBe true
    }

    @Test
    fun aSuccessfulInstall() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        deliver(PackageInstaller.STATUS_SUCCESS)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installed) }
    }

    @Test
    fun anAbortedInstall() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        deliver(PackageInstaller.STATUS_FAILURE_ABORTED)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Idle) }
    }

    @Test
    fun aFailedInstall() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        deliver(PackageInstaller.STATUS_FAILURE)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
    }

    @Test
    fun aUserActionIsStarted() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        val action = Intent(Intent.ACTION_VIEW).setClassName("com.android.packageinstaller", "Confirm")
        deliver(PackageInstaller.STATUS_PENDING_USER_ACTION, extra = action)
        verify { harness.service.startActivity(any()) }
    }

    @Test
    fun aUserActionWithoutAnIntent() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        deliver(PackageInstaller.STATUS_PENDING_USER_ACTION)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
        logcat.messages.any { it.startsWith("Fatal error for") } shouldBe true
    }

    @Test
    fun cancellingTheActiveEntry() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = apkUri("one.apk"))
        installer.cancelEntry(queueEntry(1L)) shouldBe false
        installer.cancelEntry(queueEntry(2L)) shouldBe true
    }

    @Test
    fun destroyingUnregistersTheReceiver() {
        val installer = installer()
        installer.onDestroy()
        verify { harness.service.unregisterReceiver(any()) }
    }
}
