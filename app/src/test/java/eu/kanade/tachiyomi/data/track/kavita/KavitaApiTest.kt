package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.track.mockServerClient
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/** [KavitaApi.getNewToken] and the URL helpers. */
internal class KavitaApiTest {

    private val harness = KavitaHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun apiFromUrl() {
        harness.api.getApiFromUrl(KavitaHarness.SERIES_URL) shouldBe KavitaHarness.API_URL
        harness.api.getApiFromUrl("http://host/api") shouldBe "http://host/api/api"
    }

    @Test
    fun tokenOnOk() {
        harness.enqueue("auth.json")
        harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "key") shouldBe "jwt-token"
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/api/Plugin/authenticate"
        request.url.queryParameter("apiKey") shouldBe "key"
        request.url.queryParameter("pluginName") shouldBe "Tachiyomi-Kavita"
        request.headers["Authorization"] shouldBe null
    }

    @Test
    fun unauthorizedIsAnError() {
        harness.enqueueRaw("", code = 401)
        val error = shouldThrow<IOException> { harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "") }
        // The generic handler wraps the status-specific IOException once more.
        error.cause?.message shouldBe "Unauthorized / api key not valid"
        harness.koin.logged.first() shouldBe
            "Unauthorized / API key not valid: API URL: ${KavitaHarness.API_URL}, empty API key: true"
        harness.enqueueRaw("", code = 401)
        shouldThrow<IOException> { harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "k") }
        harness.koin.logged.any { it.endsWith("empty API key: false") } shouldBe true
    }

    @Test
    fun serverErrorIsAnError() {
        harness.enqueueRaw("", code = 500)
        val error = shouldThrow<IOException> { harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "k") }
        error.cause?.message shouldBe "Error fetching JWT token"
        harness.koin.logged.first() shouldBe
            "Error fetching JWT token. API URL: ${KavitaHarness.API_URL}, empty API key: false"
        harness.enqueueRaw("", code = 500)
        shouldThrow<IOException> { harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "") }
        harness.koin.logged.any { it.endsWith("empty API key: true") } shouldBe true
    }

    @Test
    fun otherCodesGiveNoToken() {
        harness.enqueueRaw("", code = 404)
        harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "k").shouldBeNull()
    }

    @Test
    fun timeoutGivesNoToken() {
        val slowClient = mockServerClient(harness.server).newBuilder().readTimeout(200, TimeUnit.MILLISECONDS).build()
        val api = KavitaApi(slowClient, KavitaInterceptor(harness.tracker))
        harness.server.enqueue(MockResponse.Builder().headersDelay(2, TimeUnit.SECONDS).build())
        api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "k").shouldBeNull()
    }

    @Test
    fun unparseableTokenWrapsTheCause() {
        harness.enqueueRaw("not json")
        val error = shouldThrow<IOException> { harness.api.getNewToken(apiUrl = KavitaHarness.API_URL, apiKey = "k") }
        error.cause.shouldBeInstanceOf<SerializationException>()
        error.message.orEmpty() shouldStartWith "kotlinx.serialization"
    }
}
