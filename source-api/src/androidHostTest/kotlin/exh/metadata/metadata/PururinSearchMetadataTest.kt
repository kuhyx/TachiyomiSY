package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class PururinSearchMetadataTest {
    private fun fullMetadata(): PururinSearchMetadata = PururinSearchMetadata().apply {
        prId = 1
        prShortLink = "slug"
        title = "Title"
        altTitle = "Alt"
        thumbnailUrl = "https://pururin.me/cover.jpg"
        uploaderDisp = "Disp"
        uploader = "Uploader"
        pages = 9
        fileSize = "12 MB"
        ratingCount = 4
        averageRating = 4.25
    }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = PururinSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.title shouldBe "Fallback title"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.artist shouldBe ""
        result.author shouldBe "Fallback author"
        result.status shouldBe SManga.LICENSED
        result.genre shouldBe ""
        result.description shouldBe "meta"
    }

    @Test
    fun mangaInfoUsesEveryField() {
        val meta = fullMetadata().apply {
            tags += tag("artist", "a1")
            tags += tag("artist", "a2")
            tags += tag("category", "c1")
        }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/gallery/1/slug"
        result.title shouldBe "Title"
        result.thumbnail_url shouldBe "https://pururin.me/cover.jpg"
        result.artist shouldBe "a1, a2"
        result.genre shouldBe "artist: a1, artist: a2, category: c1"
        result.description shouldBe "meta"
    }

    @Test
    fun keyNeedsShortLink() {
        val meta = PururinSearchMetadata().apply { prId = 1 }
        meta.createMangaInfo(sampleManga()).url shouldBe "/fallback/url"
    }

    @Test
    fun keyNeedsId() {
        val meta = PururinSearchMetadata().apply { prShortLink = "slug" }
        meta.createMangaInfo(sampleManga()).url shouldBe "/fallback/url"
    }

    @Test
    fun altTitleIsSecondChoice() {
        val meta = PururinSearchMetadata().apply { altTitle = "Alt" }
        meta.createMangaInfo(sampleManga()).title shouldBe "Alt"
        meta.titles shouldContainExactly listOf(RaisedTitle(title = "Alt", type = 1))
    }

    @Test
    fun extraPairsListEveryField() {
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "1",
            labelFor(MR.strings.title) to "Title",
            labelFor(SYMR.strings.alt_title) to "Alt",
            labelFor(SYMR.strings.thumbnail_url) to "https://pururin.me/cover.jpg",
            labelFor(SYMR.strings.uploader_capital) to "Disp",
            labelFor(SYMR.strings.uploader) to "Uploader",
            labelFor(SYMR.strings.page_count) to "9",
            labelFor(SYMR.strings.gallery_size) to "12 MB",
            labelFor(SYMR.strings.total_ratings) to "4",
            labelFor(SYMR.strings.average_rating) to "4.25",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        PururinSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(PururinSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(PururinSearchMetadata.serializer(), encoded)
        decoded.prId shouldBe 1
        decoded.prShortLink shouldBe "slug"
        decoded.thumbnailUrl shouldBe "https://pururin.me/cover.jpg"
        decoded.uploaderDisp shouldBe "Disp"
        decoded.pages shouldBe 9
        decoded.fileSize shouldBe "12 MB"
        decoded.ratingCount shouldBe 4
        decoded.averageRating shouldBe 4.25
        // Titles and uploader are transient.
        decoded.title shouldBe null
        decoded.altTitle shouldBe null
        decoded.uploader shouldBe null
    }

    @Test
    fun jsonOmitsDefaults() {
        Json.encodeToString(PururinSearchMetadata.serializer(), PururinSearchMetadata()) shouldBe "{}"
        Json.decodeFromString(PururinSearchMetadata.serializer(), "{}").prId shouldBe null
    }

    @Test
    fun jsonAcceptsExplicitNulls() {
        val text = """
            {"prId":null,"prShortLink":null,"thumbnailUrl":null,"uploaderDisp":null,"pages":null,
             "fileSize":null,"ratingCount":null,"averageRating":null}
        """.trimIndent()
        val decoded = Json.decodeFromString(PururinSearchMetadata.serializer(), text)
        decoded.prId shouldBe null
        decoded.prShortLink shouldBe null
        decoded.thumbnailUrl shouldBe null
        decoded.uploaderDisp shouldBe null
        decoded.pages shouldBe null
        decoded.fileSize shouldBe null
        decoded.ratingCount shouldBe null
        decoded.averageRating shouldBe null
    }
}
