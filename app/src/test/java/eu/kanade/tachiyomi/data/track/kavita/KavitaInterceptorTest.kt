package eu.kanade.tachiyomi.data.track.kavita

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KavitaInterceptorTest {

    private val harness = KavitaHarness()
    private val request = Request.Builder().url(KavitaHarness.SERIES_URL).build()

    @BeforeEach
    fun setUp() {
        harness.start(authenticated = false)
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun chain(): Interceptor.Chain = mockk {
        every { request() } returns request
        every { proceed(any()) } answers {
            Response.Builder().request(firstArg()).protocol(Protocol.HTTP_1_1).code(200).message("").build()
        }
    }

    @Test
    fun loadsTokensOnFirstUse() {
        harness.configureSources("" to "", "http://other/api" to "", KavitaHarness.API_URL to "key")
        harness.enqueue("auth.json")
        val response = KavitaInterceptor(harness.tracker).intercept(chain())
        response.request.header("Authorization") shouldBe "Bearer jwt-token"
        response.request.header("User-Agent").orEmpty() shouldStartWith "TachiyomiSY v"
        val auths = checkNotNull(harness.tracker.authentications).authentications
        auths.map { it.sourceId } shouldBe listOf(1, 2, 3)
        auths[2] shouldBe SourceAuth(sourceId = 3, apiUrl = KavitaHarness.API_URL, jwtToken = "jwt-token")
    }

    /** Only a stubbed [Kavita.loadOAuth] leaves the authentications unset after the first use. */
    @Test
    fun unloadedAuthGetsNoToken() {
        val kavita = spyk(harness.tracker)
        every { kavita.loadOAuth() } just Runs
        val response = KavitaInterceptor(kavita).intercept(chain())
        response.request.header("Authorization") shouldBe "Bearer null"
        verify(exactly = 1) { kavita.loadOAuth() }
    }

    @Test
    fun unknownServerGetsNoToken() {
        harness.tracker.authentications = OAuth()
        val response = KavitaInterceptor(harness.tracker).intercept(chain())
        response.request.header("Authorization") shouldBe "Bearer null"
    }
}
