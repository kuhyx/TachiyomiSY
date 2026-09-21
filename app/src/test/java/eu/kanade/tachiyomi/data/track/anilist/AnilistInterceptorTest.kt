package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class AnilistInterceptorTest {

    private lateinit var anilist: Anilist
    private val original = Request.Builder().url(AnilistApi.API_URL).build()
    private val proceeded = mutableListOf<Request>()

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            response(request, 200, "{}")
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        anilist = Anilist(TrackerManager.ANILIST)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    // `expires` is in seconds here; the interceptor converts it to millis and shaves a minute off.
    private fun oauth(expiresInSeconds: Long) = ALOAuth(
        accessToken = "acc",
        tokenType = "Bearer",
        expires = System.currentTimeMillis() / 1000L + expiresInSeconds,
        expiresIn = expiresInSeconds,
    )

    @Test
    fun missingTokenFails() {
        shouldThrow<IOException> { AnilistInterceptor(anilist, null).intercept(chain) }.message shouldBe
            "Not authenticated with Anilist"
        shouldThrow<IOException> { AnilistInterceptor(anilist, "").intercept(chain) }
    }

    @Test
    fun tokenWithoutStoredOAuthFails() {
        val error = shouldThrow<IOException> { AnilistInterceptor(anilist, "tok").intercept(chain) }
        error.message shouldBe "No authentication token"
    }

    @Test
    fun expiredTokenLogsOut() {
        anilist.saveCredentials("777", "tok")
        val interceptor = AnilistInterceptor(anilist, "tok")
        interceptor.setAuth(oauth(expiresInSeconds = 30L))
        shouldThrow<IOException> { interceptor.intercept(chain) }.message shouldBe "Token expired"
        anilist.isLoggedIn shouldBe false
    }

    @Test
    fun validTokenAddsHeaders() {
        val interceptor = AnilistInterceptor(anilist, "tok")
        interceptor.setAuth(oauth(expiresInSeconds = 3600L))
        interceptor.intercept(chain).code shouldBe 200
        val sent = proceeded.single()
        sent.header("Authorization") shouldBe "Bearer acc"
        checkNotNull(sent.header("User-Agent")).startsWith("TachiSY v") shouldBe true
    }

    @Test
    fun storedOAuthIsLoadedLazily() {
        val stored = oauth(expiresInSeconds = 3600L)
        anilist.saveOAuth(stored)
        AnilistInterceptor(anilist, "tok").intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer acc"
        anilist.loadOAuth() shouldBe stored
    }

    @Test
    fun clearingAuthForgetsToken() {
        val interceptor = AnilistInterceptor(anilist, "tok")
        interceptor.setAuth(oauth(expiresInSeconds = 3600L))
        interceptor.setAuth(null)
        anilist.loadOAuth() shouldBe null
        shouldThrow<IOException> { interceptor.intercept(chain) }.message shouldBe "Not authenticated with Anilist"
    }
}
