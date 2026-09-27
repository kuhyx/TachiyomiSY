package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller.LEGACY
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller.PACKAGEINSTALLER
import eu.kanade.domain.base.BasePreferences.ExtensionInstaller.PRIVATE
import eu.kanade.domain.base.ExtensionInstallerPreference
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.extension.anAvailableExtension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.storage.getUriCompat
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class ExtensionInstallerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer()
    private var choice = PACKAGEINSTALLER
    private var privateInstall: () -> Boolean = { true }
    private val extension = anAvailableExtension()
    private val installers = mutableListOf<ExtensionInstaller>()

    @Before
    fun setUp() {
        captureLogcat()
        server.start()
        val preference = mockk<ExtensionInstallerPreference> { every { get() } answers { choice } }
        val base = mockk<BasePreferences> { every { extensionInstaller } returns preference }
        val network = mockk<NetworkHelper> { every { client } returns OkHttpClient() }
        startKoin {
            modules(
                module {
                    single { base }
                    single { network }
                },
            )
        }
        mockkStatic("eu.kanade.tachiyomi.util.storage.FileExtensionsKt")
        every { any<File>().getUriCompat(any()) } returns Uri.parse("content://apk")
        mockkObject(ExtensionLoader)
        mockkStatic("eu.kanade.tachiyomi.extension.util.ExtensionLoaderPrivateKt")
        every { ExtensionLoader.installPrivateExtensionFile(any(), any()) } answers { privateInstall() }
        every { ExtensionLoader.uninstallPrivateExtension(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        // The installer's IO scope is private; its download jobs must end before the next class runs.
        runBlocking {
            withTimeout(TIMEOUT) {
                installers.forEach { installer ->
                    val field = ExtensionInstaller::class.java.getDeclaredField("scope")
                    field.isAccessible = true
                    (field.get(installer) as CoroutineScope).coroutineContext.job.children.forEach { it.join() }
                }
            }
        }
        server.close()
        stopKoin()
        unmockkAll()
        releaseLogcat()
    }

    private fun installer() = ExtensionInstaller(context).also { installers += it }

    private fun install(code: Int = 200, until: (InstallStep) -> Boolean): InstallStep {
        server.enqueue(MockResponse.Builder().code(code).body("apk").build())
        val steps = installer().downloadAndInstall(server.url("/a.apk").toString(), extension)
        return runBlocking { withTimeout(TIMEOUT) { steps.first(until) } }
    }

    @Test
    fun failedDownloadIsAnError() {
        install(code = 404) { it == InstallStep.Error } shouldBe InstallStep.Error
    }

    @Test
    fun serviceInstallsByDefault() {
        install { it == InstallStep.Installing } shouldBe InstallStep.Installing
        waitFor { shadowOf(context).peekNextStartedService() != null }
        shadowOf(context).nextStartedService.component?.className shouldBe ExtensionInstallService::class.java.name
    }

    @Test
    fun legacyOpensTheInstaller() {
        choice = LEGACY
        install { it == InstallStep.Installing }
        waitFor { shadowOf(context).peekNextStartedActivity() != null }
        val intent = shadowOf(context).nextStartedActivity
        intent.component?.className shouldBe ExtensionInstallActivity::class.java.name
        intent.getLongExtra(ExtensionInstaller.EXTRA_DOWNLOAD_ID, 0) shouldBe "pkg.one".hashCode().toLong()
    }

    @Test
    fun privateInstallReportsResult() {
        choice = PRIVATE
        install { it == InstallStep.Installed } shouldBe InstallStep.Installed
        privateInstall = { false }
        install { it == InstallStep.Error } shouldBe InstallStep.Error
        privateInstall = { error("corrupt") }
        install { it == InstallStep.Error } shouldBe InstallStep.Error
    }

    @Test
    fun interruptedDownloadStops() {
        val call = mockk<Call> { every { execute() } throws InterruptedException() }
        val client = mockk<OkHttpClient> { every { newCall(any()) } returns call }
        val network = mockk<NetworkHelper>()
        every { network.client } returns client
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<BasePreferences>(relaxed = true) }
                    single { network }
                },
            )
        }
        val steps = installer().downloadAndInstall("https://x/a.apk", extension)
        runBlocking { withTimeout(TIMEOUT) { steps.first { it == InstallStep.Downloading } } }
    }

    @Test
    fun stepsFollowUpdates() {
        val installer = installer()
        server.enqueue(MockResponse.Builder().code(404).build())
        server.enqueue(MockResponse.Builder().code(404).build())
        installer.downloadAndInstall(server.url("/a.apk").toString(), extension)
        val steps = installer.downloadAndInstall(server.url("/a.apk").toString(), extension)
        installer.updateInstallStep("pkg.one".hashCode().toLong(), InstallStep.Idle)
        installer.updateInstallStep(1L, InstallStep.Installed)
        runBlocking { withTimeout(TIMEOUT) { steps.first { it != InstallStep.Pending } } }
        installer.cancelInstall("pkg.one")
        installer.cancelInstall("pkg.none")
    }

    @Test
    fun uninstallPicksTheRoute() {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "pkg.system" })
        val installer = installer()
        installer.uninstallApk("pkg.system")
        shadowOf(context).nextStartedActivity.action shouldBe "android.intent.action.UNINSTALL_PACKAGE"
        installer.uninstallApk("pkg.private")
        shadowOf(context).broadcastIntents.last().data.toString() shouldBe "package:pkg.private"
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(POLL)
        condition() shouldBe true
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val POLL = 10L
    }
}
