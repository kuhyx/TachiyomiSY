package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.presentation.util.LocalBackPress
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class SearchableSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val uriHandler = mockk<UriHandler>(relaxed = true)

    @Before
    fun setUp() {
        val sourceManager = mockk<SourceManager> { every { getAll() } returns emptyList() }
        koin.start(
            module {
                single { stubTrackerManager(emptyList()) }
                single { sourceManager }
            },
        )
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    private fun show(screen: SearchableSettings, back: (() -> Unit)?) {
        compose.setContent {
            CompositionLocalProvider(
                LocalNavigator provides harness.navigator,
                LocalBackPress provides back,
                LocalUriHandler provides uriHandler,
            ) {
                MaterialTheme { screen.Content() }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun contentWithBackAndHelp() {
        var backs = 0
        show(SettingsTrackingScreen) { backs++ }
        compose.onNodeWithText("Tracking").assertExists()
        compose.onNodeWithContentDescription("Tracking guide").performClick()
        verify { uriHandler.openUri("https://mihon.app/docs/guides/tracking") }
        backs shouldBe 0
    }

    @Test
    fun contentWithoutBack() {
        show(SettingsReaderScreen, back = null)
        compose.onNodeWithText("Reader").assertExists()
        SettingsReaderScreen.isEnabled() shouldBe true
    }
}
