package eu.kanade.tachiyomi.extension.installer

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.ByteArrayOutputStream
import java.io.File

/** A [PackageInstallerInstaller] over a mocked platform installer whose sessions always open. */
internal class PackageInstallerHarness(private val harness: InstallerHarness) {
    val packageInstaller: PackageInstaller = mockk(relaxed = true)
    val session: PackageInstaller.Session = mockk(relaxed = true)
    val resolver: ContentResolver = mockk(relaxed = true)
    val written = ByteArrayOutputStream()

    fun install() {
        val packageManager = mockk<PackageManager>()
        every { packageManager.packageInstaller } returns packageInstaller
        every { harness.service.packageManager } returns packageManager
        every { harness.service.contentResolver } returns resolver
        every { resolver.openInputStream(any()) } answers { "payload".byteInputStream() }
        every { packageInstaller.createSession(any()) } returns SESSION_ID
        every { packageInstaller.openSession(SESSION_ID) } returns session
        every { session.openWrite(any(), any(), any()) } returns written
    }

    /** A readable apk file, so its size is known. */
    fun apk(dir: File, name: String): Uri = Uri.fromFile(File(dir, name).apply { writeText("payload") })

    fun installer(): PackageInstallerInstaller = PackageInstallerInstaller(harness.service)

    private var registered: BroadcastReceiver? = null

    /** The receiver the installer registered for the platform installer's status broadcasts. */
    fun receiver(): BroadcastReceiver {
        registered?.let { return it }
        val captured = slot<BroadcastReceiver>()
        verify {
            harness.service.registerReceiver(
                capture(captured),
                any<IntentFilter>(),
                any<String>(),
                any<Handler>(),
                any<Int>(),
            )
        }
        return captured.captured.also { registered = it }
    }

    fun deliver(status: Int?, extra: Intent? = null) {
        val intent = Intent("PackageInstallerInstaller.INSTALL_ACTION")
        status?.let { intent.putExtra(PackageInstaller.EXTRA_STATUS, it) }
        extra?.let { intent.putExtra(Intent.EXTRA_INTENT, it) }
        receiver().onReceive(harness.application, intent)
    }

    companion object {
        const val SESSION_ID = 7
    }
}
