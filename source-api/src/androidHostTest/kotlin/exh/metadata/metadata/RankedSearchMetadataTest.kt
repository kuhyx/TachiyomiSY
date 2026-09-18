package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class RankedSearchMetadataTest {
    private val json = Json

    @Test
    fun rankDefaultsToNull() {
        val metadata = RankedSearchMetadata()
        metadata.rank shouldBe null
        metadata.rank = 7
        metadata.rank shouldBe 7
    }

    @Test
    fun createMangaInfoReturnsInput() {
        val manga = SManga(url = "/u", title = "t")
        RankedSearchMetadata().createMangaInfo(manga) shouldBe manga
    }

    @Test
    fun extraInfoPairsAreEmpty() {
        RankedSearchMetadata().getExtraInfoPairs(mockk()) shouldBe emptyList()
    }

    @Test
    fun decodesFullJson() {
        json.decodeFromString(RankedSearchMetadata.serializer(), """{"rank":3}""").rank shouldBe 3
    }

    @Test
    fun decodesEmptyJson() {
        json.decodeFromString(RankedSearchMetadata.serializer(), "{}").rank shouldBe null
    }

    @Test
    fun decodesNullField() {
        json.decodeFromString(RankedSearchMetadata.serializer(), """{"rank":null}""").rank shouldBe null
    }

    @Test
    fun encodesRankOnly() {
        val metadata = RankedSearchMetadata().apply {
            rank = 9
            mangaId = 4
            uploader = "u"
        }
        json.encodeToString(RankedSearchMetadata.serializer(), metadata) shouldBe """{"rank":9}"""
        json.encodeToString(RankedSearchMetadata.serializer(), RankedSearchMetadata()) shouldBe "{}"
    }

    @Test
    fun baseFieldsAreTransient() {
        val decoded = json.decodeFromString(RankedSearchMetadata.serializer(), """{"rank":1}""")
        decoded.mangaId shouldBe -1
        decoded.uploader shouldBe null
        decoded.tags shouldBe emptyList()
        decoded.titles shouldBe emptyList()
        decoded shouldNotBe null
    }
}
