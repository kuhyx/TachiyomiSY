package eu.kanade.tachiyomi.ui.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class WebViewScreenModelTest {
    private val jar = mockk<AndroidCookieJar>()
    private val network = mockk<NetworkHelper> { every { cookieJar } returns jar }
    private val http = mockk<HttpSource> { every { headers } returns Headers.headersOf("User-Agent", "ua") }
    private val sourceManager = mockk<SourceManager> {
        every { get(1L) } returns http
        every { get(2L) } returns mockk<Source>()
        every { get(3L) } returns null
    }

    @After
    fun tearDown() = stopKoin()

    private fun model(sourceId: Long?) = WebViewScreenModel(sourceId, sourceManager, network)

    @Test
    fun httpSourceHeadersAreCopied() {
        model(1L).headers shouldBe mapOf("user-agent" to "ua")
    }

    @Test
    fun otherSourcesHaveNoHeaders() {
        model(null).headers shouldBe emptyMap()
        model(2L).headers shouldBe emptyMap()
        model(3L).headers shouldBe emptyMap()
    }

    @Test
    fun brokenHeadersAreLogged() {
        every { http.headers } throws IllegalStateException("no")
        model(1L).headers shouldBe emptyMap()
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single { network }
                },
            )
        }
        WebViewScreenModel(1L).sourceId shouldBe 1L
    }

    @Test
    fun sharingStartsAChooser() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        model(null).shareWebpage(activity, "https://example.org")
        shadowOf(activity).nextStartedActivity.action shouldBe Intent.ACTION_CHOOSER
    }

    @Test
    fun failedSharingToasts() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val refusing = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent?) = throw ActivityNotFoundException("none")
        }
        model(null).shareWebpage(refusing, "https://example.org")
        ShadowToast.getTextOfLatestToast() shouldBe "none"
    }

    @Test
    fun openingUsesTheBrowser() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        model(null).openInBrowser(activity, "https://example.org")
        shadowOf(activity).nextStartedActivity.dataString shouldBe "https://example.org"
    }

    @Test
    fun cookiesAreClearedForUrls() {
        every { jar.remove(any(), any(), any()) } returns 2
        model(null).clearCookies("https://example.org/a")
        model(null).clearCookies("not a url")
        verify(exactly = 1) { jar.remove("https://example.org/a".toHttpUrl(), any(), any()) }
        verify(exactly = 1) { jar.remove(any(), any(), any()) }
    }
}
