package exh.metadata.sql.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class SearchMetadataTest {
    private val json = Json
    private val row = SearchMetadata(mangaId = 1, uploader = "up", extra = "{}", indexedExtra = "ix", extraVersion = 2)

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = row
        (row == same) shouldBe true
        (row == row.copy()) shouldBe true
        row.hashCode() shouldBe row.copy().hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        row.equals("row") shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (row == row.copy(mangaId = 2)) shouldBe false
        (row == row.copy(uploader = null)) shouldBe false
        (row == row.copy(extra = "[]")) shouldBe false
        (row == row.copy(indexedExtra = null)) shouldBe false
        (row == row.copy(extraVersion = 3)) shouldBe false
    }

    @Test
    fun hashCodeHandlesNullFields() {
        val bare = row.copy(uploader = null, indexedExtra = null)
        bare.hashCode() shouldNotBe row.hashCode()
        bare.hashCode() shouldBe bare.copy().hashCode()
    }

    @Test
    fun toStringListsFields() {
        row.toString() shouldBe "SearchMetadata(mangaId=1, uploader=up, extra={}, indexedExtra=ix, extraVersion=2)"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        row.copy(mangaId = 7).mangaId shouldBe 7
        row.copy(uploader = "x").uploader shouldBe "x"
        row.copy(extra = "e").extra shouldBe "e"
        row.copy(indexedExtra = "i").indexedExtra shouldBe "i"
        row.copy(extraVersion = 9).extraVersion shouldBe 9
    }

    @Test
    fun componentsMatchFields() {
        val components = listOf(
            row.component1(), row.component2(), row.component3(), row.component4(), row.component5(),
        )
        components shouldBe listOf(1L, "up", "{}", "ix", 2)
    }

    @Test
    fun jsonRoundTripsFullAndNull() {
        val encoded = json.encodeToString(SearchMetadata.serializer(), row)
        encoded shouldBe """{"mangaId":1,"uploader":"up","extra":"{}","indexedExtra":"ix","extraVersion":2}"""
        json.decodeFromString(SearchMetadata.serializer(), encoded) shouldBe row
        val bare = row.copy(uploader = null, indexedExtra = null)
        val bareEncoded = json.encodeToString(SearchMetadata.serializer(), bare)
        bareEncoded shouldBe """{"mangaId":1,"uploader":null,"extra":"{}","indexedExtra":null,"extraVersion":2}"""
        json.decodeFromString(SearchMetadata.serializer(), bareEncoded) shouldBe bare
    }

    @Test
    fun jsonRejectsEmptyObject() {
        shouldThrow<SerializationException> { json.decodeFromString(SearchMetadata.serializer(), "{}") }
    }
}
