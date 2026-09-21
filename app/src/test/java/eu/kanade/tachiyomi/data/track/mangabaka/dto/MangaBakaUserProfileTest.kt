package eu.kanade.tachiyomi.data.track.mangabaka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class MangaBakaUserProfileTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun decodesTheFixture() {
        val response = json.decodeFromString<MangaBakaUserProfileResponse>(
            fixture("eu/kanade/tachiyomi/data/track/mangabaka/profile.json"),
        )
        response.data shouldBe MangaBakaUserProfile(
            id = "user-id-1",
            ratingSteps = 5,
            nickname = "Nick",
            preferredUsername = "preferred",
        )
        response.data.id shouldBe "user-id-1"
        response.data.ratingSteps shouldBe 5
        response.data.nickname shouldBe "Nick"
        response.data.preferredUsername shouldBe "preferred"
    }

    @Test
    fun decodesWithoutOptionalNames() {
        val profile = json.decodeFromString<MangaBakaUserProfile>("""{"id":"x","rating_steps":1}""")
        profile.nickname shouldBe null
        profile.preferredUsername shouldBe null
        json.decodeFromString<MangaBakaUserProfileResponse>(
            json.encodeToString(MangaBakaUserProfileResponse(profile)),
        ).data shouldBe profile
    }
}
