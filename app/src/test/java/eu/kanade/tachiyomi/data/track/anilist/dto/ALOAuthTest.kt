package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ALOAuthTest {

    @Test
    fun expiryComparesToNow() {
        val now = System.currentTimeMillis()
        ALOAuth("a", "Bearer", now + 60_000L, 60_000L).isExpired() shouldBe false
        ALOAuth("a", "Bearer", now - 1L, 60_000L).isExpired() shouldBe true
    }

    @Test
    fun serialNamesRoundTrip() {
        val json = TrackerHarness.json
        val oauth = ALOAuth(accessToken = "a", tokenType = "Bearer", expires = 5L, expiresIn = 4L)
        val encoded = json.encodeToString(oauth)
        encoded shouldBe """{"access_token":"a","token_type":"Bearer","expires":5,"expires_in":4}"""
        val decoded = json.decodeFromString<ALOAuth>(encoded)
        decoded shouldBe oauth
        exerciseDto(decoded)
    }
}
