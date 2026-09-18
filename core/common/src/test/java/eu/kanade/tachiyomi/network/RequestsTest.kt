package eu.kanade.tachiyomi.network

import io.kotest.matchers.shouldBe
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

internal class RequestsTest {
    private val headers = Headers.headersOf("X-Test", "1")
    private val noStore = CacheControl.Builder().noStore().build()

    @Test
    fun getFromStringUsesDefaults() {
        val request = GET(TEST_URL)
        request.method shouldBe "GET"
        request.url shouldBe TEST_URL.toHttpUrl()
        request.headers.names() shouldBe setOf("Cache-Control")
        request.cacheControl.maxAgeSeconds shouldBe 600
        request.body shouldBe null
    }

    @Test
    fun getFromStringHonoursArguments() {
        val request = GET(TEST_URL, headers, noStore)
        request.header("X-Test") shouldBe "1"
        request.cacheControl.noStore shouldBe true
    }

    @Test
    fun getFromUrlUsesDefaults() {
        val request = GET(TEST_URL.toHttpUrl())
        request.method shouldBe "GET"
        request.headers.names() shouldBe setOf("Cache-Control")
        request.cacheControl.maxAgeSeconds shouldBe 600
    }

    @Test
    fun getFromUrlHonoursArguments() {
        val request = GET(TEST_URL.toHttpUrl(), headers, noStore)
        request.header("X-Test") shouldBe "1"
        request.cacheControl.noStore shouldBe true
    }

    @Test
    fun getFromStringWithHeadersOnly() {
        val request = GET(TEST_URL, headers)
        request.header("X-Test") shouldBe "1"
        request.cacheControl.maxAgeSeconds shouldBe 600
    }
}
