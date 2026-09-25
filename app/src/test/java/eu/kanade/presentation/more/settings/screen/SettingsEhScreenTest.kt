package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class SettingsEhScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val eh = EhScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        koin.start(eh.module())
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    private fun subtitle(title: String): String? = harness.item(title).subtitle?.toString()

    @Test
    fun enabledFollowsPreference() {
        SettingsEhScreen.isEnabled() shouldBe true
        koin.exh.isHentaiEnabled.set(false)
        SettingsEhScreen.isEnabled() shouldBe false
    }

    @Test
    fun loginSwitchOffStores() {
        koin.exh.enableExhentai.set(true)
        harness.show(SettingsEhScreen)
        subtitle("Enable ExHentai") shouldBe null
        harness.switch("Enable ExHentai", value = false) shouldBe true
        koin.exh.enableExhentai.get() shouldBe false
        compose.waitForIdle()
        subtitle("Enable ExHentai") shouldBe "Requires login"
    }

    @Test
    fun loginCancelledStaysOff() {
        harness.registry.answer = { ActivityResult(Activity.RESULT_CANCELED, null) }
        harness.show(SettingsEhScreen)
        harness.switch("Enable ExHentai", value = true) shouldBe false
        harness.count("Settings profile note") shouldBe 0
    }

    @Test
    fun loginOkAsksToUpload() {
        harness.registry.answer = { ActivityResult(Activity.RESULT_OK, null) }
        harness.show(SettingsEhScreen)
        harness.switch("Enable ExHentai", value = true) shouldBe false
        compose.waitForIdle()
        harness.count("Settings profile note") shouldBe 1
    }

    @Test
    fun subtitlesFollowValues() {
        harness.show(SettingsEhScreen)
        subtitle("Use original images") shouldBe "Currently using resampled images"
        subtitle("Show Japanese titles in search results").orEmpty() shouldStartWith "Currently showing English"
        koin.exh.exhUseOriginalImages.set(true)
        koin.exh.useJapaneseTitle.set(true)
        compose.waitForIdle()
        subtitle("Use original images") shouldBe "Currently using original images"
        subtitle("Show Japanese titles in search results").orEmpty() shouldStartWith "Currently showing Japanese"
    }

    @Test
    fun watchedTagsOpensWebView() {
        koin.exh.enableExhentai.set(true)
        harness.show(SettingsEhScreen)
        harness.item("Watched Tags").enabled shouldBe true
        harness.click("Watched Tags")
        shadowOf(koin.context).nextStartedActivity.component?.className shouldBe WebViewActivity::class.java.name
    }
}
