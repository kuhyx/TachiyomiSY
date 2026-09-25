package exh.ui.login

import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import exh.source.ExhPreferences
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

private const val FORUMS = "https://forums.e-hentai.org/index.php"
private const val EXH = "https://exhentai.org/"

@RunWith(RobolectricTestRunner::class)
internal class EhLoginPageHandlerTest {
    private val preferences = ExhPreferences(InMemoryPreferenceStore())
    private val view = mockk<WebView>(relaxed = true)
    private val callback = slot<ValueCallback<String>>()
    private var loggedIn = 0
    private val handler = EhLoginPageHandler { loggedIn++ }

    @Before
    fun setUp() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
        CookieManager.getInstance().removeAllCookies(null)
        every { view.evaluateJavascript(any(), capture(callback)) } answers { }
    }

    @After
    fun tearDown() = stopKoin()

    private fun forumCookies() {
        CookieManager.getInstance().setCookie(FORUMS, "ipb_member_id=12")
        CookieManager.getInstance().setCookie(FORUMS, "ipb_pass_hash=hash")
    }

    private fun finishForums(url: String, scriptResult: String) {
        handler.onPageFinished(view, url, customIgneous = null)
        callback.captured.onReceiveValue(scriptResult)
    }

    @Test
    fun cloudflareStopsTheFlow() {
        forumCookies()
        finishForums(FORUMS, "true")
        verify(exactly = 1) { view.evaluateJavascript(any(), any()) }
        verify(exactly = 0) { view.loadUrl(any<String>()) }
    }

    @Test
    fun forumsGetRestyledAndLoggedIn() {
        forumCookies()
        finishForums(FORUMS, "false")
        verify { view.evaluateJavascript(EhLoginActivity.HIDE_JS, null) }
        verify { view.loadUrl(EXH) }
    }

    @Test
    fun skipInjectKeepsTheStyle() {
        finishForums("$FORUMS?${EhLoginActivity.PARAM_SKIP_INJECT}=true&code=1", "false")
        verify(exactly = 0) { view.evaluateJavascript(EhLoginActivity.HIDE_JS, null) }
        verify(exactly = 0) { view.loadUrl(any<String>()) }
    }

    @Test
    fun aFailedCodeStaysOnTheForums() {
        forumCookies()
        finishForums("https://FORUMS.e-hentai.org/index.php?code=0", "false")
        verify(exactly = 0) { view.loadUrl(any<String>()) }
    }

    @Test
    fun halfLoggedInStaysOnTheForums() {
        CookieManager.getInstance().setCookie(FORUMS, "ipb_member_id=12")
        finishForums("$FORUMS?code=1", "false")
        verify(exactly = 0) { view.loadUrl(any<String>()) }
    }

    @Test
    fun exhentaiCookiesAreStored() {
        CookieManager.getInstance().setCookie(EXH, "ipb_member_id=12")
        CookieManager.getInstance().setCookie(EXH, "ipb_pass_hash=hash")
        CookieManager.getInstance().setCookie(EXH, "igneous=site")
        handler.onPageFinished(view, EXH, customIgneous = null)
        loggedIn shouldBe 1
        preferences.enableExhentai.get() shouldBe true
        preferences.memberIdVal.get() shouldBe "12"
        preferences.passHashVal.get() shouldBe "hash"
        preferences.igneousVal.get() shouldBe "site"
    }

    @Test
    fun aCustomIgneousWins() {
        CookieManager.getInstance().setCookie(EXH, "ipb_member_id=12")
        CookieManager.getInstance().setCookie(EXH, "ipb_pass_hash=hash")
        handler.onPageFinished(view, EXH, customIgneous = "mine")
        loggedIn shouldBe 1
        preferences.igneousVal.get() shouldBe "mine"
        CookieManager.getInstance().getCookie(EXH).contains("igneous=mine") shouldBe true
    }

    @Test
    fun missingCookiesAreNotALogin() {
        CookieManager.getInstance().setCookie(EXH, "ipb_member_id=12")
        handler.onPageFinished(view, EXH, customIgneous = null)
        loggedIn shouldBe 0
        preferences.enableExhentai.get() shouldBe false
    }

    @Test
    fun noCookiesAtAllAreNotALogin() {
        handler.onPageFinished(view, EXH, customIgneous = null)
        loggedIn shouldBe 0
    }

    @Test
    fun otherHostsAreIgnored() {
        handler.onPageFinished(view, "https://e-hentai.org/", customIgneous = null)
        loggedIn shouldBe 0
        verify(exactly = 0) { view.evaluateJavascript(any(), any()) }
    }
}
