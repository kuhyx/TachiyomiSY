package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
import exh.uconfig.EHConfigurator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsEhSyncTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val eh = EhScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkConstructor(EHConfigurator::class)
        coEvery { anyConstructed<EHConfigurator>().configureAll() } just runs
        mockkStatic("exh.eh.EHentaiUpdateSchedulingKt")
        every { EHentaiUpdateWorker.scheduleBackground(any(), any(), any()) } just runs
        koin.start(eh.module())
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

    private fun subtitle(title: String): String = harness.item(title).subtitle.toString()

    @Test
    fun resetSucceedsWithToast() {
        coEvery { eh.deleteFavorites.await() } just runs
        harness.click("Force sync state reset")
        tap("OK")
        compose.awaitMain(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        ShadowToast.getTextOfLatestToast().toString() shouldBe "Sync state reset"
    }

    @Test
    fun resetFailureIsLogged() {
        coEvery { eh.deleteFavorites.await() } throws IllegalStateException("db")
        harness.click("Force sync state reset")
        tap("OK")
        coVerify(timeout = 10_000) { eh.deleteFavorites.await() }
        ShadowToast.shownToastCount() shouldBe 0
        harness.click("Force sync state reset")
        tap("Cancel")
        harness.count("Are you sure?") shouldBe 0
    }

    @Test
    fun frequencySubtitleAndSchedule() {
        subtitle("Time between update batches") shouldStartWith "TachiyomiSY checks/updates"
        koin.exh.exhAutoUpdateFrequency.set(0)
        compose.waitForIdle()
        subtitle("Time between update batches") shouldStartWith "TachiyomiSY will currently never"
        harness.list("Time between update batches", 6) shouldBe true
        verify { EHentaiUpdateWorker.scheduleBackground(any(), prefInterval = 6, prefRestrictions = null) }
    }

    @Test
    fun requirementsAndSchedule() {
        koin.exh.exhAutoUpdateRequirements.set(emptySet())
        compose.waitForIdle()
        subtitle("Auto update restrictions") shouldBe "Restrictions: None"
        koin.exh.exhAutoUpdateRequirements.set(setOf("wifi", "ac", "other"))
        compose.waitForIdle()
        subtitle("Auto update restrictions") shouldBe "Restrictions: When charging, other, Only on Wi-Fi"
        harness.multi("Auto update restrictions", setOf("ac")) shouldBe true
        verify { EHentaiUpdateWorker.scheduleBackground(any(), prefInterval = null, prefRestrictions = setOf("ac")) }
    }
}
