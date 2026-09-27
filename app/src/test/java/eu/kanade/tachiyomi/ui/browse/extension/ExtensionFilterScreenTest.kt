package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

private const val INTERNAL_ERROR = "InternalError: Check crash logs for further information"

@RunWith(RobolectricTestRunner::class)
internal class ExtensionFilterScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val getLanguages = mockk<GetExtensionLanguages>()
    private val toggle = mockk<ToggleLanguage>(relaxed = true)
    private val host = ScreenHost(ExtensionFilterScreen())

    @Before
    fun setUp() = koin.start(
        module {
            single { getLanguages }
            single { toggle }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun languagesToggle() {
        every { getLanguages.subscribe() } returns MutableStateFlow(listOf("en", "fr"))
        host.show(compose)
        compose.pollLabel("English")
        compose.onNodeWithText("English").performClick()
        verify(timeout = 5_000) { toggle.await("en") }
    }

    @Test
    fun loadingShowsSpinner() {
        every { getLanguages.subscribe() } returns MutableSharedFlow()
        host.show(compose)
        compose.onNodeWithText("English").assertDoesNotExist()
    }

    // A failure on the first emission arrives while the screen still shows its spinner.
    @Test
    fun earlyFailureToasts() {
        every { getLanguages.subscribe() } returns flow { error("offline") }
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.getTextOfLatestToast() == INTERNAL_ERROR }
        compose.pollLabel("Well, this is awkward")
        host.top.javaClass shouldBe ExtensionFilterScreen::class.java
    }

    @Test
    fun lateFailureToasts() {
        // The failure is held until the list is on screen, so it is a genuinely late one.
        val fail = CompletableDeferred<Unit>()
        every { getLanguages.subscribe() } returns flow {
            emit(listOf("en"))
            fail.await()
            error("offline")
        }
        host.show(compose)
        compose.pollLabel("English")
        fail.complete(Unit)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.getTextOfLatestToast() == INTERNAL_ERROR }
        compose.pollLabel("English")
    }
}
