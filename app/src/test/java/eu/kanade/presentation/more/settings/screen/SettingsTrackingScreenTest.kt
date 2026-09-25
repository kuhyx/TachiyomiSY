package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class SettingsTrackingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val installed = enhancedTracker("Komga", listOf(InstalledSource::class.qualifiedName!!))
    private val missing = enhancedTracker("Kavita", listOf("not.installed.Source"))

    @After
    fun tearDown() {
        koin.stop()
    }

    private fun show(enhanced: List<Tracker>) {
        val sourceManager = mockk<SourceManager> { every { getAll() } returns listOf(InstalledSource()) }
        koin.start(
            module {
                single { stubTrackerManager(enhanced) }
                single { sourceManager }
            },
        )
        harness.show(SettingsTrackingScreen)
    }

    private fun tracker(name: String): Preference.PreferenceItem.TrackerPreference = harness.items()
        .filterIsInstance<Preference.PreferenceItem.TrackerPreference>()
        .first { it.tracker.name == name }

    private fun info(): String = harness.items()
        .filterIsInstance<Preference.PreferenceItem.InfoPreference>()
        .last()
        .title

    @Test
    fun enhancedTrackersSplitByInstall() {
        show(listOf(installed, missing))
        tracker("Komga").login()
        verify { (installed as EnhancedTracker).loginNoop() }
        tracker("Komga").logout()
        verify { installed.logout() }
        info() shouldContain "Available but source not installed: Kavita"
    }

    @Test
    fun allEnhancedInstalled() {
        show(listOf(installed))
        info() shouldNotContain "Available but source not installed"
    }

    @Test
    fun browserLoginOpensBrowser() {
        show(emptyList())
        compose.runOnIdle { tracker("MyAnimeList").login() }
        harness.count("Log in to MyAnimeList") shouldBe 0
    }

    @Test
    fun credentialLoginOpensDialog() {
        show(emptyList())
        compose.runOnIdle { tracker("Kitsu").login() }
        compose.waitForIdle()
        harness.count("Log in to Kitsu") shouldBe 1
        harness.count("Email address") shouldBe 1
    }

    @Test
    fun usernameLoginOpensDialog() {
        show(emptyList())
        compose.runOnIdle { tracker("MangaUpdates").login() }
        compose.waitForIdle()
        harness.count("Username") shouldBe 1
    }

    @Test
    fun logoutOpensDialog() {
        show(emptyList())
        compose.runOnIdle { tracker("MyAnimeList").logout() }
        compose.waitForIdle()
        harness.count("Log out from MyAnimeList?") shouldBe 1
        compose.runOnIdle { tracker("Kitsu").logout() }
        compose.waitForIdle()
        harness.count("Log out from Kitsu?") shouldBe 1
    }
}
