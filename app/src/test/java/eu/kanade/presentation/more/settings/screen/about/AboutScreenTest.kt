package eu.kanade.presentation.more.settings.screen.about

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.CrashLogUtil
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class AboutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val uriHandler = mockk<UriHandler>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(CrashLogUtil::class)
        every { anyConstructed<CrashLogUtil>().getDebugInfo() } returns "debug info"
        stopKoin()
        val store = InMemoryPreferenceStore()
        val context = ApplicationProvider.getApplicationContext<Context>()
        startKoin {
            modules(
                module {
                    single { UiPreferences(store) }
                    single { BasePreferences(context, store) }
                    single { mockk<ExtensionManager>(relaxed = true) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun show(back: (() -> Unit)?) {
        compose.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides uriHandler,
                LocalBackPress provides back,
                LocalNavigator provides navigator,
            ) {
                MaterialTheme { AboutScreen.Content() }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun versionCopiesDebugInfo() {
        show(back = {})
        compose.onNodeWithText("Version").performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text.toString() shouldBe "debug info"
    }

    @Test
    fun linksOpen() {
        show(back = null)
        compose.onNodeWithText("Privacy policy").performClick()
        verify { uriHandler.openUri("https://mihon.app/privacy/") }
        compose.onNodeWithContentDescription("GitHub").performClick()
        verify { uriHandler.openUri("https://github.com/jobobby04/tachiyomisy") }
        compose.onNodeWithText("Open source licenses").performClick()
        verify { navigator.push(any<OpenSourceLicensesScreen>()) }
    }

    @Test
    fun versionNames() {
        AboutScreen.getVersionName(withBuildDate = false) shouldStartWith "Debug "
        AboutScreen.getVersionName(withBuildDate = true) shouldStartWith "Debug "
    }

    @Test
    fun buildTimeWithoutPreferences() {
        stopKoin()
        AboutScreen.getFormattedBuildTime() shouldBe BuildConfig.BUILD_TIME
    }
}
