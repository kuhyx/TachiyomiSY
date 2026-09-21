package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.hikka.dto.hikkaOAuth
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class HikkaInterceptorTest {

    private val koin = TrackKoin()
    private lateinit var hikka: Hikka
    private val request = Request.Builder().url("https://api.hikka.io/manga/slug").build()
    private val now = System.currentTimeMillis() / 1000

    @BeforeEach
    fun setUp() {
        koin.start()
        hikka = Hikka(10L)
    }

    @AfterEach
    fun tearDown() {
        koin.stop()
    }

    private fun chain(answer: (Request) -> Response): Interceptor.Chain = mockk {
        every { request() } returns request
        every { proceed(any()) } answers { answer(firstArg()) }
    }

    private fun response(request: Request, code: Int, body: String = ""): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    private fun chainAnsweringProbeThenInfo(refreshCode: Int, infoCode: Int): Interceptor.Chain = chain {
        when (it.url.encodedPath) {
            "/user/me" -> response(it, refreshCode)
            "/auth/token/info" -> response(it, infoCode, fixture(TOKEN_INFO))
            else -> response(it, 200)
        }
    }

    @Test
    fun noTokenThrows() {
        val chain = chain { response(it, 200) }
        shouldThrow<IOException> { HikkaInterceptor(hikka).intercept(chain) }.message shouldBe
            "Hikka: You are not authorized"
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun freshTokenAddsHeaders() {
        hikka.saveOAuth(hikkaOAuth(expiration = now + 7200, accessToken = "fresh"))
        val chain = chain { response(it, 200) }
        val response = HikkaInterceptor(hikka).intercept(chain)
        response.request.header("auth") shouldBe "fresh"
        response.request.header("accept") shouldBe "application/json"
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun expiredTokenIsReValidated() {
        hikka.saveCredentials("user", "stale")
        hikka.saveOAuth(hikkaOAuth(expiration = now - 10, accessToken = "stale"))
        val chain = chainAnsweringProbeThenInfo(refreshCode = 200, infoCode = 200)
        val response = HikkaInterceptor(hikka).intercept(chain)
        response.request.header("auth") shouldBe "stale"
        val saved = checkNotNull(hikka.loadOAuth())
        saved.accessToken shouldBe "stale"
        saved.expiration shouldBe 4_102_444_800L
        saved.created shouldBe 1_700_000_000L
        verify(exactly = 3) { chain.proceed(any()) }
    }

    @Test
    fun rejectedTokenLogsOut() {
        hikka.saveCredentials("user", "stale")
        hikka.saveOAuth(hikkaOAuth(expiration = now - 10, accessToken = "stale"))
        val chain = chainAnsweringProbeThenInfo(refreshCode = 401, infoCode = 200)
        shouldThrow<IOException> { HikkaInterceptor(hikka).intercept(chain) }.message shouldBe
            "Hikka: The token is expired"
        hikka.isLoggedIn shouldBe false
        hikka.loadOAuth().shouldBeNull()
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun failedTokenInfoThrows() {
        hikka.saveOAuth(hikkaOAuth(expiration = now - 10, accessToken = "stale"))
        val chain = chainAnsweringProbeThenInfo(refreshCode = 200, infoCode = 500)
        shouldThrow<IOException> { HikkaInterceptor(hikka).intercept(chain) }.message shouldBe
            "Hikka: Auth token info failed"
        verify(exactly = 2) { chain.proceed(any()) }
    }

    @Test
    fun setAuthNullForgetsTheToken() {
        hikka.saveOAuth(hikkaOAuth(expiration = now + 7200))
        val interceptor = HikkaInterceptor(hikka)
        interceptor.setAuth(null)
        hikka.loadOAuth().shouldBeNull()
        shouldThrow<IOException> { interceptor.intercept(chain { response(it, 200) }) }
    }

    private companion object {
        const val TOKEN_INFO = "eu/kanade/tachiyomi/data/track/hikka/token_info.json"
    }
}
