package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class MangaSerializationTest {

    private val filled = MangaFixtures.manga(id = 42L).copy(
        source = 7L,
        lastUpdate = 100L,
        nextUpdate = 200L,
        fetchInterval = -3,
        dateAdded = 300L,
        viewerFlags = 4L,
        chapterFlags = 5L,
        coverLastModified = 600L,
        url = "/manga/42",
        ogTitle = "Title",
        ogArtist = "Artist",
        ogAuthor = "Author",
        ogThumbnailUrl = "https://example.com/cover.png",
        ogDescription = "Description",
        ogGenre = listOf("Action", "Drama"),
        ogStatus = 2L,
        updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
        initialized = true,
        lastModifiedAt = 700L,
        favoriteModifiedAt = 800L,
        version = 9L,
        notes = "notes",
        memo = JsonObject(mapOf("key" to JsonPrimitive("value"))),
    )

    @Test
    fun jsonRoundTripKeepsEveryField() {
        val decoded = Json.decodeFromString<Manga>(Json.encodeToString(filled))

        decoded shouldBe filled
        decoded.memo.getValue("key").jsonPrimitive.content shouldBe "value"
    }

    @Test
    fun jsonRoundTripKeepsNulls() {
        val blank = Manga.create()

        val encoded = Json.encodeToString(blank)
        val decoded = Json.decodeFromString<Manga>(encoded)

        decoded shouldBe blank
        decoded.ogArtist shouldBe null
        decoded.ogAuthor shouldBe null
        decoded.ogThumbnailUrl shouldBe null
        decoded.ogDescription shouldBe null
        decoded.ogGenre shouldBe null
        decoded.favoriteModifiedAt shouldBe null
        encoded.contains("\"ogGenre\":null") shouldBe true
    }

    @Test
    fun jsonKeepsFavoriteFlag() {
        val favorite = filled.copy(id = MangaFixtures.PLAIN_FAVORITE_ID, favorite = true)

        val decoded = Json.decodeFromString<Manga>(Json.encodeToString(favorite))

        decoded shouldBe favorite
        decoded.favorite shouldBe true
        decoded.title shouldBe "Title"
    }

    @Test
    fun javaSerializationRoundTrips() {
        val restored = javaRoundTrip(filled)

        restored shouldBe filled
        restored.ogGenre shouldBe listOf("Action", "Drama")
        restored.updateStrategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
    }

    @Test
    fun javaSerializationKeepsNulls() {
        val restored = javaRoundTrip(Manga.create())

        restored shouldBe Manga.create()
        restored.ogGenre shouldBe null
    }

    @Test
    fun javaProxyDecodesJson() {
        val proxy = Manga.JavaToKotlinXSerializable(Json.encodeToString(filled))

        javaRoundTrip(proxy) shouldBe filled
    }

    private fun javaRoundTrip(value: Any): Manga {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        return restored.shouldBeInstanceOf<Manga>()
    }
}
