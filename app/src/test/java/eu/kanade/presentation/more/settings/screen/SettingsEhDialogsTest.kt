package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.presentation.more.settings.widget.pressDialogBack
import exh.uconfig.EHConfigurator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsEhDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val eh = EhScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkConstructor(EHConfigurator::class)
        coEvery { anyConstructed<EHConfigurator>().configureAll() } just runs
        koin.exh.exhShowSettingsUploadWarning.set(false)
        koin.start(eh.module())
        koin.exh.enableExhentai.set(true)
        harness.show(SettingsEhScreen)
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

    private fun type(text: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(text)
        compose.waitForIdle()
    }

    @Test
    fun filterThresholdValidates() {
        harness.click("Tag Filtering Threshold")
        type("abc")
        harness.count("Must be between -9999 and 0!") shouldBe 1
        type("5")
        harness.count("Must be between -9999 and 0!") shouldBe 1
        type("-10")
        harness.count("Must be between -9999 and 0!") shouldBe 0
        tap("OK")
        koin.exh.ehTagFilterValue.get() shouldBe -10
    }

    @Test
    fun watchingThresholdStores() {
        harness.click("Tag Watching Threshold")
        type("25")
        tap("OK")
        koin.exh.ehTagWatchingValue.get() shouldBe 25
        harness.click("Tag Watching Threshold")
        tap("Cancel")
        harness.click("Tag Filtering Threshold")
        tap("Cancel")
        harness.count("Cancel") shouldBe 0
    }

    @Test
    fun languagesToggleAndStore() {
        val before = koin.exh.exhSettingsLanguages.get()
        harness.click("Language Filtering")
        val boxes = compose.onAllNodes(isToggleable())
        repeat(2) {
            (0..4).forEach { boxes[it].performClick() }
        }
        boxes[2].performClick()
        tap("OK")
        koin.exh.exhSettingsLanguages.get() shouldNotBe before
        harness.click("Language Filtering")
        tap("Cancel")
        harness.count("Japanese") shouldBe 0
    }

    @Test
    fun frontPageToggleAndStore() {
        val before = koin.exh.exhEnabledCategories.get()
        harness.click("Front Page Categories")
        tap("Manga")
        tap("OK")
        koin.exh.exhEnabledCategories.get() shouldNotBe before
        harness.click("Front Page Categories")
        tap("Cancel")
        harness.count("Artist CG") shouldBe 0
    }

    @Test
    fun syncNotesDialog() {
        harness.click("Show favorites sync notes")
        tap("OK")
        harness.count("OK") shouldBe 0
        harness.click("Show favorites sync notes")
        pressDialogBack()
        compose.waitForIdle()
        harness.count("OK") shouldBe 0
    }
}
