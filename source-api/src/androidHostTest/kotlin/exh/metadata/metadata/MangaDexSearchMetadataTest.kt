package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import exh.md.utils.MangaDexRelation
import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class MangaDexSearchMetadataTest {
    private fun fullMetadata(): MangaDexSearchMetadata = MangaDexSearchMetadata().apply {
        mdUuid = "uuid"
        cover = "https://md/cover.jpg"
        title = "Title"
        altTitles = listOf("Alt 1", "Alt 2")
        description = "Description"
        authors = listOf("Author 1", "Author 2")
        artists = listOf("Artist 1")
        langFlag = "ja"
        lastChapterNumber = 12
        rating = 4.5f
        anilistId = "al"
        kitsuId = "ki"
        myAnimeListId = "mal"
        mangaUpdatesId = "mu"
        animePlanetId = "ap"
        status = SManga.ONGOING
        followStatus = 3
        relation = MangaDexRelation.SEQUEL
    }

    @Test
    fun mangaInfoUsesEveryField() {
        val meta = fullMetadata().apply { tags += tag("genre", "Action") }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/manga/uuid"
        result.title shouldBe "Title"
        result.thumbnail_url shouldBe "https://md/cover.jpg"
        result.author shouldBe "Author 1, Author 2"
        result.artist shouldBe "Artist 1"
        result.status shouldBe SManga.ONGOING
        result.genre shouldBe "genre: Action"
        result.description shouldBe "Description"
    }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = MangaDexSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.title shouldBe "Fallback title"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.author shouldBe "Fallback author"
        result.artist shouldBe "Fallback artist"
        result.status shouldBe SManga.LICENSED
        result.genre shouldBe ""
        result.description shouldBe "Fallback description"
    }

    @Test
    fun titleDelegateWritesTitles() {
        val meta = MangaDexSearchMetadata().apply { title = "Title" }
        meta.titles shouldContainExactly listOf(RaisedTitle(title = "Title", type = 0))
        meta.title = null
        meta.titles shouldBe emptyList()
    }

    @Test
    fun extraPairsListEveryField() {
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "uuid",
            labelFor(SYMR.strings.thumbnail_url) to "https://md/cover.jpg",
            labelFor(MR.strings.title) to "Title",
            labelFor(SYMR.strings.author) to "Author 1, Author 2",
            labelFor(SYMR.strings.artist) to "Artist 1",
            labelFor(SYMR.strings.language) to "ja",
            labelFor(SYMR.strings.last_chapter_number) to "12",
            labelFor(SYMR.strings.average_rating) to "4.5",
            labelFor(MR.strings.status) to SManga.ONGOING.toString(),
            labelFor(SYMR.strings.follow_status) to "3",
            labelFor(SYMR.strings.anilist_id) to "al",
            labelFor(SYMR.strings.kitsu_id) to "ki",
            labelFor(SYMR.strings.mal_id) to "mal",
            labelFor(SYMR.strings.manga_updates_id) to "mu",
            labelFor(SYMR.strings.anime_planet_id) to "ap",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        MangaDexSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(MangaDexSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(MangaDexSearchMetadata.serializer(), encoded)
        decoded.mdUuid shouldBe "uuid"
        decoded.cover shouldBe "https://md/cover.jpg"
        decoded.altTitles shouldBe listOf("Alt 1", "Alt 2")
        decoded.description shouldBe "Description"
        decoded.authors shouldBe listOf("Author 1", "Author 2")
        decoded.artists shouldBe listOf("Artist 1")
        decoded.langFlag shouldBe "ja"
        decoded.lastChapterNumber shouldBe 12
        decoded.rating shouldBe 4.5f
        decoded.anilistId shouldBe "al"
        decoded.kitsuId shouldBe "ki"
        decoded.myAnimeListId shouldBe "mal"
        decoded.mangaUpdatesId shouldBe "mu"
        decoded.animePlanetId shouldBe "ap"
        decoded.status shouldBe SManga.ONGOING
        decoded.followStatus shouldBe 3
        decoded.relation shouldBe MangaDexRelation.SEQUEL
        decoded.titles shouldBe emptyList()
    }

    @Test
    fun jsonEncodesRelationByName() {
        val meta = MangaDexSearchMetadata().apply { relation = MangaDexRelation.SIMILAR }
        Json.encodeToString(MangaDexSearchMetadata.serializer(), meta) shouldBe """{"relation":"SIMILAR"}"""
    }

    @Test
    fun jsonOmitsDefaults() {
        Json.encodeToString(MangaDexSearchMetadata.serializer(), MangaDexSearchMetadata()) shouldBe "{}"
        val decoded = Json.decodeFromString(MangaDexSearchMetadata.serializer(), "{}")
        decoded.mdUuid shouldBe null
        decoded.relation shouldBe null
    }

    @Test
    fun jsonAcceptsExplicitNulls() {
        val text = """
            {"mdUuid":null,"cover":null,"altTitles":null,"description":null,"authors":null,"artists":null,
             "langFlag":null,"lastChapterNumber":null,"rating":null,"anilistId":null,"kitsuId":null,
             "myAnimeListId":null,"mangaUpdatesId":null,"animePlanetId":null,"status":null,
             "followStatus":null,"relation":null}
        """.trimIndent()
        val decoded = Json.decodeFromString(MangaDexSearchMetadata.serializer(), text)
        decoded.mdUuid shouldBe null
        decoded.altTitles shouldBe null
        decoded.authors shouldBe null
        decoded.artists shouldBe null
        decoded.rating shouldBe null
        decoded.status shouldBe null
        decoded.followStatus shouldBe null
        decoded.relation shouldBe null
    }
}
