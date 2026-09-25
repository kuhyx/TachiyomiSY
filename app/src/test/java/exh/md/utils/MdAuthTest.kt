package exh.md.utils

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import org.junit.jupiter.api.Test

internal fun oauth(access: String = "access", refresh: String = "refresh", expiresIn: Long = 3600): MALOAuth =
    MALOAuth(tokenType = "Bearer", refreshToken = refresh, accessToken = access, expiresIn = expiresIn, createdAt = 10)

internal class MdAuthTest {
    private val preferences = TrackPreferences(MemoPreferenceStore())
    private val mdList: MdList = mockk { every { id } returns 60L }

    @Test
    fun saveAndLoadOAuth() {
        MdUtil.loadOAuth(preferences, mdList).shouldBeNull()
        MdUtil.saveOAuth(preferences, mdList, oauth())
        preferences.trackToken(mdList).get().isNotBlank() shouldBe true
        MdUtil.loadOAuth(preferences, mdList) shouldBe oauth()
        MdUtil.saveOAuth(preferences, mdList, null)
        preferences.trackToken(mdList).get() shouldBe ""
        MdUtil.loadOAuth(preferences, mdList).shouldBeNull()
    }

    @Test
    fun refreshTokenRequest() {
        MdUtil.codeVerifier = "fixed"
        val request = MdUtil.refreshTokenRequest(oauth(access = "tok", refresh = "ref"))
        request.url.toString() shouldBe "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token"
        request.header("Authorization") shouldBe "Bearer tok"
        val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        body shouldBe "client_id=tachiyomisy&grant_type=refresh_token&refresh_token=ref" +
            "&code_verifier=fixed&redirect_uri=tachiyomisy%3A%2F%2Fmangadex-auth"
    }

    @Test
    fun pkceCodeIsGeneratedOnce() {
        MdUtil.codeVerifier = null
        val first = MdUtil.getPkceChallengeCode()
        first.isNotBlank() shouldBe true
        MdUtil.getPkceChallengeCode() shouldBe first
        MdUtil.codeVerifier = "other"
        MdUtil.getPkceChallengeCode() shouldBe "other"
        first shouldNotBe "other"
    }

    @Test
    fun encodeToBody() {
        val body = MdUtil.encodeToBody(mapOf("a" to 1))
        body.contentType().toString() shouldBe "application/json; charset=utf-8"
        Buffer().also { body.writeTo(it) }.readUtf8().replace(Regex("\\s"), "") shouldBe """{"a":1}"""
    }
}
