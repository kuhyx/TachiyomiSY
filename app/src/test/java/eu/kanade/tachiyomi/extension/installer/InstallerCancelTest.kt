package eu.kanade.tachiyomi.extension.installer

import android.os.Looper
import eu.kanade.tachiyomi.extension.model.InstallStep
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class InstallerCancelTest {
    private val harness = InstallerHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    private fun installer(): RecordingInstaller = RecordingInstaller(harness.service)

    private fun queued(installer: RecordingInstaller, vararg ids: Long) {
        ids.forEach { installer.addToQueue(downloadId = it, uri = entryUri(it)) }
    }

    @Test
    fun aQueuedEntryIsCancelled() {
        val installer = installer()
        queued(installer, 1L, 2L)
        installer.cancelQueue(2L)
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Idle) }
        installer.processed shouldContainExactly listOf(queueEntry(1L))
        installer.activeEntry() shouldBe queueEntry(1L)
    }

    @Test
    fun theActiveEntryIsCancelled() {
        val installer = installer()
        queued(installer, 1L, 2L)
        installer.cancelQueue(1L)
        installer.processed shouldContainExactly listOf(queueEntry(1L), queueEntry(2L))
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Idle) }
    }

    @Test
    fun refusedCancellationKeepsEntry() {
        val installer = installer()
        installer.allowCancel = false
        queued(installer, 1L)
        installer.cancelQueue(1L)
        installer.activeEntry() shouldBe queueEntry(1L)
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Idle) }
    }

    @Test
    fun anUnknownDownloadIsIgnored() {
        val installer = installer()
        installer.cancelQueue(9L)
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(any(), InstallStep.Idle) }
    }

    @Test
    fun cancelBroadcastReachesQueue() {
        val installer = installer()
        queued(installer, 1L, 2L)
        Installer.cancelInstallQueue(harness.application, 2L)
        shadowOf(Looper.getMainLooper()).idle()
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Idle) }
    }

    @Test
    fun aBroadcastWithoutADownloadId() {
        installer()
        Installer.cancelInstallQueue(harness.application, -1L)
        shadowOf(Looper.getMainLooper()).idle()
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(any(), InstallStep.Idle) }
    }

    @Test
    fun theDefaultCancelAlwaysAgrees() {
        val installer = PlainInstaller(harness.service)
        installer.addToQueue(downloadId = 4L, uri = entryUri(4L))
        installer.cancelQueue(4L)
        verify { harness.extensionInstaller.updateInstallStep(4L, InstallStep.Idle) }
    }

    @Test
    fun destroyingErrorsTheWholeQueue() {
        val installer = installer()
        queued(installer, 1L, 2L, 3L)
        installer.onDestroy()
        verify { harness.extensionInstaller.updateInstallStep(2L, InstallStep.Error) }
        verify { harness.extensionInstaller.updateInstallStep(3L, InstallStep.Error) }
        installer.activeEntry().shouldBeNull()
    }
}
