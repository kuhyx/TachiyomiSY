package eu.kanade.tachiyomi.util

import io.kotest.matchers.shouldBe
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test

private const val HTML: String =
    """<div><p class="a" data-x="1">First</p><p class="a">Second</p><span class="n">42</span></div>"""

internal class JsoupExtensionsTest {
    private val root: Element = Jsoup.parse(HTML).body()

    @Test
    fun selectTextReturnsFirstMatch() {
        root.selectText("p.a") shouldBe "First"
    }

    @Test
    fun selectTextFallsBackToDefault() {
        root.selectText("p.missing") shouldBe null
        root.selectText("p.missing", "fallback") shouldBe "fallback"
    }

    @Test
    fun selectIntParsesFirstMatch() {
        root.selectInt("span.n") shouldBe 42
    }

    @Test
    fun selectIntFallsBackToDefault() {
        root.selectInt("span.missing") shouldBe 0
        root.selectInt("span.missing", 7) shouldBe 7
    }

    @Test
    fun attrOrTextReadsAttributeOrText() {
        val first = root.selectFirst("p.a") ?: error("fixture has p.a")
        first.attrOrText("data-x") shouldBe "1"
        first.attrOrText("text") shouldBe "First"
    }

    @Test
    fun asJsoupParsesBodyAtRequestUrl() {
        val document = response("<a href='/rel'>x</a>").asJsoup()
        document.location() shouldBe "https://example.org/page"
        document.selectFirst("a")?.absUrl("href") shouldBe "https://example.org/rel"
    }

    @Test
    fun asJsoupPrefersGivenHtml() {
        val document = response("<p>body</p>").asJsoup("<p>given</p>")
        document.selectText("p") shouldBe "given"
    }
}

internal fun response(body: String, url: String = "https://example.org/page", code: Int = 200): Response =
    Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("stub")
        .body(body.toResponseBody())
        .build()
