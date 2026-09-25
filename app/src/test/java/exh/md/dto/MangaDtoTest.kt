package exh.md.dto

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

internal val sampleAttributes: MangaAttributesDto = MangaAttributesDto(
    title = buildJsonObject { put("en", "Title") },
    altTitles = listOf(mapOf("ja" to "Alt")),
    description = buildJsonObject { put("en", "Desc") },
    links = buildJsonObject { put("al", "1") },
    originalLanguage = "ja",
    lastVolume = "2",
    lastChapter = "10",
    contentRating = "safe",
    publicationDemographic = "shounen",
    status = "ongoing",
    year = 2020,
    tags = listOf(TagDto("t1", TagAttributesDto(mapOf("en" to "Action")))),
)

internal val sampleData: MangaDataDto = MangaDataDto(
    id = "m1",
    type = "manga",
    attributes = sampleAttributes,
    relationships = listOf(RelationshipDto("a1", "author", IncludesAttributesDto(name = "Auth", fileName = null))),
)

internal const val SAMPLE_ATTRIBUTES_JSON: String = """{"title":{"en":"Title"},"altTitles":[{"ja":"Alt"}],
"description":{"en":"Desc"},"links":{"al":"1"},"originalLanguage":"ja","lastVolume":"2","lastChapter":"10",
"contentRating":"safe","publicationDemographic":"shounen","status":"ongoing","year":2020,
"tags":[{"id":"t1","attributes":{"name":{"en":"Action"}}}]}"""

internal const val SAMPLE_DATA_JSON: String = """{"id":"m1","type":"manga","attributes":$SAMPLE_ATTRIBUTES_JSON,
"relationships":[{"id":"a1","type":"author","attributes":{"name":"Auth"}}]}"""

internal class MangaDtoTest {
    @Test
    fun mangaAttributesFull() {
        val decoded = roundTrip(MangaAttributesDto.serializer(), SAMPLE_ATTRIBUTES_JSON, sampleAttributes)
        decoded.copy(year = null).year shouldBe null
        decoded.tags.single().attributes.name["en"] shouldBe "Action"
        decoded.tags.single().copy(id = "t2").id shouldBe "t2"
        decoded.tags.single().attributes.copy(name = emptyMap()).name.isEmpty() shouldBe true
    }

    @Test
    fun mangaAttributesMinimal() {
        val minimal = """{"title":null,"altTitles":[],"description":null,"links":null,"originalLanguage":"ko",""" +
            """"lastVolume":null,"lastChapter":null,"contentRating":null,"publicationDemographic":null,""" +
            """"status":null,"year":null,"tags":[]}"""
        val decoded = dtoJson.decodeFromString(MangaAttributesDto.serializer(), minimal)
        decoded.title shouldBe JsonNull
        decoded.links shouldBe null
        decoded.originalLanguage shouldBe "ko"
    }

    @Test
    fun mangaDataAndDto() {
        val data = roundTrip(MangaDataDto.serializer(), SAMPLE_DATA_JSON, sampleData)
        data.copy(id = "m2").id shouldBe "m2"
        val dto = roundTrip(
            MangaDto.serializer(),
            """{"result":"ok","data":$SAMPLE_DATA_JSON}""",
            MangaDto("ok", sampleData),
        )
        dto.copy(result = "error").result shouldBe "error"
        val list = roundTrip(
            MangaListDto.serializer(),
            """{"limit":1,"offset":0,"total":3,"data":[$SAMPLE_DATA_JSON]}""",
            MangaListDto(limit = 1, offset = 0, total = 3, data = listOf(sampleData)),
        )
        list.copy(total = 4).total shouldBe 4
    }

    @Test
    fun relationshipDefaults() {
        val bare = roundTrip(
            RelationshipDto.serializer(),
            """{"id":"r","type":"artist"}""",
            RelationshipDto("r", "artist"),
        )
        bare.attributes shouldBe null
        bare.copy(type = "author").type shouldBe "author"
        val includes = roundTrip(IncludesAttributesDto.serializer(), "{}", IncludesAttributesDto())
        includes.copy(name = "n", fileName = "f").fileName shouldBe "f"
        roundTrip(
            IncludesAttributesDto.serializer(),
            """{"name":"n","fileName":"f.png"}""",
            IncludesAttributesDto(name = "n", fileName = "f.png"),
        )
    }

    @Test
    fun tagDtos() {
        val tag = roundTrip(
            TagDto.serializer(),
            """{"id":"t","attributes":{"name":{"en":"Drama"}}}""",
            TagDto("t", TagAttributesDto(mapOf("en" to "Drama"))),
        )
        tag.attributes.name["en"] shouldBe "Drama"
        JsonPrimitive("x").toString() shouldBe "\"x\""
    }
}
