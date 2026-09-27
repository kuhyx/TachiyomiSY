package eu.kanade.presentation.crash

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class CrashScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        val extensions = mockk<ExtensionManager> {
            every { availableExtensionsFlow } returns MutableStateFlow(emptyList())
            every { installedExtensionsFlow } returns MutableStateFlow(emptyList())
        }
        val preferences = BasePreferences(ApplicationProvider.getApplicationContext(), InMemoryPreferenceStore())
        stopKoin()
        startKoin {
            modules(
                module {
                    single { extensions }
                    single { preferences }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun previewShowsTheException() {
        compose.setContent { CrashScreenPreview() }
        compose.waitForLabel("java.lang.RuntimeException: Dummy")
    }

    @Test
    fun sharingLogsWithoutLogcatToasts() {
        var restarted = 0
        compose.setContent { CrashScreen(exception = IllegalStateException("boom")) { restarted++ } }
        compose.waitForLabel("Share crash logs")
        compose.onNodeWithText("Restart the application").performClick()
        restarted shouldBe 1
        compose.onNodeWithText("Share crash logs").performClick()
        eventually { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to get logs"
    }
}
