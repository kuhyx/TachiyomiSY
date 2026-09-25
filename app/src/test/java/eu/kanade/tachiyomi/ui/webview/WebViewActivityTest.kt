package eu.kanade.tachiyomi.ui.webview

import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.toShareIntent
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Headers
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
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class WebViewActivityTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val activityKoin = ActivityKoin()
    private val jar = mockk<AndroidCookieJar> { every { remove(any(), any(), any()) } returns 1 }
    private val http = mockk<HttpSource> { every { headers } returns Headers.headersOf("a", "b") }
    private val sourceManager = mockk<SourceManager>()

    @Before
    fun setUp() {
        mockkObject(WebViewUtil)
        every { WebViewUtil.supportsWebView(any()) } returns true
        every { sourceManager.get(any<Long>()) } returns null
        every { sourceManager.get(1L) } returns http
        startKoin {
            modules(
                activityKoin.module(),
                module {
                    single { sourceManager }
                    single { mockk<NetworkHelper> { every { cookieJar } returns jar } }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private fun launch(intent: Intent = WebViewActivity.newIntent(activityKoin.application, URL, 1L, "Title")) =
        ActivityScenario.launch<WebViewActivity>(intent)

    @Test
    fun missingWebViewFinishes() {
        every { WebViewUtil.supportsWebView(any()) } returns false
        launch().state shouldBe Lifecycle.State.DESTROYED
        ShadowToast.getTextOfLatestToast() shouldBe "WebView is required for the app to function"
    }

    @Test
    fun missingUrlShowsNothing() {
        val scenario = launch(Intent(activityKoin.application, WebViewActivity::class.java))
        scenario.onActivity { it.isFinishing shouldBe false }
        val titleOnly = Intent(activityKoin.application, WebViewActivity::class.java).putExtra("title_key", "t")
        launch(titleOnly).onActivity { activity ->
            val content = AssistContent()
            activity.onProvideAssistContent(content)
            content.webUri.shouldBeNull()
        }
    }

    @Test
    fun pageIsShownWithItsTitle() {
        launch().onActivity { activity ->
            val content = AssistContent()
            activity.onProvideAssistContent(content)
            content.webUri.toString() shouldBe URL
        }
        compose.onNodeWithText("Title").assertExists()
    }

    @Test
    fun otherSourcesAndFailures() {
        val otherSource = WebViewActivity.newIntent(activityKoin.application, URL, sourceId = 2L)
        launch(otherSource).state shouldBe Lifecycle.State.RESUMED
        every { http.headers } throws IllegalStateException("headers")
        launch().state shouldBe Lifecycle.State.RESUMED
    }

    @Test
    fun overflowReachesActivity() {
        val scenario = launch()
        overflow("Clear cookies")
        verify { jar.remove(any(), any(), any()) }
        overflow("Open in browser")
        overflow("Share")
        scenario.onActivity { activity ->
            val started = generateSequence { shadowOf(activity).nextStartedActivity }.toList()
            started.map { it.action } shouldBe listOf(Intent.ACTION_CHOOSER, Intent.ACTION_VIEW)
        }
    }

    @Test
    fun failedShareToasts() {
        mockkStatic("eu.kanade.tachiyomi.util.system.IntentExtensionsKt")
        every { any<Uri>().toShareIntent(any(), any(), any()) } throws IllegalStateException("no share")
        launch()
        overflow("Share")
        ShadowToast.getTextOfLatestToast() shouldBe "no share"
    }

    @Test
    fun navigatingUpFinishes() {
        val scenario = launch()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        scenario.onActivity { it.isFinishing shouldBe true }
    }

    @Test
    @Config(sdk = [33])
    fun legacyTransitionsWork() {
        val scenario = launch()
        scenario.onActivity {
            it.finish()
            it.isFinishing shouldBe true
        }
    }

    private fun overflow(title: String) {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }
}

private const val URL = "https://example.org/page"
