package exh.md.network

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import exh.md.utils.MdUtil
import exh.md.utils.loadOAuth
import exh.md.utils.oauth
import exh.md.utils.saveOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.jupiter.api.Test

private const val AUTH = "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect"

internal class MangaDexLoginHelperTest {
    private val server = CannedServer()
    private val preferences = TrackPreferences(MemoPreferenceStore())
    private val mdList: MdList = mockk { every { id } returns 60L }
    private val interceptor = MangaDexAuthInterceptor(preferences, mdList)
    private val helper = MangaDexLoginHelper(server.client, preferences, mdList, interceptor)

    private fun body(index: Int = 0): String = Buffer().also { server.request(index).body?.writeTo(it) }.readUtf8()

    @Test
    fun loginStoresTokens() {
        MdUtil.codeVerifier = "verifier"
        server.body = """{"token_type":"Bearer","refresh_token":"r","access_token":"a","expires_in":3600}"""
        runBlocking { helper.login("code123") } shouldBe true
        server.request().url.toString() shouldBe "$AUTH/token"
        body() shouldBe "client_id=tachiyomisy&grant_type=authorization_code&code=code123&code_verifier=verifier" +
            "&redirect_uri=tachiyomisy%3A%2F%2Fmangadex-auth"
        interceptor.token shouldBe "a"
        MdUtil.loadOAuth(preferences, mdList)?.refreshToken shouldBe "r"
    }

    @Test
    fun loginFailureLogsOut() {
        justRun { mdList.logout() }
        server.body = "not json"
        runBlocking { helper.login("code") } shouldBe false
        verify { mdList.logout() }
        interceptor.token.shouldBeNull()
    }

    @Test
    fun logoutWithoutTokensIsLocal() {
        justRun { mdList.logout() }
        runBlocking { helper.logout() } shouldBe true
        verify(exactly = 1) { mdList.logout() }
        server.requests.isEmpty() shouldBe true
        MdUtil.saveOAuth(preferences, mdList, oauth(access = "", refresh = "r"))
        runBlocking { helper.logout() } shouldBe true
        MdUtil.saveOAuth(preferences, mdList, oauth(access = "a", refresh = ""))
        runBlocking { helper.logout() } shouldBe true
        verify(exactly = 3) { mdList.logout() }
    }

    @Test
    fun logoutRevokesRemotely() {
        justRun { mdList.logout() }
        interceptor.setAuth(oauth(access = "a", refresh = "r"))
        server.body = ""
        runBlocking { helper.logout() } shouldBe true
        server.request().url.toString() shouldBe "$AUTH/logout"
        server.request().header("Authorization") shouldBe "Bearer a"
        body() shouldBe "client_id=tachiyomisy&refresh_token=r&redirect_uri=tachiyomisy%3A%2F%2Fmangadex-auth"
        interceptor.token.shouldBeNull()
        MdUtil.loadOAuth(preferences, mdList).shouldBeNull()
    }

    @Test
    fun logoutRemoteFailure() {
        interceptor.setAuth(oauth(access = "a", refresh = "r"))
        server.code = 500
        runBlocking { helper.logout() } shouldBe false
        interceptor.token shouldBe "a"
    }
}
