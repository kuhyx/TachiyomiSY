package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.response
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuOAuth
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

internal class KitsuInterceptorTest {

    private lateinit var kitsu: Kitsu
    private val original = Request.Builder().url("https://kitsu.app/api/edge/users").build()
    private val proceeded = mutableListOf<Request>()
    private var refreshCode = 200

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            if (request.url.encodedPath == "/api/oauth/token") {
                response(request, refreshCode, fixture("kitsu", "oauth.json"))
            } else {
                response(request, 200, "{}")
            }
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        kitsu = Kitsu(TrackerManager.KITSU)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun oauth(createdAt: Long, refreshToken: String? = "ref") = KitsuOAuth(
        accessToken = "old",
        tokenType = "bearer",
        createdAt = createdAt,
        expiresIn = 7200L,
        refreshToken = refreshToken,
    )

    private fun now() = System.currentTimeMillis() / 1000L

    @Test
    fun unauthenticatedFails() {
        val interceptor = KitsuInterceptor(kitsu)
        shouldThrow<IOException> { interceptor.intercept(chain) }.message shouldBe "Not authenticated with Kitsu"
    }

    @Test
    fun missingRefreshTokenFails() {
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.newAuth(oauth(createdAt = now(), refreshToken = null))
        shouldThrow<NullPointerException> { interceptor.intercept(chain) }
    }

    @Test
    fun validTokenAddsHeaders() {
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.newAuth(oauth(createdAt = now()))
        interceptor.intercept(chain).code shouldBe 200
        proceeded.size shouldBe 1
        val sent = proceeded.single()
        sent.header("Authorization") shouldBe "Bearer old"
        sent.header("Accept") shouldBe "application/vnd.api+json"
        sent.header("Content-Type") shouldBe "application/vnd.api+json"
        checkNotNull(sent.header("User-Agent")).startsWith("TachiyomiSY v") shouldBe true
    }

    @Test
    fun expiredTokenIsRefreshed() {
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.newAuth(oauth(createdAt = 0L))
        interceptor.intercept(chain)
        proceeded.size shouldBe 2
        val refresh = proceeded[0]
        refresh.method shouldBe "POST"
        Buffer().also { checkNotNull(refresh.body).writeTo(it) }.readUtf8().contains("refresh_token=ref") shouldBe true
        proceeded[1].header("Authorization") shouldBe "Bearer acc"
        checkNotNull(kitsu.restoreToken()).accessToken shouldBe "acc"
    }

    @Test
    fun failedRefreshKeepsOldToken() {
        refreshCode = 500
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.newAuth(oauth(createdAt = 0L))
        interceptor.intercept(chain)
        proceeded.size shouldBe 2
        proceeded[1].header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun restoresStoredToken() {
        kitsu.saveToken(oauth(createdAt = now()))
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun clearingAuthDropsStoredToken() {
        val interceptor = KitsuInterceptor(kitsu)
        interceptor.newAuth(oauth(createdAt = now()))
        interceptor.newAuth(null)
        kitsu.restoreToken().shouldBeNull()
        shouldThrow<IOException> { interceptor.intercept(chain) }
    }
}
