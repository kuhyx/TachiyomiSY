package eu.kanade.tachiyomi.ui.webview

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.base.ScreenHost
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class WebViewScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val jar = mockk<AndroidCookieJar> { every { remove(any(), any(), any()) } returns 1 }
    private val sourceManager = mockk<SourceManager>()

    @Before
    fun setUp() {
        every { sourceManager.get(any<Long>()) } returns null
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single { mockk<NetworkHelper> { every { cookieJar } returns jar } }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun overflow(title: String) {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    @Test
    fun actionsGoThroughTheModel() {
        val screen = WebViewScreen(URL, initialTitle = "Title", sourceId = 1L)
        screen.onProvideAssistUrl().shouldBeNull()
        compose.setContent { ScreenHost(screen) }
        compose.onNodeWithText("Title").assertExists()
        overflow("Clear cookies")
        verify { jar.remove(any(), any(), any()) }
        overflow("Open in browser")
        overflow("Share")
        val started = generateSequence { shadowOf(compose.activity).nextStartedActivity }.toList()
        started.map(Intent::getAction) shouldBe listOf(Intent.ACTION_CHOOSER, Intent.ACTION_VIEW)
    }

    @Test
    fun navigatingUpPops() {
        val root = WebViewScreen(URL)
        compose.setContent { ScreenHost(root) }
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
    }
}

private const val URL = "https://example.org/page"
