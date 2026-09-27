package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.anUntrustedExtension
import eu.kanade.tachiyomi.extension.model.LoadResult
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ExtensionInstallReceiverTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val listener = mockk<ExtensionInstallReceiver.Listener>(relaxed = true)
    private val receiver = ExtensionInstallReceiver(listener)
    private val installed = anInstalledExtension()
    private val untrusted = anUntrustedExtension()
    private var logged = mutableListOf<String>()

    @Before
    fun setUp() {
        logged = captureLogcat()
        mockkObject(ExtensionLoader)
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.ok") } returns LoadResult.Success(installed)
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.bad") } returns LoadResult.Untrusted(untrusted)
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.err") } returns LoadResult.Error
    }

    @After
    fun tearDown() {
        receiver.scope.cancel()
        unmockkAll()
        releaseLogcat()
    }

    private fun send(action: String?, pkg: String? = null, replacing: Boolean = false) {
        val intent = Intent(action)
        pkg?.let { intent.data = "package:$it".toUri() }
        if (replacing) intent.putExtra(Intent.EXTRA_REPLACING, true)
        receiver.onReceive(context, intent)
    }

    @Test
    fun addedExtensionsAreReported() {
        send(Intent.ACTION_PACKAGE_ADDED, "pkg.ok")
        send(ADDED, "pkg.bad")
        send(Intent.ACTION_PACKAGE_ADDED, "pkg.err")
        verify(timeout = 5_000) { listener.onExtensionInstalled(installed) }
        verify(timeout = 5_000) { listener.onExtensionUntrusted(untrusted) }
        coVerify(timeout = 5_000) { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.err") }
    }

    @Test
    fun replacedExtensionsAreReported() {
        send(Intent.ACTION_PACKAGE_REPLACED, "pkg.ok")
        send("${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_REPLACED", "pkg.bad")
        send(Intent.ACTION_PACKAGE_REPLACED, "pkg.err")
        verify(timeout = 5_000) { listener.onExtensionUpdated(installed) }
        verify(timeout = 5_000) { listener.onExtensionUntrusted(untrusted) }
        coVerify(timeout = 5_000) { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.err") }
    }

    @Test
    fun missingPackageIsAnError() {
        send(Intent.ACTION_PACKAGE_ADDED)
        waitFor { logged.any { it == "Package name not found" } }
        coVerify(exactly = 0) { ExtensionLoader.loadExtensionFromPkgName(any(), any()) }
    }

    @Test
    fun removalsAreReported() {
        send(Intent.ACTION_PACKAGE_REMOVED, "pkg.gone")
        send(REMOVED)
        verify(exactly = 1) { listener.onPackageUninstalled("pkg.gone") }
    }

    @Test
    fun replacingEventsAreIgnored() {
        send(Intent.ACTION_PACKAGE_ADDED, "pkg.ok", replacing = true)
        send(Intent.ACTION_PACKAGE_REMOVED, "pkg.ok", replacing = true)
        send("other", "pkg.ok")
        receiver.onReceive(context, null)
        verify(exactly = 0) { listener.onPackageUninstalled(any()) }
        coVerify(exactly = 0) { ExtensionLoader.loadExtensionFromPkgName(any(), any()) }
    }

    @Test
    fun notificationsAreBroadcast() {
        receiver.register(context)
        ExtensionInstallReceiver.notifyAdded(context, "a")
        ExtensionInstallReceiver.notifyReplaced(context, "b")
        ExtensionInstallReceiver.notifyRemoved(context, "c")
        shadowOf(context).broadcastIntents.map { it.action to it.data.toString() } shouldBe listOf(
            ADDED to "package:a",
            "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_REPLACED" to "package:b",
            REMOVED to "package:c",
        )
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        condition() shouldBe true
    }

    private companion object {
        const val ADDED = "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_ADDED"
        const val REMOVED = "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_REMOVED"
    }
}
