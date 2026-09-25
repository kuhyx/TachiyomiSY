package exh.util

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OkHttpUtilTest {
    @BeforeEach
    fun initLogging() {
        XLog.init(LogConfiguration.Builder().logLevel(LogLevel.ALL).build(), Printer { _, _, _ -> })
    }

    private fun response(body: String, type: String?) = Response.Builder()
        .request(Request.Builder().url("https://example.test/").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody(type?.toMediaType()))
        .build()

    @Test
    fun htmlBodyIsParsedAndRebuilt() {
        val original = response("<html><body><p id=\"x\">hi</p></body></html>", "text/html")
        var seen: String? = null
        val rebuilt = original.interceptAsHtml { seen = it.selectFirst("#x")?.text() }
        seen shouldBe "hi"
        rebuilt.body.string() shouldBe "<html><body><p id=\"x\">hi</p></body></html>"
        rebuilt.body.contentType()?.subtype shouldBe "html"
    }

    @Test
    fun failingBlockIsSwallowed() {
        val original = response("<p>x</p>", "text/html")
        val rebuilt = original.interceptAsHtml { error("nope") }
        rebuilt.body.string() shouldBe "<p>x</p>"
    }

    @Test
    fun nonHtmlIsReturnedUntouched() {
        var called = false
        val json = response("{}", "application/json")
        json.interceptAsHtml { called = true } shouldBeSameInstanceAs json
        val plain = response("x", "text/plain")
        plain.interceptAsHtml { called = true } shouldBeSameInstanceAs plain
        val untyped = response("x", null)
        untyped.interceptAsHtml { called = true } shouldBeSameInstanceAs untyped
        called shouldBe false
    }

    @Test
    fun vanishingContentTypeUntouched() {
        val body = mockk<ResponseBody>()
        every { body.contentType() } returnsMany listOf("text/html".toMediaType(), null)
        val flaky = response("x", null).newBuilder().body(body).build()
        flaky.interceptAsHtml { error("never") } shouldBeSameInstanceAs flaky
    }
}
