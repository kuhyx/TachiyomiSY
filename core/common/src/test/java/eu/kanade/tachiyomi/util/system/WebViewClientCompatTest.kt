package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions

/** Records every compat hook; the deprecated finals are reached through reflection so they compile warning-free. */
internal class RecordingClient(private val response: WebResourceResponse? = null) : WebViewClientCompat() {
    val overridden = mutableListOf<String>()
    val intercepted = mutableListOf<String>()
    val errors = mutableListOf<List<Any?>>()

    override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
        overridden += url
        return true
    }

    override fun shouldInterceptRequestCompat(view: WebView, url: String): WebResourceResponse? {
        intercepted += url
        return response
    }

    override fun onReceivedErrorCompat(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String,
        isMainFrame: Boolean,
    ) {
        errors += listOf(errorCode, description, failingUrl, isMainFrame)
    }
}

@RunWith(RobolectricTestRunner::class)
internal class WebViewClientCompatTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val webView = WebView(context)

    private fun request(url: String, mainFrame: Boolean): WebResourceRequest {
        val request = mockk<WebResourceRequest>()
        every { request.url } returns Uri.parse(url)
        every { request.isForMainFrame } returns mainFrame
        return request
    }

    // The deprecated overload of `name` whose last parameter is a String, reached reflectively so
    // the call compiles without a deprecation warning.
    private fun legacy(name: String, arity: Int): KFunction<*> = WebViewClientCompat::class.declaredFunctions.single {
        it.name == name && it.parameters.size == arity && it.parameters.last().type.classifier == String::class
    }

    @Test
    fun overrideUrlUsesTheRequestUrl() {
        val client = RecordingClient()
        client.shouldOverrideUrlLoading(webView, request("https://a.test/", mainFrame = true)) shouldBe true
        client.overridden shouldContainExactly listOf("https://a.test/")
    }

    @Test
    fun legacyOverrideUrlPassesUrl() {
        val client = RecordingClient()
        legacy("shouldOverrideUrlLoading", arity = 3).call(client, webView, "https://b.test/") shouldBe true
        client.overridden shouldContainExactly listOf("https://b.test/")
    }

    @Test
    fun interceptReturnsCompatResponse() {
        val response = WebResourceResponse("text/plain", "utf-8", "x".byteInputStream())
        val client = RecordingClient(response)
        client.shouldInterceptRequest(webView, request("https://c.test/", mainFrame = false)) shouldBe response
        RecordingClient().shouldInterceptRequest(webView, request("https://c.test/", mainFrame = false)).shouldBeNull()
        client.intercepted shouldContainExactly listOf("https://c.test/")
    }

    @Test
    fun legacyInterceptPassesUrl() {
        val client = RecordingClient()
        legacy("shouldInterceptRequest", arity = 3).call(client, webView, "https://d.test/").shouldBeNull()
        client.intercepted shouldContainExactly listOf("https://d.test/")
    }

    @Test
    fun receivedErrorUnpacksTheError() {
        val client = RecordingClient()
        val error = mockk<WebResourceError> {
            every { errorCode } returns -2
            every { description } returns "net::ERR_FAILED"
        }
        client.onReceivedError(webView, request("https://e.test/", mainFrame = true), error)
        client.errors shouldContainExactly listOf(listOf(-2, "net::ERR_FAILED", "https://e.test/", true))
    }

    @Test
    fun errorToleratesNullDescription() {
        val client = RecordingClient()
        val error = mockk<WebResourceError> {
            every { errorCode } returns -6
            every { description } returns null
        }
        client.onReceivedError(webView, request("https://f.test/", mainFrame = false), error)
        client.errors shouldContainExactly listOf(listOf(-6, null, "https://f.test/", false))
    }

    @Test
    fun legacyErrorComparesCurrentUrl() {
        val client = RecordingClient()
        val method = legacy("onReceivedError", arity = 5)
        method.call(client, webView, -1, "desc", "https://g.test/")
        webView.loadUrl("https://g.test/")
        method.call(client, webView, -1, null, "https://g.test/")
        client.errors shouldContainExactly listOf(
            listOf(-1, "desc", "https://g.test/", false),
            listOf(-1, null, "https://g.test/", true),
        )
    }

    @Test
    fun httpErrorReportsStatus() {
        val client = RecordingClient()
        val response = WebResourceResponse("text/html", "utf-8", "".byteInputStream()).apply {
            setStatusCodeAndReasonPhrase(404, "Not Found")
        }
        client.onReceivedHttpError(webView, request("https://h.test/", mainFrame = true), response)
        client.errors shouldContainExactly listOf(listOf(404, "Not Found", "https://h.test/", true))
    }

    @Test
    fun defaultHooksDoNothing() {
        val client = object : WebViewClientCompat() {}
        client.shouldOverrideUrlCompat(webView, "u") shouldBe false
        client.shouldInterceptRequestCompat(webView, "u").shouldBeNull()
        client.onReceivedErrorCompat(
            view = webView,
            errorCode = 0,
            description = null,
            failingUrl = "u",
            isMainFrame = false,
        )
    }
}
