package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KitsuOAuthTest {

    private val json = TrackerHarness.json

    private fun oauth(createdAt: Long, expiresIn: Long = 7200L) = KitsuOAuth(
        accessToken = "acc",
        tokenType = "bearer",
        createdAt = createdAt,
        expiresIn = expiresIn,
        refreshToken = "ref",
    )

    @Test
    fun freshTokenIsNotExpired() {
        oauth(createdAt = System.currentTimeMillis() / 1000L).isExpired() shouldBe false
    }

    @Test
    fun tokenInsideMarginIsExpired() {
        oauth(createdAt = System.currentTimeMillis() / 1000L, expiresIn = 3599L).isExpired() shouldBe true
    }

    @Test
    fun decodesSerialNames() {
        val decoded = json.decodeFromString<KitsuOAuth>(
            """{"access_token":"a","token_type":"b","created_at":1,"expires_in":2,"refresh_token":null}""",
        )
        decoded shouldBe KitsuOAuth(
            accessToken = "a",
            tokenType = "b",
            createdAt = 1L,
            expiresIn = 2L,
            refreshToken = null,
        )
        exerciseDto(decoded)
    }

    @Test
    fun encodesSerialNames() {
        json.encodeToString(oauth(createdAt = 1L)) shouldBe
            """{"access_token":"acc","token_type":"bearer","created_at":1,"expires_in":7200,"refresh_token":"ref"}"""
    }
}
