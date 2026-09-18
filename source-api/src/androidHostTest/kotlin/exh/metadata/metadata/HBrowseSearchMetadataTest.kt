package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class HBrowseSearchMetadataTest {
    private fun fullMetadata(): HBrowseSearchMetadata = HBrowseSearchMetadata().apply {
        hbId = 5
        hbUrl = "/5/c00001"
        thumbnail = "https://www.hbrowse.com/thumb.jpg"
        title = "Title"
        length = 30
    }

    private fun mangaWithThumbnail(thumbnailUrl: String?): SManga = sampleManga().apply { thumbnail_url = thumbnailUrl }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = HBrowseSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.title shouldBe "Fallback title"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.artist shouldBe ""
        result.author shouldBe "Fallback author"
        result.genre shouldBe ""
        result.description shouldBe "meta"
    }

    @Test
    fun mangaInfoUsesEveryField() {
        val meta = fullMetadata().apply {
            tags += tag("artist", "a1")
            tags += tag("artist", "a2")
        }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/5/c00001"
        result.title shouldBe "Title"
        // The metadata thumbnail is never applied; the manga keeps its own when it has one.
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.artist shouldBe "a1, a2"
        result.genre shouldBe "artist: a1, artist: a2"
    }

    @Test
    fun guessesCoverWhenThumbnailNull() {
        val result = fullMetadata().createMangaInfo(mangaWithThumbnail(null))
        result.thumbnail_url shouldBe "https://www.hbrowse.com/thumbnails/5_1.jpg#guessed"
    }

    @Test
    fun guessesCoverWhenThumbnailBlank() {
        val result = fullMetadata().createMangaInfo(mangaWithThumbnail("  "))
        result.thumbnail_url shouldBe "https://www.hbrowse.com/thumbnails/5_1.jpg#guessed"
    }

    @Test
    fun guessesCoverFromNullIdVerbatim() {
        val result = HBrowseSearchMetadata().createMangaInfo(mangaWithThumbnail(""))
        result.thumbnail_url shouldBe "https://www.hbrowse.com/thumbnails/null_1.jpg#guessed"
    }

    @Test
    fun titleDelegateWritesTitles() {
        fullMetadata().titles shouldContainExactly listOf(RaisedTitle(title = "Title", type = 0))
    }

    @Test
    fun extraPairsListEveryField() {
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "5",
            labelFor(SYMR.strings.url) to "/5/c00001",
            labelFor(SYMR.strings.thumbnail_url) to "https://www.hbrowse.com/thumb.jpg",
            labelFor(MR.strings.title) to "Title",
            labelFor(SYMR.strings.page_count) to "30",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        HBrowseSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(HBrowseSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(HBrowseSearchMetadata.serializer(), encoded)
        decoded.hbId shouldBe 5
        decoded.hbUrl shouldBe "/5/c00001"
        decoded.thumbnail shouldBe "https://www.hbrowse.com/thumb.jpg"
        decoded.length shouldBe 30
        decoded.title shouldBe null
    }

    @Test
    fun jsonOmitsDefaultsAndReadsNulls() {
        Json.encodeToString(HBrowseSearchMetadata.serializer(), HBrowseSearchMetadata()) shouldBe "{}"
        Json.decodeFromString(HBrowseSearchMetadata.serializer(), "{}").hbId shouldBe null
        val text = """{"hbId":null,"hbUrl":null,"thumbnail":null,"length":null}"""
        val decoded = Json.decodeFromString(HBrowseSearchMetadata.serializer(), text)
        decoded.hbId shouldBe null
        decoded.hbUrl shouldBe null
        decoded.thumbnail shouldBe null
        decoded.length shouldBe null
    }
}
