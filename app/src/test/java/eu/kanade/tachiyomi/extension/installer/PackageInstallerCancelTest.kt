package eu.kanade.tachiyomi.extension.installer

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class PackageInstallerCancelTest {
    private val harness = InstallerHarness()
    private val platform = PackageInstallerHarness(harness)
    private val sdk = Build.VERSION.SDK_INT
    private val apk by lazy { platform.apk(harness.application.cacheDir, "one.apk") }

    @Before
    fun setUp() {
        harness.install()
        platform.install()
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        harness.uninstall()
    }

    private fun queueOne(): PackageInstallerInstaller {
        val installer = platform.installer()
        installer.addToQueue(downloadId = 1L, uri = apk)
        return installer
    }

    @Test
    fun nothingActiveCanBeCancelled() {
        platform.installer().cancelEntry(queueEntry(1L)) shouldBe true
    }

    @Test
    fun anotherEntryCanBeCancelled() {
        queueOne().cancelEntry(queueEntry(2L)) shouldBe true
    }

    @Test
    fun theActiveSessionIsAbandoned() {
        queueOne().cancelEntry(Installer.Entry(downloadId = 1L, uri = apk)) shouldBe false
        verify { platform.packageInstaller.abandonSession(PackageInstallerHarness.SESSION_ID) }
    }

    @Test
    fun finishedSessionIsNotAbandoned() {
        val installer = queueOne()
        every { platform.packageInstaller.abandonSession(any()) } throws SecurityException("done")
        installer.cancelEntry(Installer.Entry(downloadId = 1L, uri = apk)) shouldBe true
    }

    @Test
    fun olderSdkNeedsNoUserActionFlag() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.R)
        queueOne()
        verify { platform.session.commit(any()) }
    }

    @Test
    fun aUserActionBeforeAnySession() {
        every { platform.packageInstaller.createSession(any()) } throws java.io.IOException("full")
        queueOne()
        val action = Intent(Intent.ACTION_VIEW)
            .setClassName("com.android.packageinstaller", "Confirm")
            .putExtra(PackageInstaller.EXTRA_SESSION_ID, PackageInstallerHarness.SESSION_ID)
        platform.deliver(PackageInstaller.STATUS_PENDING_USER_ACTION, extra = action)
        val started = slot<Intent>()
        verify { harness.service.startActivity(capture(started)) }
        started.captured.hasExtra(PackageInstaller.EXTRA_SESSION_ID) shouldBe false
    }
}
