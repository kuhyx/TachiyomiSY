package eu.kanade.tachiyomi.network

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Test

internal class RequestsBodyTest {
    private val headers = Headers.headersOf("X-Test", "1")
    private val noStore = CacheControl.Builder().noStore().build()
    private val body = "payload".toRequestBody("text/plain".toMediaType())

    private val builders: Map<String, (String, Headers, RequestBody, CacheControl) -> Request> = mapOf(
        "POST" to { url, h, b, c -> POST(url = url, headers = h, body = b, cache = c) },
        "PUT" to { url, h, b, c -> PUT(url = url, headers = h, body = b, cache = c) },
        "PATCH" to { url, h, b, c -> PATCH(url = url, headers = h, body = b, cache = c) },
        "DELETE" to { url, h, b, c -> DELETE(url = url, headers = h, body = b, cache = c) },
    )

    @Test
    fun explicitArgumentsAreApplied() {
        builders.forEach { (method, build) ->
            withClue(method) {
                val request = build(TEST_URL, headers, body, noStore)
                request.method shouldBe method
                request.header("X-Test") shouldBe "1"
                request.body shouldBe body
                request.cacheControl.noStore shouldBe true
            }
        }
    }

    @Test
    fun postDefaults() {
        assertDefaults(POST(TEST_URL), "POST")
    }

    @Test
    fun putDefaults() {
        assertDefaults(PUT(TEST_URL), "PUT")
    }

    @Test
    fun patchDefaults() {
        assertDefaults(PATCH(TEST_URL), "PATCH")
    }

    @Test
    fun deleteDefaults() {
        assertDefaults(DELETE(TEST_URL), "DELETE")
    }

    @Test
    fun partialArgumentsKeepDefaults() {
        val request = POST(TEST_URL, headers)
        request.header("X-Test") shouldBe "1"
        request.body.shouldBeInstanceOf<FormBody>()
        request.cacheControl.maxAgeSeconds shouldBe 600
    }

    private fun assertDefaults(request: Request, method: String) {
        request.method shouldBe method
        request.headers.names() shouldBe setOf("Cache-Control")
        request.body.shouldBeInstanceOf<FormBody>().size shouldBe 0
        request.cacheControl.maxAgeSeconds shouldBe 600
    }
}
