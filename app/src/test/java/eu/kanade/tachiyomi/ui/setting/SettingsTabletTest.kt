package eu.kanade.tachiyomi.ui.setting

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.more.settings.screen.SettingsKoin
import eu.kanade.presentation.more.settings.screen.allScreensModules
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import eu.kanade.tachiyomi.util.CrashLogUtil
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The tablet layout: the settings list beside the open screen, the destination picking the latter. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw800dp-w1280dp-h2000dp")
internal class SettingsTabletTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()

    @Before
    fun setUp() {
        mockkConstructor(CrashLogUtil::class)
        every { anyConstructed<CrashLogUtil>().getDebugInfo() } returns "debug info"
        koin.start(
            *allScreensModules(),
            module {
                single { mockk<ExtensionManager>(relaxed = true) }
                single { XML.v1 { policy { ignoreUnknownChildren() } } }
            },
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun show(screen: SettingsScreen, title: String): ScreenHost {
        val host = ScreenHost(screen)
        host.show(compose)
        compose.waitForLabel(title)
        return host
    }

    private fun navigateUp() {
        compose.onAllNodesWithContentDescription("Navigate up").onFirst().performClick()
        compose.waitForIdle()
    }

    @Test
    fun appearanceIsTheDefault() {
        val host = show(SettingsScreen(), "Theme")
        navigateUp()
        host.top.shouldBeInstanceOf<BlankScreen>()
    }

    @Test
    fun aboutDestination() {
        show(SettingsScreen(SettingsScreen.Destination.About), "Version")
    }

    @Test
    fun dataDestination() {
        show(SettingsScreen(SettingsScreen.Destination.DataAndStorage), "Storage location")
    }

    @Test
    fun trackingDestination() {
        val host = show(SettingsScreen(SettingsScreen.Destination.Tracking), "Tracking")
        navigateUp()
        host.top.shouldBeInstanceOf<BlankScreen>()
    }
}
