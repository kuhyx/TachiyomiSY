package exh.metadata.metadata

import exh.metadata.metadata.base.RaisedTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class EightMusesSearchMetadataTest {
    private fun fullMetadata(): EightMusesSearchMetadata = EightMusesSearchMetadata().apply {
        path = listOf("comics", "album", "issue-1")
        title = "Title"
        thumbnailUrl = "https://8muses/cover.jpg"
    }

    @Test
    fun emptyPathIsRoot() {
        EightMusesSearchMetadata().createMangaInfo(sampleManga()).url shouldBe "/"
    }

    @Test
    fun mangaInfoFallsBackToManga() {
        val result = EightMusesSearchMetadata().createMangaInfo(sampleManga())
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
            tags += tag("tags", "t1")
        }
        val result = meta.createMangaInfo(sampleManga())
        result.url shouldBe "/comics/album/issue-1"
        result.title shouldBe "Title"
        result.thumbnail_url shouldBe "https://8muses/cover.jpg"
        result.artist shouldBe "a1"
        result.genre shouldBe "artist: a1, tags: t1"
        result.description shouldBe "meta"
    }

    @Test
    fun titleDelegateWritesTitles() {
        fullMetadata().titles shouldContainExactly listOf(RaisedTitle(title = "Title", type = 0))
    }

    @Test
    fun extraPairsListEveryField() {
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(MR.strings.title) to "Title",
            labelFor(SYMR.strings.path) to "/comics/album/issue-1",
            labelFor(SYMR.strings.thumbnail_url) to "https://8muses/cover.jpg",
        )
    }

    @Test
    fun extraPairsSkipEmptyFields() {
        EightMusesSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldBe emptyList()
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(EightMusesSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(EightMusesSearchMetadata.serializer(), encoded)
        decoded.path shouldBe listOf("comics", "album", "issue-1")
        decoded.thumbnailUrl shouldBe "https://8muses/cover.jpg"
        decoded.title shouldBe null
    }

    @Test
    fun jsonOmitsDefaultsAndReadsNulls() {
        Json.encodeToString(EightMusesSearchMetadata.serializer(), EightMusesSearchMetadata()) shouldBe "{}"
        Json.decodeFromString(EightMusesSearchMetadata.serializer(), "{}").path shouldBe emptyList()
        val text = """{"path":[],"thumbnailUrl":null}"""
        val decoded = Json.decodeFromString(EightMusesSearchMetadata.serializer(), text)
        decoded.path shouldBe emptyList()
        decoded.thumbnailUrl shouldBe null
    }

    @Test
    fun constantsNameTheNamespaces() {
        EightMusesSearchMetadata.TAGS_NAMESPACE shouldBe "tags"
        EightMusesSearchMetadata.ARTIST_NAMESPACE shouldBe "artist"
        EightMusesSearchMetadata.TAG_TYPE_DEFAULT shouldBe 0
    }
}
