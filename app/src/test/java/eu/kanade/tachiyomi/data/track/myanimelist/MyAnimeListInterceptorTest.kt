package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.response
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
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

internal class MyAnimeListInterceptorTest {

    private lateinit var mal: MyAnimeList
    private val original = Request.Builder().url("https://api.myanimelist.net/v2/users/@me").build()
    private val proceeded = mutableListOf<Request>()
    private var refreshCode = 200
    private var refreshBody = fixture("myanimelist", "oauth.json")
    private var refreshFails = false

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            if (request.url.encodedPath == "/v1/oauth2/token") {
                check(!refreshFails) { "refresh endpoint down" }
                response(request, refreshCode, refreshBody)
            } else {
                response(request, 200, "{}")
            }
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        mal = MyAnimeList(TrackerManager.MYANIMELIST)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun oauth(expiresIn: Long) = MALOAuth(
        tokenType = "Bearer",
        refreshToken = "ref",
        accessToken = "old",
        expiresIn = expiresIn,
        createdAt = System.currentTimeMillis() / 1000L,
    )

    private fun expiredInterceptor() = MyAnimeListInterceptor(mal).apply { setAuth(oauth(expiresIn = 0L)) }

    @Test
    fun expiredLoginFailsFast() {
        mal.setAuthExpired()
        shouldThrow<MALTokenExpired> { MyAnimeListInterceptor(mal).intercept(chain) }
    }

    @Test
    fun unauthenticatedFails() {
        shouldThrow<IOException> { MyAnimeListInterceptor(mal).intercept(chain) }.message shouldBe
            "MAL: User is not authenticated"
    }

    @Test
    fun validTokenAddsHeader() {
        val interceptor = MyAnimeListInterceptor(mal)
        interceptor.setAuth(oauth(expiresIn = 3600L))
        interceptor.intercept(chain).code shouldBe 200
        proceeded.single().header("Authorization") shouldBe "Bearer old"
        checkNotNull(mal.loadOAuth()).accessToken shouldBe "old"
    }

    @Test
    fun expiredTokenIsRefreshed() {
        expiredInterceptor().intercept(chain)
        proceeded.size shouldBe 2
        proceeded[0].header("Authorization") shouldBe "Bearer old"
        proceeded[1].header("Authorization") shouldBe "Bearer acc"
        checkNotNull(mal.loadOAuth()).accessToken shouldBe "acc"
    }

    @Test
    fun unauthorizedRefreshExpires() {
        refreshCode = 401
        shouldThrow<MALTokenExpired> { expiredInterceptor().intercept(chain) }
        mal.getIfAuthExpired() shouldBe true
    }

    @Test
    fun failedRefreshIsReported() {
        refreshCode = 500
        shouldThrow<MALTokenRefreshFailed> { expiredInterceptor().intercept(chain) }
        refreshCode = 200
        refreshBody = "not json"
        shouldThrow<MALTokenRefreshFailed> { expiredInterceptor().intercept(chain) }
        refreshFails = true
        shouldThrow<MALTokenRefreshFailed> { expiredInterceptor().intercept(chain) }
    }

    @Test
    fun loginExpiringMidRefreshFails() {
        var reads = 0
        TrackerHarness.store.scriptBoolean(mal.authExpiredKey()) { reads++ > 0 }
        shouldThrow<MALTokenExpired> { expiredInterceptor().intercept(chain) }
        proceeded.size shouldBe 0
    }

    @Test
    fun tokenRenewedMidRefreshIsReused() {
        val interceptor = expiredInterceptor()
        var reads = 0
        // The second read happens inside the refresh lock: another caller re-authenticated meanwhile.
        TrackerHarness.store.scriptBoolean(mal.authExpiredKey()) {
            if (reads++ == 1) interceptor.setAuth(oauth(expiresIn = 3600L).copy(accessToken = "fresh"))
            false
        }
        interceptor.intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer fresh"
    }

    @Test
    fun storedTokenIsRestored() {
        mal.saveOAuth(oauth(expiresIn = 3600L))
        MyAnimeListInterceptor(mal).intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer old"
        MALTitleNotApproved().message shouldBe "MAL: This title can't be added because it is waiting for approval."
    }
}

internal fun MyAnimeList.authExpiredKey(): String = "__PRIVATE_pref_tracker_auth_expired_$id"
