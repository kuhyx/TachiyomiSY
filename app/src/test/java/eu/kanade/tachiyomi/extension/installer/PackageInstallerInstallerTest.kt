package eu.kanade.tachiyomi.extension.installer

import android.content.Intent
import android.content.pm.PackageInstaller
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

@RunWith(RobolectricTestRunner::class)
internal class PackageInstallerInstallerTest {
    private val harness = InstallerHarness()
    private val platform = PackageInstallerHarness(harness)
    private val logcat = CapturingLogcat()

    @Before
    fun setUp() {
        logcat.install()
        harness.install()
        platform.install()
    }

    @After
    fun tearDown() {
        harness.uninstall()
        logcat.uninstall()
    }

    private fun queueOne(): PackageInstallerInstaller {
        val installer = platform.installer()
        installer.addToQueue(downloadId = 1L, uri = platform.apk(harness.application.cacheDir, "one.apk"))
        return installer
    }

    @Test
    fun anApkIsWrittenAndCommitted() {
        val installer = queueOne()
        installer.ready shouldBe true
        platform.written.toString() shouldBe "payload"
        verify { platform.session.commit(any()) }
        verify { platform.resolver.delete(any(), null, null) }
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
    }

    @Test
    fun anUnknownUriHasNoSize() {
        val installer = platform.installer()
        installer.addToQueue(downloadId = 2L, uri = android.net.Uri.parse("nowhere://apk"))
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Error) }
        verify { platform.packageInstaller.abandonSession(PackageInstallerHarness.SESSION_ID) }
        logcat.messages.single().startsWith("Failed to install extension 2") shouldBe true
    }

    @Test
    fun anUnreadableApk() {
        every { platform.resolver.openInputStream(any()) } returns null
        queueOne()
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
    }

    @Test
    fun noSessionNothingToAbandon() {
        every { platform.packageInstaller.createSession(any()) } throws java.io.IOException("full")
        queueOne()
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
        verify(exactly = 0) { platform.packageInstaller.abandonSession(any()) }
    }

    @Test
    fun statusBroadcasts() {
        queueOne()
        platform.deliver(PackageInstaller.STATUS_SUCCESS)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installed) }
    }

    @Test
    fun abortedAndFailedBroadcasts() {
        val installer = queueOne()
        platform.deliver(PackageInstaller.STATUS_FAILURE_ABORTED)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Idle) }
        installer.addToQueue(downloadId = 3L, uri = platform.apk(harness.application.cacheDir, "3.apk"))
        platform.deliver(null)
        verify { harness.extensionInstaller.updateInstallStep(3L, InstallStep.Error) }
    }

    @Test
    fun aUserActionIsStarted() {
        queueOne()
        val action = Intent(Intent.ACTION_VIEW)
            .setClassName("com.android.packageinstaller", "Confirm")
            .setPackage("com.android.packageinstaller")
            .putExtra(PackageInstaller.EXTRA_SESSION_ID, PackageInstallerHarness.SESSION_ID)
        platform.deliver(PackageInstaller.STATUS_PENDING_USER_ACTION, extra = action)
        val started = slot<Intent>()
        verify { harness.service.startActivity(capture(started)) }
        (started.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
    }

    @Test
    fun aUserActionWithoutAnIntent() {
        queueOne()
        platform.deliver(PackageInstaller.STATUS_PENDING_USER_ACTION)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Error) }
        logcat.messages.any { it.startsWith("Fatal error for") } shouldBe true
    }

    @Test
    fun destroyingUnregistersTheReceiver() {
        platform.installer().onDestroy()
        verify { harness.service.unregisterReceiver(any()) }
    }
}
