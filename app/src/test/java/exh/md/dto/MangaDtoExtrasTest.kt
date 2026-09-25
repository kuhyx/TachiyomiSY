package exh.md.dto

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaDtoExtrasTest {
    @Test
    fun authorDtos() {
        val author = AuthorDto("ok", AuthorDataDto("a1", AuthorAttributesDto("Name")))
        val decoded = roundTrip(
            AuthorDto.serializer(),
            """{"result":"ok","data":{"id":"a1","attributes":{"name":"Name"}}}""",
            author,
        )
        decoded.data.copy(id = "a2").id shouldBe "a2"
        decoded.data.attributes.copy(name = "Other").name shouldBe "Other"
        decoded.copy(result = "ko").result shouldBe "ko"
        val list = roundTrip(
            AuthorListDto.serializer(),
            """{"results":[{"result":"ok","data":{"id":"a1","attributes":{"name":"Name"}}}]}""",
            AuthorListDto(listOf(author)),
        )
        list.copy(results = emptyList()).results.isEmpty() shouldBe true
    }

    @Test
    fun readingStatusDtos() {
        val reading = roundTrip(ReadingStatusDto.serializer(), """{"status":"reading"}""", ReadingStatusDto("reading"))
        reading.copy(status = null).status shouldBe null
        roundTrip(ReadingStatusDto.serializer(), """{"status":null}""", ReadingStatusDto(null))
        val map = roundTrip(
            ReadingStatusMapDto.serializer(),
            """{"statuses":{"m1":"reading","m2":null}}""",
            ReadingStatusMapDto(mapOf("m1" to "reading", "m2" to null)),
        )
        map.copy(statuses = emptyMap()).statuses.isEmpty() shouldBe true
        val read = roundTrip(ReadChapterDto.serializer(), """{"data":["c1"]}""", ReadChapterDto(listOf("c1")))
        read.copy(data = emptyList()).data.isEmpty() shouldBe true
    }

    @Test
    fun coverDtos() {
        val cover = CoverDto("c1", CoverAttributesDto("f.png"), listOf(RelationshipDto("m1", "manga")))
        val decoded = roundTrip(
            CoverListDto.serializer(),
            """{"data":[{"id":"c1","attributes":{"fileName":"f.png"},"relationships":[{"id":"m1","type":"manga"}]}]}""",
            CoverListDto(listOf(cover)),
        )
        decoded.data.single().copy(id = "c2").id shouldBe "c2"
        decoded.data.single().attributes.copy(fileName = "g.png").fileName shouldBe "g.png"
        decoded.copy(data = emptyList()).data.isEmpty() shouldBe true
    }

    @Test
    fun aggregateDtos() {
        val chapter = AggregateChapter("1", "2")
        val volume = AggregateVolume("1", "1", mapOf("1" to chapter))
        val decoded = roundTrip(
            AggregateDto.serializer(),
            """{"result":"ok","volumes":{"1":{"volume":"1","count":"1",""" +
                """"chapters":{"1":{"chapter":"1","count":"2"}}}}}""",
            AggregateDto("ok", mapOf("1" to volume)),
        )
        decoded.copy(result = "ko").result shouldBe "ko"
        decoded.volumes.getValue("1").copy(count = "3").count shouldBe "3"
        decoded.volumes.getValue("1").chapters.getValue("1").copy(chapter = "2").chapter shouldBe "2"
    }
}
