package eu.kanade.presentation.more.settings.screen

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.core.common.util.system.ImageUtil

@RunWith(RobolectricTestRunner::class)
internal class SettingsAdvancedNetworkTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val advanced = AdvancedScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkObject(GLUtil, ImageUtil)
        every { GLUtil.DEVICE_TEXTURE_LIMIT } returns 4096
        every { GLUtil.CUSTOM_TEXTURE_LIMIT_OPTIONS } returns listOf(4096, 3072, 2048)
        every { ImageUtil.HARDWARE_BITMAP_UNSUPPORTED } returns false
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every {
            LibraryUpdateJob.startNow(
                context = any(),
                category = any(),
                target = any(),
                group = any(),
                groupExtra = any(),
            )
        } returns true
        koin.start(advanced.module())
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun toast(): String = ShadowToast.getTextOfLatestToast().toString()

    @Test
    fun networkRows() {
        harness.show(SettingsAdvancedScreen)
        harness.click("Clear cookies")
        verify { advanced.network.cookieJar.removeAll() }
        toast() shouldBe "Cookies cleared"
        harness.click("Clear WebView data")
        toast() shouldBe "WebView data cleared"
        harness.list("DNS over HTTPS (DoH)", 1) shouldBe true
        toast() shouldBe "Requires app restart to take effect"
    }

    @Test
    fun userAgentValidation() {
        harness.show(SettingsAdvancedScreen)
        harness.item("Reset default user agent string").enabled shouldBe false
        harness.edit("Default user agent string", "bad\nagent") shouldBe false
        toast() shouldBe "Invalid user agent string"
        harness.edit("Default user agent string", "Agent/1.0") shouldBe true
        koin.network.defaultUserAgent.set("Agent/1.0")
        compose.waitForIdle()
        harness.item("Reset default user agent string").enabled shouldBe true
        harness.click("Reset default user agent string")
        koin.network.defaultUserAgent.isSet() shouldBe false
    }

    @Test
    fun webViewFailureToasts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mockkStatic("eu.kanade.tachiyomi.util.system.WebViewUtilKt")
        every { any<WebView>().setDefaultSettings() } throws IllegalStateException("no webview")
        context.clearWebViewData()
        toast() shouldBe "Error occurred while clearing"
    }

    @Test
    fun libraryRows() {
        coEvery { advanced.resetViewerFlags.await() } returns true
        harness.show(SettingsAdvancedScreen)
        harness.click("Refresh library covers")
        verify { LibraryUpdateJob.startNow(any(), target = LibraryUpdateJob.Target.COVERS) }
        harness.click("Reset per-series reader settings")
        compose.awaitMain(timeoutMillis = 5_000) { ShadowToast.shownToastCount() == 1 }
        toast() shouldBe "All reader settings reset"
        coEvery { advanced.resetViewerFlags.await() } returns false
        harness.click("Reset per-series reader settings")
        compose.awaitMain(timeoutMillis = 5_000) { toast() == "Couldn't reset reader settings" }
    }

    @Test
    fun readerGroupEntries() {
        harness.show(SettingsAdvancedScreen)
        harness.item("Custom hardware bitmap threshold").enabled shouldBe true
        compose.onNodeWithText("Selected: 2048", substring = true).assertExists()
        koin.base.hardwareBitmapThreshold.set(999)
        compose.waitForIdle()
        compose.onNodeWithText("Selected: 2048", substring = true).assertDoesNotExist()
    }

    @Test
    fun readerThresholdDisabled() {
        every { ImageUtil.HARDWARE_BITMAP_UNSUPPORTED } returns true
        harness.show(SettingsAdvancedScreen)
        harness.item("Custom hardware bitmap threshold").enabled shouldBe false
    }

    @Test
    fun readerThresholdSmallDevice() {
        every { GLUtil.DEVICE_TEXTURE_LIMIT } returns GLUtil.SAFE_TEXTURE_LIMIT
        harness.show(SettingsAdvancedScreen)
        harness.item("Custom hardware bitmap threshold").enabled shouldBe false
    }

    @Test
    fun displayProfilePicked() {
        val profile = Uri.parse("content://profiles/p.icc")
        harness.registry.answer = { profile }
        harness.show(SettingsAdvancedScreen)
        harness.click("Custom display profile")
        koin.base.displayProfile.get() shouldBe profile.toString()
        harness.registry.answer = { null }
        harness.click("Custom display profile")
        koin.base.displayProfile.get() shouldBe profile.toString()
    }
}
