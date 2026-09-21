package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.response
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
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

internal class BangumiInterceptorTest {

    private lateinit var bangumi: Bangumi
    private val original = Request.Builder().url("https://api.bgm.tv/v0/me").build()
    private val proceeded = mutableListOf<Request>()
    private var refreshCode = 200

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            if (request.url.encodedPath == "/oauth/access_token") {
                response(request, refreshCode, fixture("bangumi", "oauth.json"))
            } else {
                response(request, 200, "{}")
            }
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        bangumi = Bangumi(TrackerManager.BANGUMI)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun oauth(expiresIn: Long, userId: Long? = 42L) = BGMOAuth(
        accessToken = "old",
        tokenType = "Bearer",
        expiresIn = expiresIn,
        refreshToken = "ref",
        userId = userId,
    )

    @Test
    fun unauthenticatedFails() {
        shouldThrow<IOException> { BangumiInterceptor(bangumi).intercept(chain) }.message shouldBe
            "Not authenticated with Bangumi"
    }

    @Test
    fun validTokenAddsHeaders() {
        val interceptor = BangumiInterceptor(bangumi)
        interceptor.newAuth(oauth(expiresIn = 86_400L))
        interceptor.intercept(chain).code shouldBe 200
        val sent = proceeded.single()
        sent.header("Authorization") shouldBe "Bearer old"
        checkNotNull(sent.header("User-Agent")).startsWith("jobobby04/TachiyomiSY/v") shouldBe true
    }

    @Test
    fun expiredTokenIsRefreshed() {
        val interceptor = BangumiInterceptor(bangumi)
        interceptor.newAuth(oauth(expiresIn = 0L))
        interceptor.intercept(chain)
        proceeded.size shouldBe 2
        val refresh = proceeded[0]
        refresh.url.toString() shouldBe "https://bgm.tv/oauth/access_token"
        val form = Buffer().also { checkNotNull(refresh.body).writeTo(it) }.readUtf8()
        form.contains("refresh_token=ref") shouldBe true
        form.contains("redirect_uri=mihon%3A%2F%2Fbangumi-auth") shouldBe true
        proceeded[1].header("Authorization") shouldBe "Bearer acc"
        checkNotNull(bangumi.restoreToken()).accessToken shouldBe "acc"
    }

    @Test
    fun failedRefreshKeepsOldToken() {
        refreshCode = 500
        val interceptor = BangumiInterceptor(bangumi)
        interceptor.newAuth(oauth(expiresIn = 0L))
        interceptor.intercept(chain)
        proceeded[1].header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun newAuthKeepsPreviousUserId() {
        val interceptor = BangumiInterceptor(bangumi)
        interceptor.newAuth(oauth(expiresIn = 86_400L, userId = 7L))
        interceptor.newAuth(oauth(expiresIn = 0L, userId = null))
        interceptor.intercept(chain)
        proceeded[1].header("Authorization") shouldBe "Bearer acc"
        checkNotNull(bangumi.restoreToken()).userId shouldBe 42L
    }

    @Test
    fun restoresStoredToken() {
        bangumi.saveToken(oauth(expiresIn = 86_400L))
        BangumiInterceptor(bangumi).intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer old"
    }

    @Test
    fun clearingAuthDropsStoredToken() {
        val interceptor = BangumiInterceptor(bangumi)
        interceptor.newAuth(oauth(expiresIn = 86_400L))
        interceptor.newAuth(null)
        bangumi.restoreToken().shouldBeNull()
        shouldThrow<IOException> { interceptor.intercept(chain) }
    }
}
