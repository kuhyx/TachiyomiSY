package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.mangabaka.dto.mangaBakaOAuth
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
import kotlin.time.Clock

internal class MangaBakaInterceptorTest {

    private val koin = TrackKoin()
    private lateinit var mangaBaka: MangaBaka
    private val request = Request.Builder().url("https://api.mangabaka.org/v1/my/profile").build()
    private val now = Clock.System.now().epochSeconds

    @BeforeEach
    fun setUp() {
        koin.start()
        mangaBaka = MangaBaka(11L)
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

    private fun freshTokenJson(accessToken: String): String =
        koin.json.encodeToString(mangaBakaOAuth(expiresAt = now + 7200, accessToken = accessToken))

    @Test
    fun noTokenThrows() {
        val chain = chain { response(it, 200) }
        val error = shouldThrow<IOException> { MangaBakaInterceptor(mangaBaka).intercept(chain) }
        error.message shouldBe "Not authenticated with MangaBaka"
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun freshTokenAddsBearer() {
        mangaBaka.saveToken(mangaBakaOAuth(expiresAt = now + 7200, accessToken = "fresh"))
        val chain = chain { response(it, 200) }
        val response = MangaBakaInterceptor(mangaBaka).intercept(chain)
        response.request.header("Authorization") shouldBe "Bearer fresh"
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun expiredTokenIsRefreshed() {
        mangaBaka.saveToken(mangaBakaOAuth(expiresAt = now - 10, accessToken = "stale"))
        val chain = chain {
            if (it.url.encodedPath.endsWith("/token")) {
                response(it, 200, freshTokenJson("renewed"))
            } else {
                response(it, 200)
            }
        }
        val response = MangaBakaInterceptor(mangaBaka).intercept(chain)
        response.request.header("Authorization") shouldBe "Bearer renewed"
        mangaBaka.restoreToken()?.accessToken shouldBe "renewed"
        verify(exactly = 2) { chain.proceed(any()) }
    }

    @Test
    fun failedRefreshKeepsTheOldToken() {
        mangaBaka.saveToken(mangaBakaOAuth(expiresAt = now - 10, accessToken = "stale"))
        val chain = chain {
            if (it.url.encodedPath.endsWith("/token")) response(it, 401, "{}") else response(it, 200)
        }
        val response = MangaBakaInterceptor(mangaBaka).intercept(chain)
        response.request.header("Authorization") shouldBe "Bearer stale"
        mangaBaka.restoreToken()?.accessToken shouldBe "stale"
    }

    @Test
    fun setAuthNullForgetsTheToken() {
        mangaBaka.saveToken(mangaBakaOAuth(expiresAt = now + 7200))
        val interceptor = MangaBakaInterceptor(mangaBaka)
        interceptor.setAuth(null)
        mangaBaka.restoreToken().shouldBeNull()
        shouldThrow<IOException> { interceptor.intercept(chain { response(it, 200) }) }
    }
}
