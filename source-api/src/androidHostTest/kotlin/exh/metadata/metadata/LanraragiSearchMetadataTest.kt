package exh.metadata.metadata

import android.net.Uri
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.sy.SYMR

internal class LanraragiSearchMetadataTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun urlIsNullWithoutId() {
        LanraragiSearchMetadata().url shouldBe null
    }

    @Test
    fun urlDerivesFromId() {
        LanraragiSearchMetadata().apply { arcId = "abc" }.url shouldBe "/reader?id=abc"
    }

    @Test
    fun settingUrlNullKeepsId() {
        val meta = LanraragiSearchMetadata().apply { arcId = "abc" }
        meta.url = null
        meta.arcId shouldBe "abc"
    }

    @Test
    fun settingUrlStoresItVerbatim() {
        val meta = LanraragiSearchMetadata()
        meta.url = "/reader?id=xyz"
        meta.arcId shouldBe "/reader?id=xyz"
    }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = LanraragiSearchMetadata().createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
        result.title shouldBe "Fallback title"
        result.artist shouldBe "Fallback artist"
        result.author shouldBe "Fallback artist"
        result.genre shouldBe ""
        result.status shouldBe SManga.COMPLETED
        result.description shouldBe "Fallback description"
    }

    @Test
    fun mangaInfoUsesEveryField() {
        mockkStatic(Uri::class)
        val builder = stubUriBuilder("https://lrr/api/archives/abc/thumbnail")
        val meta = LanraragiSearchMetadata().apply {
            baseUrl = "https://lrr"
            arcId = "abc"
            title = "Title"
            summary = "Summary"
            tags += tag("artist", "a1")
            tags += tag("artist", "a2")
            tags += tag("other", "o1")
        }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/reader?id=abc"
        result.thumbnail_url shouldBe "https://lrr/api/archives/abc/thumbnail"
        result.title shouldBe "Title"
        result.artist shouldBe "a1, a2"
        result.author shouldBe "a1, a2"
        result.genre shouldBe "artist: a1, artist: a2, other: o1"
        result.status shouldBe SManga.COMPLETED
        result.description shouldBe "Summary"
        verify { Uri.parse("https://lrr/api/archives/abc/thumbnail") }
        verify(exactly = 0) { builder.appendQueryParameter(any(), any()) }
    }

    @Test
    fun coverNeedsBaseUrl() {
        val meta = LanraragiSearchMetadata().apply { arcId = "abc" }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/reader?id=abc"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
    }

    @Test
    fun coverNeedsArcId() {
        val meta = LanraragiSearchMetadata().apply { baseUrl = "https://lrr" }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/fallback/url"
        result.thumbnail_url shouldBe "https://fallback/thumb.jpg"
    }

    @Test
    fun extraPairsListEveryField() {
        val meta = LanraragiSearchMetadata().apply {
            arcId = "abc"
            pageCount = 7
            filename = "file"
            extension = "zip"
            baseUrl = "https://lrr"
        }
        meta.getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "abc",
            labelFor(SYMR.strings.page_count) to "7",
            labelFor(SYMR.strings.filename) to "file",
            labelFor(SYMR.strings.file_extension) to "zip",
            labelFor(SYMR.strings.base_url) to "https://lrr",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        LanraragiSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }
}
