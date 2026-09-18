package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import io.kotest.matchers.shouldBe
import okhttp3.Headers
import okhttp3.Request
import org.junit.jupiter.api.Test

internal class UserAgentInterceptorTest {
    private val server = CannedServer { cannedResponse(it) }
    private val client = clientOf(server, UserAgentInterceptor { "default/1.0" })

    @Test
    fun addsTheDefaultWhenMissing() {
        sent(GET(TEST_URL)).header("User-Agent") shouldBe "default/1.0"
    }

    @Test
    fun replacesAnEmptyUserAgent() {
        val request = GET(TEST_URL, Headers.headersOf("User-Agent", ""))
        val sent = sent(request)
        sent.headers("User-Agent") shouldBe listOf("default/1.0")
    }

    @Test
    fun keepsAnExplicitUserAgent() {
        sent(GET(TEST_URL, Headers.headersOf("User-Agent", "custom/2"))).header("User-Agent") shouldBe "custom/2"
    }

    private fun sent(request: Request): Request {
        client.newCall(request).execute().close()
        return server.requests.single()
    }
}
