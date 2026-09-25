package exh.md.dto

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal fun chapterAttributes(
    title: String? = "Title",
    volume: String? = "1",
    chapter: String? = "2",
    externalUrl: String? = null,
    pages: Int = 3,
    publishAt: String = "2021-01-02T03:04:05+000",
): ChapterAttributesDto = ChapterAttributesDto(
    title = title,
    volume = volume,
    chapter = chapter,
    translatedLanguage = "en",
    externalUrl = externalUrl,
    pages = pages,
    version = 1,
    createdAt = "2021-01-01T00:00:00+000",
    updatedAt = "2021-01-01T00:00:00+000",
    publishAt = publishAt,
    readableAt = publishAt,
)

internal fun chapterData(
    id: String = "c1",
    attributes: ChapterAttributesDto = chapterAttributes(),
    relationships: List<RelationshipDto> = emptyList(),
): ChapterDataDto = ChapterDataDto(id = id, type = "chapter", attributes = attributes, relationships = relationships)

internal const val CHAPTER_ATTRIBUTES_JSON: String = """{"title":"Title","volume":"1","chapter":"2",
"translatedLanguage":"en","externalUrl":null,"pages":3,"version":1,"createdAt":"2021-01-01T00:00:00+000",
"updatedAt":"2021-01-01T00:00:00+000","publishAt":"2021-01-02T03:04:05+000","readableAt":"2021-01-02T03:04:05+000"}"""

internal const val CHAPTER_DATA_JSON: String =
    """{"id":"c1","type":"chapter","attributes":$CHAPTER_ATTRIBUTES_JSON,"relationships":[]}"""

internal class ChapterDtoTest {
    @Test
    fun chapterAttributesRoundTrip() {
        val decoded = roundTrip(ChapterAttributesDto.serializer(), CHAPTER_ATTRIBUTES_JSON, chapterAttributes())
        decoded.copy(title = null, volume = null, chapter = null).title shouldBe null
        decoded.externalUrl shouldBe null
    }

    @Test
    fun chapterDataAndDto() {
        val data = roundTrip(ChapterDataDto.serializer(), CHAPTER_DATA_JSON, chapterData())
        data.copy(id = "c2").id shouldBe "c2"
        val dto = roundTrip(
            ChapterDto.serializer(),
            """{"result":"ok","data":$CHAPTER_DATA_JSON}""",
            ChapterDto("ok", data),
        )
        dto.copy(result = "ko").result shouldBe "ko"
        val list = roundTrip(
            ChapterListDto.serializer(),
            """{"limit":100,"offset":0,"total":1,"data":[$CHAPTER_DATA_JSON]}""",
            ChapterListDto(limit = 100, offset = 0, total = 1, data = listOf(data)),
        )
        list.copy(offset = 5).offset shouldBe 5
    }

    @Test
    fun groupDtos() {
        val group = GroupDataDto("g1", GroupAttributesDto("Group"))
        val dto = roundTrip(
            GroupDto.serializer(),
            """{"result":"ok","data":{"id":"g1","attributes":{"name":"Group"}}}""",
            GroupDto("ok", group),
        )
        dto.copy(result = "ko").result shouldBe "ko"
        dto.data.copy(id = "g2").id shouldBe "g2"
        dto.data.attributes.copy(name = "Other").name shouldBe "Other"
        val list = roundTrip(
            GroupListDto.serializer(),
            """{"limit":1,"offset":0,"total":1,"data":[{"id":"g1","attributes":{"name":"Group"}}]}""",
            GroupListDto(limit = 1, offset = 0, total = 1, data = listOf(group)),
        )
        list.copy(limit = 2).limit shouldBe 2
    }
}
