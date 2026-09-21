package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.response
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ShikimoriInterceptorTest {

    private lateinit var shikimori: Shikimori
    private val original = Request.Builder().url(ShikimoriApi.GRAPHQL_API_URL).build()
    private val proceeded = mutableListOf<Request>()
    private var refreshCode = 200

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            if (request.url.encodedPath == "/oauth/token") {
                response(request, refreshCode, fixture("shikimori", "oauth.json"))
            } else {
                response(request, 200, "{}")
            }
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        shikimori = Shikimori(TrackerManager.SHIKIMORI)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun oauth(createdAt: Long, refreshToken: String? = "ref") = SMOAuth(
        accessToken = "old",
        tokenType = "Bearer",
        createdAt = createdAt,
        expiresIn = 86_400L,
        refreshToken = refreshToken,
    )

    private fun now() = System.currentTimeMillis() / 1000L

    @Test
    fun unauthenticatedFails() {
        shouldThrow<IOException> { ShikimoriInterceptor(shikimori).intercept(chain) }.message shouldBe
            "Not authenticated with Shikimori"
    }

    @Test
    fun missingRefreshTokenFails() {
        val interceptor = ShikimoriInterceptor(shikimori)
        interceptor.newAuth(oauth(createdAt = now(), refreshToken = null))
        shouldThrow<NullPointerException> { interceptor.intercept(chain) }
    }

    @Test
    fun validTokenAddsHeaders() {
        val interceptor = ShikimoriInterceptor(shikimori)
        interceptor.newAuth(oauth(createdAt = now()))
        interceptor.intercept(chain).code shouldBe 200
        val sent = proceeded.single()
        sent.header("Authorization") shouldBe "Bearer old"
        checkNotNull(sent.header("User-Agent")).startsWith("TachiyomiSY v") shouldBe true
    }

    @Test
    fun expiredTokenIsRefreshed() {
        val interceptor = ShikimoriInterceptor(shikimori)
        interceptor.newAuth(oauth(createdAt = 0L))
        interceptor.intercept(chain)
        proceeded.size shouldBe 2
        val refresh = proceeded[0]
        refresh.url.toString() shouldBe "https://shikimori.io/oauth/token"
        Buffer().also { checkNotNull(refresh.body).writeTo(it) }.readUtf8().contains("refresh_token=ref") shouldBe true
        proceeded[1].header("Authorization") shouldBe "Bearer acc"
        checkNotNull(shikimori.restoreToken()).accessToken shouldBe "acc"
    }

    @Test
    fun failedRefreshKeepsOldToken() {
        refreshCode = 500
        val interceptor = ShikimoriInterceptor(shikimori)
        interceptor.newAuth(oauth(createdAt = 0L))
        interceptor.intercept(chain)
        proceeded[1].header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun restoresStoredToken() {
        shikimori.saveToken(oauth(createdAt = now()))
        ShikimoriInterceptor(shikimori).intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun clearingAuthDropsStoredToken() {
        val interceptor = ShikimoriInterceptor(shikimori)
        interceptor.newAuth(oauth(createdAt = now()))
        interceptor.newAuth(null)
        shikimori.restoreToken().shouldBeNull()
        shouldThrow<IOException> { interceptor.intercept(chain) }
    }
}
