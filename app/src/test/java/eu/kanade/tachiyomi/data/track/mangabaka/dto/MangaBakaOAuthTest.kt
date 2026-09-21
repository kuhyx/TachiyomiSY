package eu.kanade.tachiyomi.data.track.mangabaka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.time.Clock

internal class MangaBakaOAuthTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesTheFixture() {
        val oauth = json.decodeFromString<MangaBakaOAuth>(
            fixture("eu/kanade/tachiyomi/data/track/mangabaka/oauth.json"),
        )
        oauth.accessToken shouldBe "access-1"
        oauth.refreshToken shouldBe "refresh-1"
        oauth.expiresIn shouldBe 3600L
        oauth.expiresAt shouldBe 4_102_444_800L
        oauth.tokenType shouldBe "Bearer"
        oauth.scope shouldBe "library.read library.write offline_access openid"
        json.decodeFromString<MangaBakaOAuth>(json.encodeToString(oauth)) shouldBe oauth
    }

    @Test
    fun expiryHasAOneMinuteMargin() {
        val now = Clock.System.now().epochSeconds
        mangaBakaOAuth(expiresAt = now + 3600).isExpired() shouldBe false
        mangaBakaOAuth(expiresAt = now + 30).isExpired() shouldBe true
        mangaBakaOAuth(expiresAt = now - 10).isExpired() shouldBe true
    }
}

internal fun mangaBakaOAuth(expiresAt: Long, accessToken: String = "access-1"): MangaBakaOAuth = MangaBakaOAuth(
    accessToken = accessToken,
    refreshToken = "refresh-1",
    expiresIn = 3600L,
    expiresAt = expiresAt,
    tokenType = "Bearer",
    scope = "openid",
)
