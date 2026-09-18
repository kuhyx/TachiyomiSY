package exh.metadata.metadata

import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.metadata.metadata.base.RaisedTitle
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RaisedSearchMetadataFlattenTest {
    @Test
    fun flattenRequiresSavedManga() {
        shouldThrow<IllegalArgumentException> { RankedSearchMetadata().flatten() }
    }

    @Test
    fun flattenWritesRowTagsAndTitles() {
        val metadata = EHentaiSearchMetadata().apply {
            mangaId = 12
            uploader = "up"
            gId = "123"
            gToken = "tok"
            tags += RaisedTag(namespace = "artist", name = "ann", type = 0)
            tags += RaisedTag(namespace = null, name = "loose", type = 2)
            title = "main"
        }
        val flat = metadata.flatten()
        flat.metadata shouldBe SearchMetadata(
            mangaId = 12,
            uploader = "up",
            extra = """{"type":"exh.metadata.metadata.EHentaiSearchMetadata","gToken":"tok"}""",
            indexedExtra = "123",
            extraVersion = 0,
        )
        flat.tags shouldContainExactly listOf(
            SearchTag(id = null, mangaId = 12, namespace = "artist", name = "ann", type = 0),
            SearchTag(id = null, mangaId = 12, namespace = null, name = "loose", type = 2),
        )
        flat.titles shouldContainExactly listOf(SearchTitle(id = null, mangaId = 12, title = "main", type = 0))
    }

    @Test
    fun flattenWithoutIndexedExtra() {
        val metadata = RankedSearchMetadata().apply {
            mangaId = 3
            rank = 2
        }
        val flat = metadata.flatten()
        flat.metadata shouldBe SearchMetadata(
            mangaId = 3,
            uploader = null,
            extra = """{"type":"exh.metadata.metadata.RankedSearchMetadata","rank":2}""",
            indexedExtra = null,
            extraVersion = 0,
        )
        flat.tags shouldBe emptyList()
        flat.titles shouldBe emptyList()
    }

    @Test
    fun fillBaseFieldsReplacesAll() {
        val metadata = EHentaiSearchMetadata().apply {
            tags += RaisedTag(namespace = "old", name = "gone", type = 0)
            title = "stale"
        }
        val flat = FlatMetadata(
            metadata = SearchMetadata(
                mangaId = 8,
                uploader = "who",
                extra = "{}",
                indexedExtra = "999",
                extraVersion = 0,
            ),
            tags = listOf(SearchTag(id = 1, mangaId = 8, namespace = "ns", name = "n", type = 1)),
            titles = listOf(SearchTitle(id = 2, mangaId = 8, title = "fresh", type = 1)),
        )
        metadata.fillBaseFields(flat)
        metadata.mangaId shouldBe 8
        metadata.uploader shouldBe "who"
        metadata.gId shouldBe "999"
        metadata.tags shouldContainExactly listOf(RaisedTag(namespace = "ns", name = "n", type = 1))
        metadata.titles shouldContainExactly listOf(RaisedTitle(title = "fresh", type = 1))
        metadata.title shouldBe null
        metadata.altTitle shouldBe "fresh"
    }

    @Test
    fun flattenThenFillRoundTrips() {
        val original = RankedSearchMetadata().apply {
            mangaId = 21
            uploader = "u"
            rank = 5
            tags += RaisedTag(namespace = null, name = "x", type = 0)
            titles += RaisedTitle(title = "t", type = 0)
        }
        val restored = RankedSearchMetadata()
        restored.fillBaseFields(original.flatten())
        restored.mangaId shouldBe 21
        restored.uploader shouldBe "u"
        restored.tags shouldContainExactly original.tags
        restored.titles shouldContainExactly original.titles
    }
}
