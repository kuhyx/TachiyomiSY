package eu.kanade.tachiyomi.ui.more

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.storage.service.StoragePreferences

@RunWith(RobolectricTestRunner::class)
internal class OnboardingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val store = MapPreferenceStore()
    private val base = BasePreferences(ApplicationProvider.getApplicationContext<Application>(), store)
    private val storage = StoragePreferences(mockk(relaxed = true), store)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { base }
                    single { UiPreferences(store) }
                    single { PrivacyPreferences(store) }
                    single { storage }
                },
            )
        }
        // A storage folder is already chosen, so every step can be passed.
        storage.baseStorageDirectory.set("content://folder")
        compose.setContent { StackHost(OnboardingScreen()) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun click(label: String) {
        compose.waitUntil(WAIT) { compose.onAllNodes(hasText(label) and hasClickAction()).fetchSemanticsNodes().any() }
        compose.onAllNodes(hasText(label) and hasClickAction()).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun toTheGuides() = repeat(GUIDE_STEP) { click("Next") }

    @Test
    fun backIsBlockedUntilFinished() {
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.onNode(hasText("Welcome!")).assertExists()
    }

    @Test
    fun finishing() {
        toTheGuides()
        click("Get started")
        compose.onNode(hasText("root")).assertExists()
        base.shownOnboardingFlow.get() shouldBe true
    }

    @Test
    fun restoringABackup() {
        toTheGuides()
        click("Restore backup")
        compose.onNode(hasText("opened:SettingsScreen")).assertExists()
        SearchableSettings.highlightKey shouldBe "Restore backup"
    }

    private companion object {
        const val WAIT = 5_000L
        const val GUIDE_STEP = 3
    }
}
