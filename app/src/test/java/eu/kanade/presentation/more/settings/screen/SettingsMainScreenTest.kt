package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDexs
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SettingsMainScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val sourceManager = mockk<SourceManager> { every { getVisibleOnlineSources() } returns emptyList() }

    @Before
    fun setUp() {
        koin.start(module { single { sourceManager } })
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun show(twoPane: Boolean, open: Screen = SettingsLibraryScreen) {
        every { harness.navigator.items } returns listOf(open)
        compose.setContent {
            CompositionLocalProvider(
                LocalNavigator provides harness.navigator,
                LocalBackPress provides {},
            ) {
                MaterialTheme { SettingsMainScreen.Content(twoPane = twoPane) }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun singlePanePushes() {
        show(twoPane = false)
        tap("Library")
        verify { harness.navigator.push(SettingsLibraryScreen) }
        compose.onNodeWithContentDescription("Search").performClick()
        verify { harness.navigator.push(any<SettingsSearchScreen>()) }
        harness.count("E-Hentai") shouldBe 1
        harness.count("MangaDex") shouldBe 0
    }

    @Test
    fun twoPaneReplaces() {
        show(twoPane = true)
        tap("Appearance")
        verify { harness.navigator.replaceAll(SettingsAppearanceScreen) }
        compose.onNodeWithContentDescription("Search").performClick()
        verify { harness.navigator.replaceAll(any<SettingsSearchScreen>()) }
    }

    @Test
    fun twoPaneFirstEntryOpen() {
        show(twoPane = true, open = SettingsAppearanceScreen)
        compose.onNodeWithText("Appearance").assertExists()
    }

    @Test
    @Config(qualifiers = "+night")
    fun twoPaneDarkSurface() {
        show(twoPane = true)
        compose.onNodeWithText("Library").assertExists()
    }

    @Test
    fun optionalSectionsShown() {
        koin.exh.isHentaiEnabled.set(false)
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDexs(any(), any()) } returns listOf(mockk<MangaDex>())
        show(twoPane = false)
        harness.count("E-Hentai") shouldBe 0
        harness.count("MangaDex") shouldBe 1
    }
}
