package eu.kanade.tachiyomi.extension.installer

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

@RunWith(RobolectricTestRunner::class)
internal class InstallerTest {
    private val harness = InstallerHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    private fun installer(ready: Boolean = true): RecordingInstaller =
        RecordingInstaller(harness.service, ready)

    @Test
    fun anEntryIsProcessedAtOnce() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = entryUri(1L))
        installer.processed shouldContainExactly listOf(queueEntry(1L))
        installer.activeEntry() shouldBe queueEntry(1L)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installing) }
    }

    @Test
    fun anInstallerThatIsNotReadyWaits() {
        val installer = installer(ready = false)
        installer.addToQueue(downloadId = 1L, uri = entryUri(1L))
        installer.processed.isEmpty() shouldBe true
        installer.ready = true
        installer.checkQueue()
        installer.processed shouldContainExactly listOf(queueEntry(1L))
    }

    @Test
    fun anEmptyQueueStopsTheService() {
        installer().checkQueue()
        verify { harness.service.stopSelf() }
    }

    @Test
    fun theQueueAdvancesOnCompletion() {
        val installer = installer()
        installer.addToQueue(downloadId = 1L, uri = entryUri(1L))
        installer.addToQueue(downloadId = 2L, uri = entryUri(2L))
        installer.processed shouldContainExactly listOf(queueEntry(1L))
        installer.continueQueue(InstallStep.Installed)
        verify { harness.extensionInstaller.updateInstallStep(1L, InstallStep.Installed) }
        installer.processed shouldContainExactly listOf(queueEntry(1L), queueEntry(2L))
    }

    @Test
    fun completingWithoutAnActiveEntry() {
        val installer = installer()
        installer.continueQueue(InstallStep.Installed)
        installer.activeEntry().shouldBeNull()
        verify(exactly = 0) { harness.extensionInstaller.updateInstallStep(any(), InstallStep.Installed) }
    }

    @Test
    fun theEntryDataClass() {
        val entry = queueEntry(3L)
        entry.downloadId shouldBe 3L
        entry.uri shouldBe entryUri(3L)
        entry.copy(downloadId = 4L).downloadId shouldBe 4L
        (entry == queueEntry(3L)) shouldBe true
        entry.hashCode() shouldBe queueEntry(3L).hashCode()
        entry.toString().contains("Entry") shouldBe true
    }
}
