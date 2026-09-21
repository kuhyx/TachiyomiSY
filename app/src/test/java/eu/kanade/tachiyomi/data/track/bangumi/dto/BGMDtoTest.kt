package eu.kanade.tachiyomi.data.track.bangumi.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test

internal class BGMDtoTest {

    private val json = TrackerHarness.json

    private inline fun <reified T> roundTrip(name: String): T {
        val decoded = json.decodeFromString<T>(fixture("bangumi", name))
        exerciseDto(decoded)
        return decoded
    }

    @Test
    fun collectionStatusMirrorsType() {
        val full = roundTrip<BGMCollectionResponse>("collection.json")
        full shouldBe BGMCollectionResponse(
            rate = 8,
            type = 3,
            epStatus = 12,
            volStatus = 2,
            private = true,
            subject = BGMSlimSubject(31, 276),
        )
        full.getStatus() shouldBe Bangumi.READING
        val bare = roundTrip<BGMCollectionResponse>("collection_bare.json")
        bare.getStatus() shouldBe Bangumi.PLAN_TO_READ
        BGMCollectionResponse(rate = null, type = null).epStatus shouldBe 0
    }

    @Test
    fun unknownCollectionTypeFails() {
        shouldThrow<IllegalArgumentException> { BGMCollectionResponse(rate = null, type = null).getStatus() }
        shouldThrow<IllegalArgumentException> { BGMCollectionResponse(rate = null, type = 9).getStatus() }
        shouldThrow<IllegalArgumentException> { BGMCollectionResponse(rate = null, type = 0).getStatus() }
    }

    @Test
    fun oauthDefaultsCreatedAt() {
        val decoded = roundTrip<BGMOAuth>("oauth.json")
        decoded.accessToken shouldBe "acc"
        decoded.userId shouldBe 42L
        decoded.isExpired() shouldBe false
        json.encodeToString(decoded) shouldContain """"created_at":"""
        BGMOAuth("a", "Bearer", 0L, 604_800L, "r", null).isExpired() shouldBe true
        BGMOAuth("a", "Bearer", expiresIn = 3599L, refreshToken = null, userId = null).isExpired() shouldBe true
    }

    @Test
    fun userDecodes() {
        roundTrip<BGMUser>("user.json") shouldBe BGMUser("kuhy42", "kuhy")
    }

    @Test
    fun infoboxPicksShapeByValue() {
        val subject = roundTrip<BGMSubject>("subject.json")
        subject.infobox shouldBe listOf(
            Infobox.SingleValue("作者", "井上雄彦"),
            Infobox.MultipleValues(
                "作者",
                listOf(InfoboxNestedValue(null, "Alias"), InfoboxNestedValue("en", "Takehiko Inoue")),
            ),
            Infobox.SingleValue("插图", "Painter"),
            Infobox.MultipleValues("别名", listOf(InfoboxNestedValue(value = "SD"))),
        )
        subject.infobox.map { it.key } shouldBe listOf("作者", "作者", "插图", "别名")
    }

    @Test
    fun infoboxRejectsOtherShapes() {
        shouldThrow<SerializationException> { json.decodeFromString<Infobox>("""{"key":"k","value":{"x":1}}""") }
        shouldThrow<SerializationException> { json.decodeFromString<Infobox>(""""just a string"""") }
        shouldThrow<SerializationException> { json.decodeFromString<Infobox>("""{"key":"k"}""") }
    }
}
