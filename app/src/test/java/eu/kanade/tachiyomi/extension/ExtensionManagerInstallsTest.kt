package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionManagerInstallsTest {

    private val harness = ExtensionManagerHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.loaded = listOf(LoadResult.Untrusted(anUntrustedExtension("pkg.two")))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun installUsesTheListingUrl() = runTest {
        val listing = anAvailableExtension()
        harness.manager().installExtension(listing).toList() shouldBe listOf(InstallStep.Installed)
        verify { anyConstructed<ExtensionInstaller>().downloadAndInstall(listing.apkUrl, listing) }
    }

    @Test
    fun updateNeedsAListing() = runTest {
        val manager = harness.manager()
        manager.updateExtension(anInstalledExtension()).toList() shouldBe emptyList()
        manager.availableExtensionMapFlow.value = mapOf("pkg.one" to anAvailableExtension())
        manager.updateExtension(anInstalledExtension()).toList() shouldBe listOf(InstallStep.Installed)
    }

    @Test
    fun installerCallsPassThrough() {
        val manager = harness.manager()
        manager.cancelInstallUpdateExtension(anInstalledExtension())
        manager.setInstalling(3L)
        manager.updateInstallStep(4L, InstallStep.Error)
        manager.uninstallExtension(anInstalledExtension())
        verify { anyConstructed<ExtensionInstaller>().cancelInstall("pkg.one") }
        verify { anyConstructed<ExtensionInstaller>().updateInstallStep(3L, InstallStep.Installing) }
        verify { anyConstructed<ExtensionInstaller>().updateInstallStep(4L, InstallStep.Error) }
        verify { anyConstructed<ExtensionInstaller>().uninstallApk("pkg.one") }
    }

    @Test
    fun trustNeedsAnUntrustedEntry() = runTest {
        harness.manager().trust(anUntrustedExtension("pkg.three"))
        verify(exactly = 0) { harness.trustExtension.trust(any(), any(), any()) }
    }

    @Test
    fun trustedExtensionLoads() = runTest {
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.two") } returns
            LoadResult.Success(anInstalledExtension("pkg.two"))
        val manager = harness.manager()
        manager.trust(anUntrustedExtension("pkg.two"))
        verify { harness.trustExtension.trust("pkg.two", 1L, "hash") }
        manager.untrustedExtensionMapFlow.value shouldBe emptyMap()
        manager.installedExtensionMapFlow.value.keys shouldBe setOf("pkg.two")
    }

    @Test
    fun failedLoadAfterTrust() = runTest {
        coEvery { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.two") } returns LoadResult.Error
        val manager = harness.manager()
        manager.trust(anUntrustedExtension("pkg.two"))
        manager.installedExtensionMapFlow.value shouldBe emptyMap()
        coVerify { ExtensionLoader.loadExtensionFromPkgName(any(), "pkg.two") }
    }
}
