package exh.metadata.metadata.base

import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RankedSearchMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class FlatMetadataTest {
    private val json = Json
    private val row = SearchMetadata(mangaId = 5, uploader = "up", extra = "{}", indexedExtra = "ix", extraVersion = 0)
    private val tag = SearchTag(id = 1, mangaId = 5, namespace = "ns", name = "n", type = 0)
    private val title = SearchTitle(id = 2, mangaId = 5, title = "t", type = 1)
    private val flat = FlatMetadata(metadata = row, tags = listOf(tag), titles = listOf(title))

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = flat
        (flat == same) shouldBe true
        (flat == FlatMetadata(metadata = row, tags = listOf(tag), titles = listOf(title))) shouldBe true
        flat.hashCode() shouldBe FlatMetadata(metadata = row, tags = listOf(tag), titles = listOf(title)).hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        flat.equals(row) shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (flat == flat.copy(metadata = row.copy(mangaId = 6))) shouldBe false
        (flat == flat.copy(tags = emptyList())) shouldBe false
        (flat == flat.copy(titles = emptyList())) shouldBe false
    }

    @Test
    fun toStringListsFields() {
        flat.toString() shouldBe "FlatMetadata(metadata=$row, tags=[$tag], titles=[$title])"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        flat.copy() shouldBe flat
        flat.copy(metadata = row.copy(mangaId = 9)).metadata.mangaId shouldBe 9
        flat.copy(tags = emptyList()).tags shouldBe emptyList()
        flat.copy(titles = emptyList()).titles shouldBe emptyList()
    }

    @Test
    fun componentsMatchFields() {
        val (metadata, tags, titles) = flat
        metadata shouldBe row
        tags shouldContainExactly listOf(tag)
        titles shouldContainExactly listOf(title)
    }

    @Test
    fun jsonRoundTrips() {
        val encoded = json.encodeToString(FlatMetadata.serializer(), flat)
        json.decodeFromString(FlatMetadata.serializer(), encoded) shouldBe flat
        val empty = FlatMetadata(metadata = row, tags = emptyList(), titles = emptyList())
        val emptyEncoded = json.encodeToString(FlatMetadata.serializer(), empty)
        val expected =
            """{"metadata":{"mangaId":5,"uploader":"up","extra":"{}","indexedExtra":"ix","extraVersion":0},""" +
                """"tags":[],"titles":[]}"""
        emptyEncoded shouldBe expected
        json.decodeFromString(FlatMetadata.serializer(), emptyEncoded) shouldBe empty
    }

    @Test
    fun raiseInflatesRankedMetadata() {
        val ranked = flat.copy(metadata = row.copy(extra = """{"rank":4}""")).raise(RankedSearchMetadata::class)
        ranked.rank shouldBe 4
        ranked.mangaId shouldBe 5
        ranked.uploader shouldBe "up"
        ranked.tags shouldContainExactly listOf(RaisedTag(namespace = "ns", name = "n", type = 0))
        ranked.titles shouldContainExactly listOf(RaisedTitle(title = "t", type = 1))
    }

    @Test
    fun raiseInflatesEHentaiMetadata() {
        val extra = """{"gToken":"tok","exh":true,"unknown":1}"""
        val raised = flat.copy(metadata = row.copy(extra = extra)).raise(EHentaiSearchMetadata::class)
        raised.gToken shouldBe "tok"
        raised.exh shouldBe true
        raised.gId shouldBe "ix"
        raised.title shouldBe null
        raised.altTitle shouldBe "t"
    }
}
