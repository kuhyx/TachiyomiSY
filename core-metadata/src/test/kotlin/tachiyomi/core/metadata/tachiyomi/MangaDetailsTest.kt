package tachiyomi.core.metadata.tachiyomi

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class MangaDetailsTest {
    @Test
    fun roundTripsThroughJson() {
        val details = MangaDetails(
            title = "T", author = "A", artist = "R", description = "D", genre = listOf("G1", "G2"), status = 2,
        )
        val encoded = Json.encodeToString(MangaDetails.serializer(), details)
        Json.decodeFromString(MangaDetails.serializer(), encoded) shouldBe details
    }

    @Test
    fun differsOnEveryField() {
        val base = MangaDetails()
        base shouldBe base.copy()
        base.hashCode() shouldBe base.copy().hashCode()
        base.toString() shouldBe base.copy().toString()
        base.equals(Any()) shouldBe false
        MangaDetails(status = 3).status shouldBe 3
        MangaDetails("t", "a").author shouldBe "a"
        val variants = listOf(
            base.copy(title = "x"), base.copy(author = "x"), base.copy(artist = "x"),
            base.copy(description = "x"), base.copy(genre = listOf("x")), base.copy(status = 1),
        )
        variants.forEach { it shouldNotBe base }
        variants.toSet().size shouldBe variants.size
    }

    @Test
    fun decodesEachFieldAlone() {
        val json = mapOf(
            "title" to "\"x\"", "author" to "\"x\"", "artist" to "\"x\"", "description" to "\"x\"",
            "genre" to "[\"x\"]", "status" to "1",
        )
        json.forEach { (field, value) ->
            val single = Json.decodeFromString(MangaDetails.serializer(), "{\"$field\":$value}")
            single shouldNotBe MangaDetails()
            Json.encodeToString(MangaDetails.serializer(), single) shouldBe "{\"$field\":$value}"
        }
        Json.encodeToString(MangaDetails.serializer(), MangaDetails()) shouldBe "{}"
    }

    @Test
    fun everyFieldDefaultsToNull() {
        val details = Json.decodeFromString(MangaDetails.serializer(), "{}")
        details shouldBe MangaDetails()
        details.title shouldBe null
        details.genre shouldBe null
    }
}
