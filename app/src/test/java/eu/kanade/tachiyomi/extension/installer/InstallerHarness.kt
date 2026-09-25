package eu.kanade.tachiyomi.extension.installer

import android.app.Application
import android.app.Service
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** The apk uri of the queue entry for [downloadId]. */
internal fun entryUri(downloadId: Long): Uri = Uri.parse("content://downloads/$downloadId")

/** A queue entry for [downloadId]. */
internal fun queueEntry(downloadId: Long): Installer.Entry =
    Installer.Entry(downloadId = downloadId, uri = entryUri(downloadId))

/**
 * The service, extension manager and Koin graph every [Installer] pulls, with the real application
 * as the broadcast context so [androidx.localbroadcastmanager.content.LocalBroadcastManager] works.
 */
internal class InstallerHarness {
    val application: Application = ApplicationProvider.getApplicationContext()
    val service: Service = mockk(relaxed = true)
    val extensionInstaller: ExtensionInstaller = mockk(relaxed = true)
    val extensionManager: ExtensionManager = mockk(relaxed = true)

    fun install() {
        every { service.applicationContext } returns application
        every { service.packageName } returns application.packageName
        every { extensionManager.installer } returns extensionInstaller
        stopKoin()
        startKoin { modules(module { single { extensionManager } }) }
    }

    fun uninstall() = stopKoin()
}

/** An [Installer] that records what the queue handed it. */
internal class RecordingInstaller(
    service: Service,
    override var ready: Boolean = true,
) : Installer(service) {
    val processed: MutableList<Entry> = mutableListOf()
    var allowCancel: Boolean = true

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        processed += entry
    }

    override fun cancelEntry(entry: Entry): Boolean = allowCancel

    /** [getActiveEntry] is protected; this opens it to the tests. */
    fun activeEntry(): Entry? = getActiveEntry()
}

/** An [Installer] that keeps every default, so the base [Installer.cancelEntry] runs. */
internal class PlainInstaller(service: Service) : Installer(service) {
    override var ready: Boolean = true
}
