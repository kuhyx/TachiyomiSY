package eu.kanade.tachiyomi.data.track.myanimelist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class MALOAuthTest {

    private val json = TrackerHarness.json

    @Test
    fun createdAtDefaultsToNow() {
        val before = System.currentTimeMillis() / 1000L
        val decoded = json.decodeFromString<MALOAuth>(fixture("myanimelist", "oauth.json"))
        (decoded.createdAt >= before) shouldBe true
        decoded.accessToken shouldBe "acc"
        decoded.refreshToken shouldBe "ref"
        decoded.tokenType shouldBe "Bearer"
        decoded.expiresIn shouldBe 2_678_400L
        decoded.isExpired() shouldBe false
        exerciseDto(decoded)
    }

    @Test
    fun createdAtIsAlwaysEncoded() {
        val oauth = MALOAuth(
            tokenType = "Bearer",
            refreshToken = "r",
            accessToken = "a",
            expiresIn = 10L,
            createdAt = 7L,
        )
        val encoded = json.encodeToString(oauth)
        encoded shouldContain """"created_at":7"""
        json.decodeFromString<MALOAuth>(encoded) shouldBe oauth
    }

    @Test
    fun expiryHasOneMinuteMargin() {
        val now = System.currentTimeMillis() / 1000L
        MALOAuth("Bearer", "r", "a", expiresIn = 120L).isExpired() shouldBe false
        MALOAuth("Bearer", "r", "a", expiresIn = 59L, createdAt = now).isExpired() shouldBe true
    }
}
