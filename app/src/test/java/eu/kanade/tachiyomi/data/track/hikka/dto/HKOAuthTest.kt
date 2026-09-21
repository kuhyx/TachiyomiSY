package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class HKOAuthTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesTheFixture() {
        val oauth = json.decodeFromString<HKOAuth>(fixture("eu/kanade/tachiyomi/data/track/hikka/oauth.json"))
        oauth.accessToken shouldBe "hikka-secret"
        oauth.expiration shouldBe 4_102_444_800L
        oauth.created shouldBe 1_700_000_000L
        json.encodeToString(oauth) shouldBe """{"secret":"hikka-secret","expiration":4102444800,"created":1700000000}"""
    }

    @Test
    fun expiresFiveMinutesEarly() {
        val now = System.currentTimeMillis() / 1000
        hikkaOAuth(expiration = now + 3600).isExpired() shouldBe false
        hikkaOAuth(expiration = now + 200).isExpired() shouldBe true
        hikkaOAuth(expiration = now - 1).isExpired() shouldBe true
    }
}

internal fun hikkaOAuth(expiration: Long, accessToken: String = "hikka-secret"): HKOAuth =
    HKOAuth(accessToken = accessToken, expiration = expiration, created = 1_700_000_000L)
