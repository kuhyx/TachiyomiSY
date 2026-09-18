package exh.metadata.metadata

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class EHentaiSearchMetadataFieldsTest {
    private val json = Json

    private val fullJson =
        """{"gToken":"tok","exh":true,"thumbnailUrl":"https://t","genre":"Doujinshi","datePosted":1000,""" +
            """"parent":"/g/1/a/","visible":"Yes","language":"English","translated":true,"size":2048,"length":20,""" +
            """"favorites":3,"ratingCount":4,"averageRating":4.5,"aged":true,"lastUpdateCheck":5000}"""

    private fun full(): EHentaiSearchMetadata = EHentaiSearchMetadata().apply {
        gId = "123"
        gToken = "tok"
        exh = true
        thumbnailUrl = "https://t"
        title = "Romaji"
        altTitle = "日本語"
        genre = "Doujinshi"
        datePosted = 1000
        parent = "/g/1/a/"
        visible = "Yes"
        language = "English"
        translated = true
        size = 2048
        length = 20
        favorites = 3
        ratingCount = 4
        averageRating = 4.5
        aged = true
        lastUpdateCheck = 5000
    }

    @Test
    fun fieldsStartUnset() {
        val metadata = EHentaiSearchMetadata()
        listOf(
            metadata.gId, metadata.gToken, metadata.exh, metadata.thumbnailUrl, metadata.title, metadata.altTitle,
            metadata.genre, metadata.datePosted, metadata.parent, metadata.visible, metadata.language,
            metadata.translated, metadata.size, metadata.length, metadata.favorites, metadata.ratingCount,
            metadata.averageRating,
        ).all { it == null } shouldBe true
        metadata.aged shouldBe false
        metadata.lastUpdateCheck shouldBe 0
    }

    @Test
    fun galleryIdIsTheIndexedExtra() {
        val metadata = EHentaiSearchMetadata().apply { mangaId = 1 }
        metadata.flatten().metadata.indexedExtra shouldBe null
        metadata.gId = "555"
        metadata.gId shouldBe "555"
        metadata.flatten().metadata.indexedExtra shouldBe "555"
    }

    @Test
    fun encodesOnlyOwnFields() {
        val encoded = json.encodeToString(EHentaiSearchMetadata.serializer(), full())
        encoded shouldBe fullJson
        encoded shouldNotContain "gId"
        encoded shouldNotContain "Romaji"
        encoded shouldNotContain "mangaId"
    }

    @Test
    fun encodesDefaultsAsEmptyObject() {
        json.encodeToString(EHentaiSearchMetadata.serializer(), EHentaiSearchMetadata()) shouldBe "{}"
    }

    @Test
    fun decodesFullJson() {
        val decoded = json.decodeFromString(EHentaiSearchMetadata.serializer(), fullJson)
        json.encodeToString(EHentaiSearchMetadata.serializer(), decoded) shouldBe fullJson
        decoded.gToken shouldBe "tok"
        decoded.exh shouldBe true
        decoded.datePosted shouldBe 1000
        decoded.size shouldBe 2048
        decoded.averageRating shouldBe 4.5
        decoded.aged shouldBe true
        decoded.lastUpdateCheck shouldBe 5000
    }

    @Test
    fun decodesEmptyJson() {
        val decoded = json.decodeFromString(EHentaiSearchMetadata.serializer(), "{}")
        decoded.gToken shouldBe null
        decoded.aged shouldBe false
        decoded.lastUpdateCheck shouldBe 0
        decoded.mangaId shouldBe -1
    }

    @Test
    fun decodesNullFields() {
        val nulls =
            """{"gToken":null,"exh":null,"thumbnailUrl":null,"genre":null,"datePosted":null,"parent":null,""" +
                """"visible":null,"language":null,"translated":null,"size":null,"length":null,"favorites":null,""" +
                """"ratingCount":null,"averageRating":null,"aged":false,"lastUpdateCheck":0}"""
        val decoded = json.decodeFromString(EHentaiSearchMetadata.serializer(), nulls)
        decoded.gToken shouldBe null
        decoded.averageRating shouldBe null
        decoded.aged shouldBe false
    }

    @Test
    fun titlesAreStoredAsTitleRows() {
        val metadata = full()
        metadata.getTitleOfType(0) shouldBe "Romaji"
        metadata.getTitleOfType(1) shouldBe "日本語"
    }
}
