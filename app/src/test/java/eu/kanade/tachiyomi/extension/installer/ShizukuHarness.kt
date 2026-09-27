package eu.kanade.tachiyomi.extension.installer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.util.ExtensionInstallService
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import mihon.app.shizuku.IShellInterface
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import rikka.shizuku.Shizuku

/**
 * A [ShizukuInstaller] over a real (Robolectric) service with every static [Shizuku] call mocked;
 * the listeners and service connection the installer hands Shizuku are captured for the tests.
 */
internal class ShizukuHarness {
    val service: ExtensionInstallService = Robolectric.buildService(ExtensionInstallService::class.java).get()
    val extensionInstaller = mockk<ExtensionInstaller>(relaxed = true)
    val shell = mockk<IShellInterface>(relaxed = true)
    val binder = mockk<IBinder>()
    val connection = slot<ServiceConnection>()
    val deadListener = slot<Shizuku.OnBinderDeadListener>()
    val permissionListener = slot<Shizuku.OnRequestPermissionResultListener>()
    var logged = mutableListOf<String>()
    var alive = true
    var granted = true

    fun start() {
        logged = captureLogcat()
        val manager = mockk<ExtensionManager>(relaxed = true)
        every { manager.installer } returns extensionInstaller
        stopKoin()
        startKoin { modules(module { single { manager } }) }
        every { binder.queryLocalInterface(any()) } returns shell
        mockkStatic(Shizuku::class)
        every { Shizuku.addBinderDeadListener(capture(deadListener)) } just runs
        every { Shizuku.removeBinderDeadListener(any()) } returns true
        every { Shizuku.addRequestPermissionResultListener(capture(permissionListener)) } just runs
        every { Shizuku.removeRequestPermissionResultListener(any()) } returns true
        every { Shizuku.requestPermission(any()) } just runs
        every { Shizuku.bindUserService(any(), capture(connection)) } just runs
        every { Shizuku.unbindUserService(any(), any(), any()) } just runs
        every { Shizuku.pingBinder() } answers { alive }
        every { Shizuku.checkSelfPermission() } answers { if (granted) GRANTED else DENIED }
    }

    fun stop() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    fun installer(): ShizukuInstaller = ShizukuInstaller(service)

    /** Shizuku answers the bind with the shell binder. */
    fun connect() = connection.captured.onServiceConnected(ComponentName("a", "b"), binder)

    /** The system reports the install session's [status]. */
    fun report(status: Int) {
        service.sendBroadcast(
            Intent(ACTION_INSTALL_RESULT)
                .setPackage(service.packageName)
                .putExtra(PackageInstaller.EXTRA_STATUS, status)
                .putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, "pkg"),
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun stopped(): Boolean = shadowOf(service).isStoppedBySelf

    private companion object {
        const val GRANTED = PackageManager.PERMISSION_GRANTED
        const val DENIED = PackageManager.PERMISSION_DENIED
    }
}
