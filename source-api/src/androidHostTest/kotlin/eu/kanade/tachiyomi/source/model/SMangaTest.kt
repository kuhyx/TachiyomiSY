package eu.kanade.tachiyomi.source.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

internal class SMangaTest {
    @Test
    fun genresNullWhenGenreNull() {
        SManga.create().getGenres() shouldBe null
    }

    @Test
    fun genresNullWhenBlank() {
        SManga("/u", "t", genre = "   ").getGenres() shouldBe null
        SManga("/u", "t", genre = "").getGenres() shouldBe null
    }

    @Test
    fun genresSplitTrimmedDeduped() {
        val manga = SManga("/u", "t", genre = "Action,  Drama, Action, , Comedy")
        manga.getGenres() shouldContainExactly listOf("Action", "Drama", "Comedy")
    }

    @Test
    fun genresNullWhenGetterTurnsNull() {
        val manga = VanishingGenreManga()
        manga.getGenres() shouldBe null
        manga.genreReadCount() shouldBe 2
        manga.genre shouldBe null
    }

    @Test
    fun createIsEmptyImpl() {
        val manga = SManga.create()
        manga.title shouldBe ""
        manga.thumbnail_url shouldBe null
        manga.artist shouldBe null
        manga.author shouldBe null
        manga.status shouldBe SManga.UNKNOWN
        manga.description shouldBe null
        manga.genre shouldBe null
        manga.update_strategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        manga.initialized shouldBe false
        manga.memo shouldBe JsonObject(emptyMap())
        shouldThrow<UninitializedPropertyAccessException> { manga.url }
    }

    @Test
    fun invokeSetsGivenFields() {
        val manga = populatedManga()
        manga.url shouldBe "/manga/1"
        manga.title shouldBe "Title"
        manga.artist shouldBe "Artist"
        manga.author shouldBe "Author"
        manga.description shouldBe "Description"
        manga.genre shouldBe "Action, Drama"
        manga.status shouldBe SManga.COMPLETED
        manga.thumbnail_url shouldBe "https://example.invalid/cover.png"
        manga.initialized shouldBe true
    }

    @Test
    fun invokeDefaultsOptionalFields() {
        val manga = SManga("/u", "t")
        manga.url shouldBe "/u"
        manga.title shouldBe "t"
        manga.artist shouldBe null
        manga.author shouldBe null
        manga.description shouldBe null
        manga.genre shouldBe null
        manga.status shouldBe SManga.UNKNOWN
        manga.thumbnail_url shouldBe null
        manga.initialized shouldBe false
    }

    @Test
    fun memberCopyKeepsSourceFields() {
        val source = populatedManga()
        source.update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        source.memo = buildJsonObject { put("k", 1) }
        val copy = source.copy()
        copy shouldNotBeSameInstanceAs source
        copy.url shouldBe source.url
        copy.title shouldBe source.title
        copy.artist shouldBe source.artist
        copy.author shouldBe source.author
        copy.thumbnail_url shouldBe source.thumbnail_url
        copy.description shouldBe source.description
        copy.genre shouldBe source.genre
        copy.status shouldBe source.status
        copy.update_strategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        copy.initialized shouldBe true
        copy.memo shouldBe source.memo
    }

    @Test
    fun originalsMirrorFields() {
        val manga = populatedManga()
        manga.originalTitle shouldBe "Title"
        manga.originalAuthor shouldBe "Author"
        manga.originalArtist shouldBe "Artist"
        manga.originalThumbnailUrl shouldBe "https://example.invalid/cover.png"
        manga.originalDescription shouldBe "Description"
        manga.originalGenre shouldBe "Action, Drama"
        manga.originalStatus shouldBe SManga.COMPLETED
    }

    @Test
    fun statusConstantsAreDistinct() {
        val statuses = listOf(
            SManga.UNKNOWN,
            SManga.ONGOING,
            SManga.COMPLETED,
            SManga.LICENSED,
            SManga.PUBLISHING_FINISHED,
            SManga.CANCELLED,
            SManga.ON_HIATUS,
        )
        statuses shouldContainExactly (0..6).toList()
    }
}
