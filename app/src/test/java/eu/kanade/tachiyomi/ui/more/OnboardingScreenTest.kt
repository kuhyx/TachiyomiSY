package eu.kanade.tachiyomi.ui.more

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.onboarding.OnboardingKoin
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class OnboardingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val koin = OnboardingKoin()
    private val base = BasePreferences(mockk(relaxed = true), MapPreferenceStore())
    private val registry = FakeResultRegistry()
    private val uriHandler = mockk<UriHandler>(relaxed = true)
    private lateinit var navigator: Navigator

    @Before
    fun setUp() {
        koin.start()
        loadKoinModules(module { single { base } })
        registry.answer = { Uri.fromFile(File(System.getProperty("java.io.tmpdir")!!)) }
    }

    @After
    fun tearDown() {
        SearchableSettings.highlightKey = null
        stopKoin()
    }

    private fun show() {
        val screen = OnboardingScreen()
        compose.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registry.owner(),
                LocalUriHandler provides uriHandler,
            ) {
                MaterialTheme {
                    Navigator(listOf(BlankScreen(), screen)) {
                        navigator = it
                        screen.Content()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun toLastStep() {
        tap("Next")
        tap("Select a folder")
        tap("Next")
        tap("Next")
    }

    @Test
    fun finishingPopsAndRemembers() {
        show()
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.activity.isFinishing shouldBe false
        toLastStep()
        tap("Get started")
        base.shownOnboardingFlow.get() shouldBe true
        navigator.lastItem.shouldBeInstanceOf<BlankScreen>()
    }

    @Test
    fun restoreOpensDataSettings() {
        show()
        toLastStep()
        tap("Restore backup")
        base.shownOnboardingFlow.get() shouldBe true
        navigator.lastItem.shouldBeInstanceOf<SettingsScreen>()
        SearchableSettings.highlightKey shouldBe "Backup and restore"
    }
}
