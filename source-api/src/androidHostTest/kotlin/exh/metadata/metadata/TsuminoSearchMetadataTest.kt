package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.MetadataUtil
import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

internal class TsuminoSearchMetadataTest {
    private fun fullMetadata(): TsuminoSearchMetadata = TsuminoSearchMetadata().apply {
        tmId = 12
        title = "Title"
        artist = "Artist"
        uploader = "Uploader"
        uploadDate = 1_600_000_000_000L
        length = 20
        ratingString = "4.5 (10 users)"
        averageRating = 4.5f
        userRatings = 10
        favorites = 3
        category = "Doujinshi"
        collection = "Collection"
        group = "Group"
        parody = listOf("p1", "p2")
        character = listOf("c1")
    }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = TsuminoSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.title shouldBe "Fallback title"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.artist shouldBe "Fallback artist"
        result.author shouldBe "Fallback author"
        result.status shouldBe SManga.UNKNOWN
        result.genre shouldBe ""
        result.description shouldBe "meta"
    }

    @Test
    fun mangaInfoUsesEveryField() {
        val meta = fullMetadata().apply { tags += tag("tags", "t1") }
        val result = meta.createMangaInfo(sampleManga())
        result.title shouldBe "Title"
        result.thumbnail_url shouldBe "https://content.tsumino.com/thumbs/12/1"
        result.artist shouldBe "Artist"
        result.status shouldBe SManga.UNKNOWN
        result.genre shouldBe "tags: t1"
        result.description shouldBe "meta"
    }

    @Test
    fun titleDelegateWritesTitles() {
        val meta = TsuminoSearchMetadata().apply { title = "Title" }
        meta.titles shouldContainExactly listOf(RaisedTitle(title = "Title", type = 0))
    }

    @Test
    fun extraPairsListEveryField() {
        val posted = MetadataUtil.EX_DATE_FORMAT.format(
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.systemDefault()),
        )
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "12",
            labelFor(MR.strings.title) to "Title",
            labelFor(SYMR.strings.uploader) to "Uploader",
            labelFor(SYMR.strings.date_posted) to posted,
            labelFor(SYMR.strings.page_count) to "20",
            labelFor(SYMR.strings.rating_string) to "4.5 (10 users)",
            labelFor(SYMR.strings.average_rating) to "4.5",
            labelFor(SYMR.strings.total_ratings) to "10",
            labelFor(SYMR.strings.total_favorites) to "3",
            labelFor(SYMR.strings.genre) to "Doujinshi",
            labelFor(SYMR.strings.collection) to "Collection",
            labelFor(SYMR.strings.group) to "Group",
            labelFor(SYMR.strings.parodies) to "p1, p2",
            labelFor(SYMR.strings.characters) to "c1",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        TsuminoSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(TsuminoSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(TsuminoSearchMetadata.serializer(), encoded)
        decoded.tmId shouldBe 12
        decoded.artist shouldBe "Artist"
        decoded.uploadDate shouldBe 1_600_000_000_000L
        decoded.length shouldBe 20
        decoded.ratingString shouldBe "4.5 (10 users)"
        decoded.averageRating shouldBe 4.5f
        decoded.userRatings shouldBe 10
        decoded.favorites shouldBe 3
        decoded.category shouldBe "Doujinshi"
        decoded.collection shouldBe "Collection"
        decoded.group shouldBe "Group"
        decoded.parody shouldBe listOf("p1", "p2")
        decoded.character shouldBe listOf("c1")
        // Title and uploader are transient.
        decoded.title shouldBe null
        decoded.uploader shouldBe null
    }

    @Test
    fun jsonOmitsDefaults() {
        Json.encodeToString(TsuminoSearchMetadata.serializer(), TsuminoSearchMetadata()) shouldBe "{}"
        val decoded = Json.decodeFromString(TsuminoSearchMetadata.serializer(), "{}")
        decoded.tmId shouldBe null
        decoded.parody shouldBe emptyList()
    }

    @Test
    fun jsonAcceptsExplicitNulls() {
        val text = """
            {"tmId":null,"artist":null,"uploadDate":null,"length":null,"ratingString":null,"averageRating":null,
             "userRatings":null,"favorites":null,"category":null,"collection":null,"group":null,
             "parody":[],"character":[]}
        """.trimIndent()
        val decoded = Json.decodeFromString(TsuminoSearchMetadata.serializer(), text)
        decoded.tmId shouldBe null
        decoded.artist shouldBe null
        decoded.uploadDate shouldBe null
        decoded.length shouldBe null
        decoded.averageRating shouldBe null
        decoded.favorites shouldBe null
        decoded.group shouldBe null
        decoded.character shouldBe emptyList()
    }
}
