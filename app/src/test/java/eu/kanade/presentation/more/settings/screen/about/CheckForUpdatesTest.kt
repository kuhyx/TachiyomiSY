package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release

@RunWith(RobolectricTestRunner::class)
internal class CheckForUpdatesTest {
    @get:Rule
    val compose = createComposeRule()

    private val navigator = mockk<Navigator>(relaxed = true)
    private val gate = CompletableDeferred<GetApplicationRelease.Result>()

    @Before
    fun setUp() {
        mockkConstructor(AppUpdateChecker::class)
        coEvery {
            anyConstructed<AppUpdateChecker>().checkForUpdate(any(), forceCheck = true)
        } coAnswers { gate.await() }
        compose.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                MaterialTheme { CheckForUpdatesItem() }
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun check(result: GetApplicationRelease.Result?) {
        compose.onNodeWithText("Check for updates").performClick()
        compose.waitForIdle()
        // A second tap while the check runs is ignored.
        compose.onNodeWithText("Check for updates").performClick()
        if (result == null) {
            gate.completeExceptionally(IllegalStateException("offline"))
        } else {
            gate.complete(result)
        }
        compose.waitUntil(timeoutMillis = 10_000) { gate.isCompleted }
        compose.waitForIdle()
    }

    @Test
    fun newUpdateOpensScreen() {
        check(GetApplicationRelease.Result.NewUpdate(Release("v2", "notes", "https://r", listOf("a.apk"))))
        verify(exactly = 1) { navigator.push(any<NewUpdateScreen>()) }
    }

    @Test
    fun noUpdateToasts() {
        check(GetApplicationRelease.Result.NoNewUpdate)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        ShadowToast.getTextOfLatestToast().toString() shouldBe "No new updates available"
    }

    @Test
    fun oldOsToasts() {
        check(GetApplicationRelease.Result.OsTooOld)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        ShadowToast.getTextOfLatestToast().toString() shouldBe "This Android version is no longer supported"
    }

    @Test
    fun failureToastsMessage() {
        check(result = null)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        ShadowToast.getTextOfLatestToast().toString() shouldBe "offline"
    }
}
