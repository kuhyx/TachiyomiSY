package eu.kanade.tachiyomi.util

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class CrashLogUtilTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val extensionManager = mockk<ExtensionManager>()
    private val preferences = BasePreferences(context, InMemoryPreferenceStore())

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun installed(name: String, versionCode: Long, obsolete: Boolean = false): Extension.Installed =
        Extension.Installed(
            name = name,
            pkgName = "pkg.$name",
            versionName = "1.0",
            versionCode = versionCode,
            libVersion = 1.5,
            lang = "en",
            isNsfw = false,
            pkgFactory = null,
            sources = emptyList(),
            icon = null,
            isObsolete = obsolete,
            isShared = false,
        )

    private fun available(name: String, versionCode: Long): Extension.Available = Extension.Available(
        name = name,
        pkgName = "pkg.$name",
        versionName = "2.0",
        versionCode = versionCode,
        libVersion = 1.5,
        lang = "en",
        isNsfw = false,
        sources = emptyList(),
        apkUrl = "https://store/$name.apk",
        iconUrl = "https://store/$name.png",
        store = mockk(),
    )

    @Test
    fun collaboratorsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { extensionManager }
                    single { preferences }
                },
            )
        }
        preferences.installationId.set("from-injekt")
        CrashLogUtil(context).getDebugInfo() shouldContain "Installation ID: from-injekt"
        stopKoin()
    }

    @Test
    fun debugInfoNamesTheBuildAnd() {
        preferences.installationId.set("install-id")
        val util = CrashLogUtil(context, extensionManager, preferences)
        val info = util.getDebugInfo()
        info shouldContain "App version:"
        info shouldContain "Installation ID: install-id"
        info shouldContain "Android version:"
        info shouldContain "WebView:"
    }

    @Test
    fun dumpsLogsOfProblemExtensions() = runTest(dispatcher) {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(
            listOf(installed("old", 1), installed("orphan", 2, obsolete = true), installed("fine", 3)),
        )
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(
            listOf(available("old", 2), available("fine", 3)),
        )
        CrashLogUtil(context, extensionManager, preferences).dumpLogs()
        val logs = File(context.externalCacheDir, "tachiyomi_sy_crash_logs.txt").readText()
        logs shouldContain "Problematic extensions:"
        logs shouldContain "- old"
        logs shouldContain "- orphan"
        logs shouldNotContain "- fine"
    }

    @Test
    fun anExceptionAndNoProblem() = runTest(dispatcher) {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(listOf(installed("fine", 3)))
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(listOf(available("fine", 3)))
        CrashLogUtil(context, extensionManager, preferences).dumpLogs(IllegalStateException("crashed"))
        val logs = File(context.externalCacheDir, "tachiyomi_sy_crash_logs.txt").readText()
        logs shouldContain "crashed"
        logs shouldNotContain "Problematic extensions:"
    }

    @Test
    fun failuresAreToasted() = runTest(dispatcher) {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(emptyList())
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(emptyList())
        val broken = spyk(context)
        every { broken.externalCacheDir } throws IllegalStateException("no cache")
        CrashLogUtil(broken, extensionManager, preferences).dumpLogs()
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to get logs"
    }

    // A logcat stand-in that exists on any host, so the dump reaches the share sheet.
    @Test
    fun sharesTheDump() = runTest(dispatcher) {
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(emptyList())
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(emptyList())
        clearFileProviderCache()
        try {
            CrashLogUtil(context, extensionManager, preferences, logcatCommand = { arrayOf("true") }).dumpLogs()
            val started = shadowOf(context as Application).nextStartedActivity
            started.action shouldBe Intent.ACTION_CHOOSER
        } finally {
            clearFileProviderCache()
        }
    }

    // FileProvider caches each authority's roots statically, and every Robolectric test gets a new cache
    // dir: without this, whichever test shares a file second resolves it against a stale root.
    private fun clearFileProviderCache() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }
}
