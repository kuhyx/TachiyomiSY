package eu.kanade.tachiyomi.extension

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.extension.util.uninstallPrivateExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.installSilentXLog
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore

/** The package name [BlacklistedSources] always hides. */
internal const val BLACKLISTED_PKG = "eu.kanade.tachiyomi.extension.all.ehentai"

private const val TIMEOUT_MS = 5_000L

/** Blocks until [condition] holds, polling; fails the test after five seconds. */
internal fun waitUntil(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + TIMEOUT_MS
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "condition not met in time" }
        Thread.sleep(POLL_MS)
    }
}

private const val POLL_MS = 10L

/** Blocks until the manager's lazily shared installed list satisfies [condition]. */
internal fun ExtensionManager.awaitInstalled(condition: (List<String>) -> Boolean) {
    runBlocking {
        withTimeout(TIMEOUT_MS) { installedExtensionsFlow.first { list -> condition(list.map { it.pkgName }) } }
    }
}

/**
 * An [ExtensionManager] over a mocked [ExtensionLoader] (answering [loaded] at start-up), a mocked
 * store repository and a constructor-mocked [ExtensionInstaller], for one Robolectric test.
 */
internal class ExtensionManagerHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val store = InMemoryPreferenceStore()
    val preferences = SourcePreferences(store)
    val trustExtension = mockk<TrustExtension>(relaxed = true)
    val repository = mockk<ExtensionStoreRepository>()
    var loaded: List<LoadResult> = emptyList()
    var logged = mutableListOf<String>()
    private val managers = mutableListOf<ExtensionManager>()

    fun start() {
        logged = captureLogcat()
        installSilentXLog()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        mockkObject(ExtensionLoader)
        every { ExtensionLoader.loadExtensions(any()) } answers { loaded }
        mockkStatic("eu.kanade.tachiyomi.extension.util.ExtensionLoaderPrivateKt")
        every { ExtensionLoader.uninstallPrivateExtension(any(), any()) } just runs
        mockkConstructor(ExtensionInstaller::class)
        every { anyConstructed<ExtensionInstaller>().downloadAndInstall(any(), any()) } returns
            flowOf(InstallStep.Installed)
        every { anyConstructed<ExtensionInstaller>().cancelInstall(any()) } just runs
        every { anyConstructed<ExtensionInstaller>().updateInstallStep(any(), any()) } just runs
        every { anyConstructed<ExtensionInstaller>().uninstallApk(any()) } just runs
        stopKoin()
        startKoin {
            modules(
                module {
                    single { repository }
                    single { SecurityPreferences(store) }
                    single { BasePreferences(context, store) }
                    single<NetworkHelper> { mockk(relaxed = true) }
                },
            )
        }
    }

    fun stop() {
        managers.forEach { it.scope.cancel() }
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    fun manager(): ExtensionManager = ExtensionManager(context, preferences, trustExtension).also { managers += it }
}
