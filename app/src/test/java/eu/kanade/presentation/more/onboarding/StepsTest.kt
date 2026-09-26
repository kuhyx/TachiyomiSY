package eu.kanade.presentation.more.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.ui.model.ThemeMode
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
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.storage.service.StoragePreferences

@RunWith(RobolectricTestRunner::class)
internal class StepsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = OnboardingKoin()

    @Before
    fun setUp() {
        koin.start()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun themeStepStoresMode() {
        val step = ThemeStep()
        compose.setContent { MaterialTheme { step.Content() } }
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()
        koin.ui.themeMode.get() shouldBe ThemeMode.DARK
        step.isComplete shouldBe true
    }

    @Test
    fun guidesPreviewRenders() {
        compose.setContent { GuidesStepPreview() }
        compose.onNodeWithText("Getting started guide").assertExists()
        GuidesStep(onRestoreBackup = {}).isComplete shouldBe true
    }

    @Test
    fun storageStepOutlivesItsChanges() {
        // InMemoryPreferenceStore's changes() completes at once, so the step's collector runs to its end.
        stopKoin()
        startKoin {
            modules(module { single { StoragePreferences(mockk(relaxed = true), InMemoryPreferenceStore()) } })
        }
        val step = StorageStep()
        compose.setContent { MaterialTheme { step.Content() } }
        compose.waitForIdle()
        step.isComplete shouldBe false
    }
}
