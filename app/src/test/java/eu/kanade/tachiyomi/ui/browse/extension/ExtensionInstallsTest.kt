package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.available
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.extension.interactor.untrusted
import eu.kanade.domain.extension.model.Extensions
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionInstallsTest {
    private val koin = BrowseKoin()
    private val stale = installed("Old", hasUpdate = true)
    private val fresh = available("Old", listOf(1L to "en"), pkgName = stale.pkgName)
    private val extensions = MutableStateFlow(Extensions(listOf(stale), listOf(installed("Mine")), emptyList(), emptyList()))
    private val steps = MutableSharedFlow<InstallStep>()
    private val manager = mockk<ExtensionManager>(relaxed = true) {
        every { installer.downloadAndInstall(any(), any()) } returns steps
        every { availableExtensionMapFlow } returns MutableStateFlow(mapOf(stale.pkgName to fresh))
    }

    @Before
    fun setUp() = koin.start(
        module {
            single { manager }
            single<GetExtensionsByType> { mockk { every { subscribe() } returns extensions } }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun model() = ExtensionsScreenModel().also { model -> eventually { model.state.value.items.size == 2 } }

    private fun step(model: ExtensionsScreenModel): InstallStep? =
        model.state.value.items.values.flatten().firstOrNull { it.extension.pkgName == stale.pkgName }?.installStep

    @Test
    fun installProgressIsShown() {
        val model = model()
        model.installExtension(fresh)
        eventually { steps.subscriptionCount.value == 1 }
        runBlocking { steps.emit(InstallStep.Installing) }
        eventually { step(model) == InstallStep.Installing }
        runBlocking { steps.emit(InstallStep.Installed) }
        eventually { step(model) == InstallStep.Idle }
    }

    @Test
    fun updateAllUpdatesStaleOnes() {
        val model = model()
        model.updateAllExtensions()
        eventually { steps.subscriptionCount.value == 1 }
        verify { manager.installer.downloadAndInstall(fresh.apkUrl, fresh) }
        model.updateExtension(installed("Mine"))
        steps.subscriptionCount.value shouldBe 1
    }

    @Test
    fun cancelClearsTheStep() {
        val model = model()
        model.installExtension(fresh)
        eventually { steps.subscriptionCount.value == 1 }
        runBlocking { steps.emit(InstallStep.Pending) }
        eventually { step(model) == InstallStep.Pending }
        model.cancelInstallUpdateExtension(fresh)
        eventually { step(model) == InstallStep.Idle }
        verify { manager.installer.cancelInstall(fresh.pkgName) }
    }

    @Test
    fun uninstallAndTrustReachManager() {
        val model = model()
        model.uninstallExtension(stale)
        verify { manager.installer.uninstallApk(stale.pkgName) }
        val odd = untrusted("Odd")
        every { manager.untrustedExtensionMapFlow } returns MutableStateFlow(emptyMap())
        model.trustExtension(odd)
        coVerify(timeout = 5_000) { manager.untrustedExtensionMapFlow }
    }
}
