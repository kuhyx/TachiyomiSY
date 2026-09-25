package exh.md.handlers

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.rawTag
import exh.md.dto.IncludesAttributesDto
import exh.md.dto.RelationshipDto
import exh.md.dto.TagAttributesDto
import exh.md.dto.TagDto
import exh.md.utils.mangaAttributes
import exh.md.utils.mangaData
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ApiMangaMetadataFieldsTest {
    private val harness = SourceTestHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun coverUrlPrefersExplicitFile() {
        val cover = RelationshipDto("c", "cover_art", IncludesAttributesDto(fileName = "rel.jpg"))
        val data = mangaData(relationships = listOf(cover))
        data.coverUrl("first.jpg", ".256.jpg") shouldBe "https://uploads.mangadex.org/covers/uuid-1/first.jpg.256.jpg"
        data.coverUrl("", "") shouldBe "https://uploads.mangadex.org/covers/uuid-1/rel.jpg"
        data.coverUrl(null, "") shouldBe "https://uploads.mangadex.org/covers/uuid-1/rel.jpg"
        mangaData().coverUrl(null, "").shouldBeNull()
        mangaData(relationships = listOf(RelationshipDto("c", "cover_art"))).coverUrl(null, "").shouldBeNull()
        val noFile = RelationshipDto("c", "cover_art", IncludesAttributesDto(name = "x"))
        mangaData(relationships = listOf(noFile)).coverUrl(null, "").shouldBeNull()
    }

    @Test
    fun descriptionOptions() {
        val attrs = mangaAttributes(description = mapOf("en" to "Desc **bold**"))
            .copy(lastVolume = "2", lastChapter = "9")
        attrs.description("en", listOf("Alt"), preferences()) shouldBe "Desc bold"
        attrs.description("en", listOf("Alt"), preferences(altTitlesInDesc = true)) shouldBe
            """
                Desc bold

                Alternative titles:
                • Alt
            """.trimIndent()
        attrs.description("en", null, preferences(finalChapterInDesc = true)) shouldBe
            """
                Desc bold

                Final chapter:
                Vol.2 Ch.9
            """.trimIndent()
        mangaAttributes().description("de", null, preferences()) shouldBe ""
    }

    @Test
    fun relationshipNames() {
        val data = mangaData(
            relationships = listOf(
                RelationshipDto("a", "Author", IncludesAttributesDto(name = "One")),
                RelationshipDto("b", "author", IncludesAttributesDto(name = "One")),
                RelationshipDto("c", "author", IncludesAttributesDto()),
                RelationshipDto("d", "author"),
                RelationshipDto("e", "artist", IncludesAttributesDto(name = "Two")),
            ),
        )
        data.relationshipNames("author") shouldContainExactly listOf("One")
        data.relationshipNames("artist") shouldContainExactly listOf("Two")
    }

    @Test
    fun mangaStatus() {
        val done = mangaAttributes().copy(lastChapter = "10")
        done.mangaStatus(SManga.PUBLISHING_FINISHED, listOf("10")) shouldBe SManga.COMPLETED
        done.mangaStatus(SManga.CANCELLED, listOf("10")) shouldBe SManga.COMPLETED
        done.mangaStatus(SManga.ONGOING, listOf("10")) shouldBe SManga.ONGOING
        done.mangaStatus(SManga.CANCELLED, listOf("9")) shouldBe SManga.CANCELLED
        mangaAttributes().mangaStatus(SManga.PUBLISHING_FINISHED, listOf("10")) shouldBe SManga.PUBLISHING_FINISHED
    }

    @Test
    fun genreTags() {
        val attrs = mangaAttributes().copy(
            publicationDemographic = "shounen",
            contentRating = "erotica",
            tags = listOf(
                TagDto("1", TagAttributesDto(mapOf("de" to "Aktion", "en" to "Action"))),
                TagDto("2", TagAttributesDto(mapOf("en" to "Drama"))),
                TagDto("3", TagAttributesDto(mapOf("ja" to "無"))),
            ),
        )
        attrs.genreTags("de") shouldContainExactly listOf(
            rawTag("Demographic", "Shounen", MangaDexSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("Content Rating", "Erotica", MangaDexSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("Tags", "Aktion", MangaDexSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("Tags", "Drama", MangaDexSearchMetadata.TAG_TYPE_DEFAULT),
        )
        mangaAttributes().copy(contentRating = "safe").genreTags("en").isEmpty() shouldBe true
    }

    @Test
    fun externalLinks() {
        val meta = MangaDexSearchMetadata()
        meta.applyExternalLinks(mangaAttributes())
        meta.anilistId.shouldBeNull()
        meta.applyExternalLinks(mangaAttributes().copy(links = JsonPrimitive("bad")))
        meta.anilistId.shouldBeNull()
        val links = buildJsonObject {
            put("al", "1")
            put("kt", "2")
            put("mal", "3")
            put("mu", "4")
            put("ap", "5")
        }
        meta.applyExternalLinks(mangaAttributes().copy(links = links))
        meta.anilistId shouldBe "1"
        meta.kitsuId shouldBe "2"
        meta.myAnimeListId shouldBe "3"
        meta.mangaUpdatesId shouldBe "4"
        meta.animePlanetId shouldBe "5"
        meta.applyExternalLinks(mangaAttributes().copy(links = buildJsonObject { put("other", "x") }))
        meta.anilistId shouldBe "1"
    }
}
