package eu.kanade.tachiyomi.extension

import android.os.Looper
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.extension.util.uninstallPrivateExtension
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ExtensionManagerTest {

    private val harness = ExtensionManagerHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.loaded = listOf(
            LoadResult.Success(anInstalledExtension("pkg.one")),
            LoadResult.Untrusted(anUntrustedExtension("pkg.two")),
            LoadResult.Error,
            LoadResult.Untrusted(anUntrustedExtension(BLACKLISTED_PKG)),
        )
    }

    @After
    fun tearDown() = harness.stop()

    private fun broadcast(send: () -> Unit) {
        send()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun startupSortsLoadResults() {
        val manager = harness.manager()
        manager.isInitialized.value shouldBe true
        manager.installedExtensionMapFlow.value.keys shouldBe setOf("pkg.one")
        manager.untrustedExtensionMapFlow.value.keys shouldBe setOf("pkg.two")
        manager.awaitInstalled { it == listOf("pkg.one") }
        manager.untrustedExtensionsFlow.value shouldBe emptyList()
        manager.availableExtensionsFlow.value shouldBe emptyList()
    }

    @Test
    fun collaboratorsComeFromInjekt() {
        loadKoinModules(
            module {
                single { harness.preferences }
                single { harness.trustExtension }
            },
        )
        val manager = ExtensionManager(harness.context)
        try {
            manager.isInitialized.value shouldBe true
        } finally {
            manager.scope.cancel()
        }
    }

    @Test
    fun blacklistCanBeDisabled() {
        harness.preferences.enableSourceBlacklist.set(false)
        harness.manager().untrustedExtensionMapFlow.value.keys shouldBe setOf("pkg.two", BLACKLISTED_PKG)
    }

    @Test
    fun updateNeedsANewerListing() {
        val manager = harness.manager()
        with(manager) {
            val installed = anInstalledExtension(versionCode = 2, libVersion = 1.5)
            installed.updateExists() shouldBe false
            installed.updateExists(anAvailableExtension(versionCode = 3, libVersion = 1.5)) shouldBe true
            installed.updateExists(anAvailableExtension(versionCode = 2, libVersion = 1.6)) shouldBe true
            installed.updateExists(anAvailableExtension(versionCode = 2, libVersion = 1.5)) shouldBe false
            availableExtensionMapFlow.value = mapOf("pkg.one" to anAvailableExtension(versionCode = 3))
            installed.withUpdateCheck().hasUpdate shouldBe true
            anInstalledExtension(versionCode = 9).withUpdateCheck().hasUpdate shouldBe false
        }
    }

    @Test
    fun sourcesMapNeedsListings() {
        val manager = harness.manager()
        manager.setupAvailableSourcesMap(emptyList())
        manager.availableExtensionsSourcesData shouldBe emptyMap()
        manager.setupAvailableSourcesMap(listOf(anAvailableExtension(sources = listOf(anAvailableSource(7)))))
        manager.getSourceData(7)?.name shouldBe "Source 7"
    }

    @Test
    fun availableHidesBlacklisted() {
        val manager = harness.manager()
        manager.availableExtensionMapFlow.value = listOf("pkg.x", BLACKLISTED_PKG)
            .associateWith { anAvailableExtension(it) }
        val shown = runBlocking { withTimeout(TIMEOUT) { manager.availableExtensionsFlow.first { it.isNotEmpty() } } }
        shown.map { it.pkgName } shouldBe listOf("pkg.x")
    }

    @Test
    fun installedBroadcastRegisters() {
        val manager = harness.manager()
        manager.availableExtensionMapFlow.value = mapOf("pkg.new" to anAvailableExtension("pkg.new", versionCode = 5))
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.new") } returns
            LoadResult.Success(anInstalledExtension("pkg.new"))
        broadcast { ExtensionInstallReceiver.notifyAdded(harness.context, "pkg.new") }
        waitUntil { manager.installedExtensionMapFlow.value["pkg.new"]?.hasUpdate == true }
        waitUntil { harness.preferences.extensionUpdatesCount.get() == 1 }
    }

    @Test
    fun updatedBroadcastReplaces() {
        val manager = harness.manager()
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.one") } returns
            LoadResult.Success(anInstalledExtension("pkg.one", versionCode = 4))
        broadcast { ExtensionInstallReceiver.notifyReplaced(harness.context, "pkg.one") }
        waitUntil { manager.installedExtensionMapFlow.value["pkg.one"]?.versionCode == 4L }
    }

    @Test
    fun untrustedBroadcastMovesIt() {
        val manager = harness.manager()
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.one") } returns
            LoadResult.Untrusted(anUntrustedExtension("pkg.one"))
        broadcast { ExtensionInstallReceiver.notifyAdded(harness.context, "pkg.one") }
        waitUntil { "pkg.one" in manager.untrustedExtensionMapFlow.value }
        manager.installedExtensionMapFlow.value shouldBe emptyMap()
    }

    @Test
    fun removedBroadcastForgetsIt() {
        val manager = harness.manager()
        broadcast { ExtensionInstallReceiver.notifyRemoved(harness.context, "pkg.one") }
        broadcast { ExtensionInstallReceiver.notifyRemoved(harness.context, "pkg.two") }
        manager.installedExtensionMapFlow.value shouldBe emptyMap()
        manager.untrustedExtensionMapFlow.value shouldBe emptyMap()
        verify { ExtensionLoader.uninstallPrivateExtension(any(), "pkg.one") }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
