package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Everything but the title in [NHentaiSearchMetadata.createMangaInfo]. */
internal class NHentaiMangaInfoTest {
    @Test
    fun usesFallbacksWhenEmpty() {
        val result = NHentaiSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.artist shouldBe "Fallback artist"
        result.author shouldBe "Fallback artist"
        result.genre shouldBe ""
        result.status shouldBe SManga.COMPLETED
        result.description shouldBe null
        result.initialized shouldBe true
    }

    @Test
    fun appliesIdAndCover() {
        val meta = NHentaiSearchMetadata().apply {
            nhId = 42
            coverImageUrl = "https://t.nhentai.net/cover.jpg"
        }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/g/42/"
        result.thumbnail_url shouldBe "https://t.nhentai.net/cover.jpg"
    }

    @Test
    fun artistTagsBecomeAuthor() {
        val meta = NHentaiSearchMetadata().apply {
            tags += tag("artist", "a1")
            tags += tag("artist", "a2")
        }
        val result = meta.createMangaInfo(sampleManga())
        result.author shouldBe "a1, a2"
        result.artist shouldBe "Fallback artist"
    }

    @Test
    fun groupTagsBecomeArtist() {
        val meta = NHentaiSearchMetadata().apply { tags += tag("group", "g1") }
        val result = meta.createMangaInfo(sampleManga())
        result.artist shouldBe "g1"
        result.author shouldBe "Fallback artist"
    }

    @Test
    fun tagsBecomeGenre() {
        val meta = NHentaiSearchMetadata().apply {
            tags += tag("artist", "a1")
            tags += tag("category", "doujinshi")
            tags += tag(null, "virtual", RaisedSearchMetadata.TAG_TYPE_VIRTUAL)
        }
        meta.createMangaInfo(sampleManga()).genre shouldBe "artist: a1, category: doujinshi"
    }

    @Test
    fun ongoingSuffixSetsOngoing() {
        val meta = NHentaiSearchMetadata().apply { englishTitle = "Story [ongoing]" }
        meta.createMangaInfo(sampleManga()).status shouldBe SManga.ONGOING
    }

    @Test
    fun ongoingSuffixIgnoresCase() {
        val meta = NHentaiSearchMetadata().apply { englishTitle = "Story (WIP)" }
        meta.createMangaInfo(sampleManga()).status shouldBe SManga.ONGOING
    }

    @Test
    fun plainTitleIsCompleted() {
        val meta = NHentaiSearchMetadata().apply { englishTitle = "Story" }
        meta.createMangaInfo(sampleManga()).status shouldBe SManga.COMPLETED
    }

    @Test
    fun ongoingOnlyChecksSuffix() {
        val meta = NHentaiSearchMetadata().apply { englishTitle = "[ongoing] Story" }
        meta.createMangaInfo(sampleManga()).status shouldBe SManga.COMPLETED
    }
}
