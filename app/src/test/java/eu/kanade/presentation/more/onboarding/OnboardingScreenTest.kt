package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = OnboardingKoin()
    private val registry = FakeResultRegistry()
    private val uriHandler = mockk<UriHandler>(relaxed = true)
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        koin.start()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registry.owner(),
                LocalUriHandler provides uriHandler,
            ) {
                MaterialTheme {
                    OnboardingScreen(onComplete = { events += "done" }, onRestoreBackup = { events += "restore" })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun walksAllSteps() {
        registry.answer = { Uri.fromFile(File(System.getProperty("java.io.tmpdir")!!)) }
        show()
        tap("Next")
        compose.onNodeWithText("Next").assertIsNotEnabled()
        tap("Storage guide")
        verify { uriHandler.openUri("https://mihon.app/docs/faq/storage") }
        tap("Select a folder")
        tap("Next")
        tap("Next")
        tap("Getting started guide")
        tap("Restore backup")
        tap("Get started")
        events shouldBe listOf("restore", "done")
    }

    @Test
    fun backGoesToPreviousStep() {
        show()
        tap("Next")
        compose.onNodeWithText("Select a folder").assertExists()
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Select a folder").assertDoesNotExist()
    }

    @Test
    fun missingPickerToasts() {
        registry.failure = ActivityNotFoundException()
        show()
        tap("Next")
        tap("Select a folder")
        ShadowToast.getTextOfLatestToast().toString() shouldBe "No file picker app found"
    }
}
