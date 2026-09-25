package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import exh.debug.SettingsDebugScreen
import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DeveloperToolsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val advanced = AdvancedScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        stubTextureLimits()
        koin.start(advanced.module())
        harness.show(SettingsAdvancedScreen)
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun hentaiFeaturesToggleSources() {
        harness.switch("Enable integrated hentai features", value = true) shouldBe true
        BlacklistedSources.HIDDEN_SOURCES shouldContain EH_SOURCE_ID
        BlacklistedSources.HIDDEN_SOURCES shouldContain EXH_SOURCE_ID
        harness.switch("Enable integrated hentai features", value = false) shouldBe true
        BlacklistedSources.HIDDEN_SOURCES shouldNotContain EH_SOURCE_ID
        BlacklistedSources.HIDDEN_SOURCES shouldNotContain EXH_SOURCE_ID
    }

    @Test
    fun encryptionAsksFirst() {
        harness.switch("Encrypt database", value = true) shouldBe false
        tap("OK")
        koin.security.encryptDatabase.get() shouldBe true
        harness.switch("Encrypt database", value = true) shouldBe false
        tap("Cancel")
        harness.count("Cancel") shouldBe 0
        harness.switch("Encrypt database", value = false) shouldBe true
    }

    @Test
    fun debugMenuAndLogLevel() {
        harness.click("Open debug menu")
        verify { harness.navigator.push(any<SettingsDebugScreen>()) }
        harness.list("Log level", 1) shouldBe true
    }
}
