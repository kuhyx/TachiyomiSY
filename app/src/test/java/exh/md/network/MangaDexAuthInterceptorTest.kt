package exh.md.network

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import eu.kanade.tachiyomi.source.online.cannedResponse
import exh.md.utils.MdUtil
import exh.md.utils.loadOAuth
import exh.md.utils.oauth
import exh.md.utils.saveOAuth
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test
import java.io.IOException

internal class MangaDexAuthInterceptorTest {
    private val preferences = TrackPreferences(MemoPreferenceStore())
    private val mdList: MdList = mockk { every { id } returns 60L }
    private val request = Request.Builder().url("https://api.mangadex.org/manga").build()
    private val proceeded = mutableListOf<Request>()

    private fun chain(vararg responses: Response): Interceptor.Chain {
        val queue = ArrayDeque(responses.toList())
        return mockk {
            every { request() } returns request
            every { proceed(any()) } answers {
                proceeded += firstArg<Request>()
                queue.removeFirst()
            }
        }
    }

    private fun oauthJson(access: String): String =
        """{"token_type":"Bearer","refresh_token":"r","access_token":"$access","expires_in":3600,""" +
            """"created_at":${System.currentTimeMillis() / 1000}}"""

    private fun liveOAuth(access: String): MALOAuth =
        oauth(access = access).copy(createdAt = System.currentTimeMillis() / 1000)

    private fun unauthorized(header: String?): Response = cannedResponse("", code = 401).newBuilder()
        .apply { if (header != null) header("www-authenticate", header) }
        .build()

    @Test
    fun withoutTokenPassesThrough() {
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        interceptor.token.shouldBeNull()
        val response = cannedResponse("ok")
        interceptor.intercept(chain(response)) shouldBe response
        proceeded.single().header("Authorization").shouldBeNull()
        interceptor.setAuth(oauth(access = ""))
        interceptor.token shouldBe ""
        interceptor.intercept(chain(response)) shouldBe response
        proceeded.size shouldBe 2
    }

    @Test
    fun unreadableStoredTokenFails() {
        preferences.trackToken(mdList).set("garbage")
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        interceptor.token shouldBe "garbage"
        shouldThrow<IOException> { interceptor.intercept(chain()) }.message shouldBe "No authentication token"
    }

    @Test
    fun withValidTokenAddsBearer() {
        MdUtil.saveOAuth(preferences, mdList, liveOAuth("live"))
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        interceptor.token?.isNotBlank() shouldBe true
        val response = cannedResponse("ok")
        interceptor.intercept(chain(response)) shouldBe response
        proceeded.single().header("Authorization") shouldBe "Bearer live"
        interceptor.intercept(chain(response)) shouldBe response
        proceeded.size shouldBe 2
    }

    @Test
    fun expiredTokenIsRefreshedFirst() {
        MdUtil.saveOAuth(preferences, mdList, oauth(access = "stale"))
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        val response = cannedResponse("ok")
        interceptor.intercept(chain(cannedResponse(oauthJson("fresh")), response)) shouldBe response
        proceeded[0].url.toString() shouldBe "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token"
        proceeded[1].header("Authorization") shouldBe "Bearer fresh"
        MdUtil.loadOAuth(preferences, mdList)?.accessToken shouldBe "fresh"
    }

    @Test
    fun failedRefreshLeavesNoAuth() {
        MdUtil.saveOAuth(preferences, mdList, oauth(access = "stale"))
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        shouldThrow<IOException> { interceptor.intercept(chain(cannedResponse("", code = 500))) }.message shouldBe
            "No authentication token"
        interceptor.token.shouldBeNull()
        MdUtil.loadOAuth(preferences, mdList).shouldBeNull()
    }

    @Test
    fun unauthorizedRetriesFresh() {
        MdUtil.saveOAuth(preferences, mdList, liveOAuth("live"))
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        val expired = unauthorized("""Bearer error="invalid_token", error_description="The access token expired"""")
        val ok = cannedResponse("ok")
        interceptor.intercept(chain(expired, cannedResponse(oauthJson("fresh")), ok)) shouldBe ok
        proceeded[2].header("Authorization") shouldBe "Bearer fresh"
    }

    @Test
    fun unauthorizedKeepsResponse() {
        MdUtil.saveOAuth(preferences, mdList, liveOAuth("live"))
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        val expired = unauthorized("The access token expired")
        interceptor.intercept(chain(expired, cannedResponse("not json"))) shouldBe expired
        val other401 = unauthorized(null)
        interceptor.intercept(chain(other401)) shouldBe other401
        val other401Header = unauthorized("nope")
        interceptor.intercept(chain(other401Header)) shouldBe other401Header
        val okExpiredHeader = cannedResponse("ok").newBuilder()
            .header("www-authenticate", "The access token expired")
            .build()
        interceptor.intercept(chain(okExpiredHeader)) shouldBe okExpiredHeader
    }

    @Test
    fun setAuthAndBearer() {
        val interceptor = MangaDexAuthInterceptor(preferences, mdList)
        interceptor.setAuth(oauth(access = "set"))
        interceptor.token shouldBe "set"
        MdUtil.loadOAuth(preferences, mdList)?.accessToken shouldBe "set"
        interceptor.setAuth(null)
        interceptor.token.shouldBeNull()
        request.withBearer("x").header("Authorization") shouldBe "Bearer x"
        MALOAuth("Bearer", "r", "a", 1).createdAt shouldBe MALOAuth("Bearer", "r", "a", 1).createdAt
    }
}
