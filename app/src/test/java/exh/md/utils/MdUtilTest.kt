package exh.md.utils

import exh.md.dto.IncludesAttributesDto
import exh.md.dto.RelationshipDto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import org.junit.jupiter.api.Test
import java.text.ParseException

internal class MdUtilTest {
    @Test
    fun urlHelpers() {
        MdUtil.buildMangaUrl("abc") shouldBe "/manga/abc"
        MdUtil.getMangaId("/manga/abc/") shouldBe "abc"
        MdUtil.getMangaId("https://mangadex.org/title/xyz") shouldBe "xyz"
        MdUtil.getChapterId("/chapter/c1") shouldBe "c1"
        MdUtil.cdnCoverUrl("m", "f.jpg") shouldBe "https://uploads.mangadex.org/covers/m/f.jpg"
        MdUtil.getScanlatorString(setOf("b", "a")) shouldBe "a & b"
        MdUtil.mangaLimit shouldBe 20
        MdUtil.chapterSuffix shouldBe "/chapter/"
    }

    @Test
    fun parseDate() {
        MdUtil.parseDate("2021-01-02T03:04:05+000") shouldBe 1_609_556_645_000L
        shouldThrow<ParseException> { MdUtil.parseDate("garbage") }
    }

    @Test
    fun createMangaEntryWithCover() {
        val data = mangaData(
            relationships = listOf(
                RelationshipDto("a", "author", IncludesAttributesDto(name = "Auth")),
                RelationshipDto("c", "cover_art", IncludesAttributesDto(fileName = "cover.png")),
            ),
        )
        val manga = MdUtil.createMangaEntry(data, "en")
        manga.url shouldBe "/manga/uuid-1"
        manga.title shouldBe "Title"
        manga.thumbnail_url shouldBe "https://uploads.mangadex.org/covers/uuid-1/cover.png"
    }

    @Test
    fun createMangaEntryWithoutCover() {
        MdUtil.createMangaEntry(mangaData(), "en").thumbnail_url shouldBe ""
        val noFile = mangaData(relationships = listOf(RelationshipDto("c", "cover_art", IncludesAttributesDto())))
        MdUtil.createMangaEntry(noFile, "en").thumbnail_url shouldBe ""
        val noAttrs = mangaData(relationships = listOf(RelationshipDto("c", "cover_art")))
        MdUtil.createMangaEntry(noAttrs, "en").thumbnail_url shouldBe ""
    }

    @Test
    fun jsonParserIsLenient() {
        MdUtil.jsonParser.parseToJsonElement("{unquoted: 1}").toString() shouldBe "{\"unquoted\":1}"
        JsonNull.asMdMap<String>().isEmpty() shouldBe true
    }
}
