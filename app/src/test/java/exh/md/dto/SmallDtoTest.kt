package exh.md.dto

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

internal class SmallDtoTest {
    @Test
    fun atHomeDtos() {
        val chapter = AtHomeChapterDto("h", listOf("a.png"), listOf("a.jpg"))
        val dto = roundTrip(
            AtHomeDto.serializer(),
            """{"baseUrl":"https://x","chapter":{"hash":"h","data":["a.png"],"dataSaver":["a.jpg"]}}""",
            AtHomeDto("https://x", chapter),
        )
        dto.copy(baseUrl = "y").baseUrl shouldBe "y"
        dto.chapter.copy(hash = "z").hash shouldBe "z"
        val report = roundTrip(
            AtHomeImageReportDto.serializer(),
            """{"url":"u","success":true,"duration":5}""",
            AtHomeImageReportDto(url = "u", success = true, duration = 5),
        )
        report.bytes shouldBe null
        report.copy(bytes = 1, cached = true).cached shouldBe true
        roundTrip(
            AtHomeImageReportDto.serializer(),
            """{"url":"u","success":false,"bytes":10,"cached":false,"duration":5}""",
            AtHomeImageReportDto(url = "u", success = false, bytes = 10, cached = false, duration = 5),
        )
    }

    @Test
    fun statisticsDtos() {
        val rating = StatisticsMangaRatingDto(average = 7.5, bayesian = 7.1)
        val dto = roundTrip(
            StatisticsDto.serializer(),
            """{"statistics":{"m1":{"rating":{"average":7.5,"bayesian":7.1}}}}""",
            StatisticsDto(mapOf("m1" to StatisticsMangaDto(rating))),
        )
        dto.copy(statistics = emptyMap()).statistics.isEmpty() shouldBe true
        val blank = rating.copy(average = null, bayesian = null)
        dto.statistics.getValue("m1").copy(rating = blank).rating.average shouldBe null
        roundTrip(
            StatisticsMangaRatingDto.serializer(),
            """{"average":null,"bayesian":null}""",
            StatisticsMangaRatingDto(null, null),
        )
    }

    @Test
    fun ratingDtos() {
        val response = roundTrip(
            RatingResponseDto.serializer(),
            """{"ratings":{"m1":{"rating":8,"createdAt":"now"}}}""",
            RatingResponseDto(
                buildJsonObject {
                    put(
                        "m1",
                        buildJsonObject {
                            put("rating", 8)
                            put("createdAt", "now")
                        },
                    )
                },
            ),
        )
        response.copy(ratings = JsonPrimitive(1)).ratings shouldBe JsonPrimitive(1)
        val personal = roundTrip(
            PersonalRatingDto.serializer(),
            """{"rating":8,"createdAt":"now"}""",
            PersonalRatingDto(8, "now"),
        )
        personal.copy(rating = 9).rating shouldBe 9
        roundTrip(RatingDto.serializer(), """{"rating":3}""", RatingDto(3)).copy(rating = 4).rating shouldBe 4
        val result = roundTrip(ResultDto.serializer(), """{"result":"ok"}""", ResultDto("ok"))
        result.copy(result = "ko").result shouldBe "ko"
    }

    @Test
    fun similarDtos() {
        val match = SimilarMangaMatchListDto("m2", mapOf("en" to "Two"), "safe", 0.9)
        val similar = roundTrip(
            SimilarMangaDto.serializer(),
            """{"id":"m1","title":{"en":"One"},"contentRating":"safe","updatedAt":"now",""" +
                """"matches":[{"id":"m2","title":{"en":"Two"},"contentRating":"safe","score":0.9}]}""",
            SimilarMangaDto("m1", mapOf("en" to "One"), "safe", listOf(match), "now"),
        )
        similar.copy(id = "x").id shouldBe "x"
        similar.matches.single().copy(score = 0.1).score shouldBe 0.1
        val relation = RelationDto(RelationAttributesDto("sequel"), listOf(RelationMangaDto("m3")))
        val list = roundTrip(
            RelationListDto.serializer(),
            """{"response":"collection","data":[{"attributes":{"relation":"sequel"},"relationships":[{"id":"m3"}]}]}""",
            RelationListDto("collection", listOf(relation)),
        )
        list.copy(response = "x").response shouldBe "x"
        relation.copy(relationships = emptyList()).relationships.isEmpty() shouldBe true
        relation.attributes.copy(relation = "prequel").relation shouldBe "prequel"
        relation.relationships.single().copy(id = "m4").id shouldBe "m4"
    }

    @Test
    fun mangaPlusDtos() {
        val page = MangaPage("https://img", 100, 200, "ab")
        val full = roundTrip(
            MangaPlusResponse.serializer(),
            """{"success":{"mangaViewer":{"pages":[{"mangaPage":{"imageUrl":"https://img","width":100,""" +
                """"height":200,"encryptionKey":"ab"}},{}]}}}""",
            MangaPlusResponse(SuccessResult(MangaViewer(listOf(MangaPlusPage(page), MangaPlusPage())))),
        )
        full.copy(success = null).success shouldBe null
        full.success?.copy(mangaViewer = null)?.mangaViewer shouldBe null
        full.success?.mangaViewer?.copy(pages = emptyList())?.pages shouldBe emptyList()
        page.copy(encryptionKey = null).encryptionKey shouldBe null
        roundTrip(MangaPlusResponse.serializer(), "{}", MangaPlusResponse())
        roundTrip(SuccessResult.serializer(), "{}", SuccessResult())
        roundTrip(MangaViewer.serializer(), "{}", MangaViewer())
        roundTrip(MangaPlusPage.serializer(), "{}", MangaPlusPage())
        roundTrip(MangaPage.serializer(), """{"imageUrl":"u","width":1,"height":2}""", MangaPage("u", 1, 2))
    }
}
