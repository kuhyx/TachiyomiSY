package exh.md.handlers

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.sManga
import exh.md.dto.IncludesAttributesDto
import exh.md.dto.MangaDto
import exh.md.dto.RelationshipDto
import exh.md.dto.StatisticsMangaDto
import exh.md.dto.StatisticsMangaRatingDto
import exh.md.dto.TagAttributesDto
import exh.md.dto.TagDto
import exh.md.dto.chapterAttributes
import exh.md.dto.chapterData
import exh.md.utils.mangaAttributes
import exh.md.utils.mangaData
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class ApiMangaParserTest {
    private val harness = SourceTestHarness()
    private val parser = ApiMangaParser("en")
    private val inserted = mutableListOf<FlatMetadata>()
    private var savedManga: Manga? = null
    private var savedFlat: FlatMetadata? = null
    private val noExtras = MangaDetailsExtras(emptyList(), null, null)

    @Before
    fun setUp() {
        harness.install()
        harness.serve<GetManga>(mockk { coEvery { await(any<String>(), any()) } answers { savedManga } })
        harness.serve<GetFlatMetadataById>(mockk { coEvery { await(any()) } answers { savedFlat } })
        val insert = mockk<InsertFlatMetadata>()
        coEvery { insert.await(any<FlatMetadata>()) } answers { inserted += firstArg<FlatMetadata>() }
        harness.serve(insert)
    }

    @After
    fun tearDown() = harness.uninstall()

    private fun parseFull(): SManga = runBlocking {
        parser.parseToManga(sManga("/manga/uuid-1"), 1L, fullDto(), noExtras, preferences())
    }

    private fun fullDto(): MangaDto {
        val attrs = mangaAttributes(
            title = mapOf("en" to "Title"),
            altTitles = listOf(mapOf("en" to "Alt EN"), mapOf("ja-ro" to "Alt RO"), mapOf("de" to "Alt DE")),
            description = mapOf("en" to "Desc"),
        ).copy(
            links = buildJsonObject { put("al", "7") },
            lastChapter = "12.5",
            status = "completed",
            publicationDemographic = "seinen",
            tags = listOf(TagDto("t", TagAttributesDto(mapOf("en" to "Action")))),
        )
        val relationships = listOf(
            RelationshipDto("a", "author", IncludesAttributesDto(name = "Auth")),
            RelationshipDto("b", "artist", IncludesAttributesDto(name = "Art")),
            RelationshipDto("c", "cover_art", IncludesAttributesDto(fileName = "cover.jpg")),
        )
        return MangaDto("ok", mangaData(attributes = attrs, relationships = relationships))
    }

    @Test
    fun parseIntoMetadataFull() {
        val meta = MangaDexSearchMetadata().apply { tags += exh.metadata.metadata.base.RaisedTag("x", "y", 0) }
        val stats = StatisticsMangaDto(StatisticsMangaRatingDto(average = 8.0, bayesian = 7.9))
        parser.parseIntoMetadata(meta, fullDto(), MangaDetailsExtras(listOf("12.5"), stats, "first.jpg"), preferences())
        meta.mdUuid shouldBe "uuid-1"
        meta.title shouldBe "Title"
        meta.altTitles shouldContainExactly listOf("Alt EN", "Alt RO")
        meta.cover shouldBe "https://uploads.mangadex.org/covers/uuid-1/first.jpg.512.jpg"
        meta.description shouldBe "Desc"
        meta.authors shouldContainExactly listOf("Auth")
        meta.artists shouldContainExactly listOf("Art")
        meta.langFlag shouldBe "ja"
        meta.lastChapterNumber shouldBe 12
        meta.rating shouldBe 7.9f
        meta.anilistId shouldBe "7"
        meta.status shouldBe SManga.COMPLETED
        meta.tags.map { it.name } shouldContainExactly listOf("Seinen", "Action")
    }

    @Test
    fun parseIntoMetadataSparse() {
        val meta = MangaDexSearchMetadata()
        val dto = MangaDto("ok", mangaData(attributes = mangaAttributes(altTitles = listOf(mapOf("fr" to "x")))))
        parser.parseIntoMetadata(meta, dto, MangaDetailsExtras(emptyList(), null, null), preferences())
        meta.altTitles.shouldBeNull()
        meta.cover.shouldBeNull()
        meta.lastChapterNumber.shouldBeNull()
        meta.rating.shouldBeNull()
        meta.status shouldBe SManga.UNKNOWN
        meta.tags.isEmpty() shouldBe true
        val noRating = StatisticsMangaDto(StatisticsMangaRatingDto(null, null))
        parser.parseIntoMetadata(meta, dto, MangaDetailsExtras(emptyList(), noRating, null), preferences())
        meta.rating.shouldBeNull()
    }

    @Test
    fun parseStatuses() {
        val expected = mapOf("ongoing" to SManga.ONGOING, "cancelled" to SManga.CANCELLED, "hiatus" to SManga.ON_HIATUS)
        for ((raw, status) in expected) {
            val meta = MangaDexSearchMetadata()
            val dto = MangaDto("ok", mangaData(attributes = mangaAttributes().copy(status = raw)))
            parser.parseIntoMetadata(meta, dto, noExtras, preferences())
            meta.status shouldBe status
        }
    }

    @Test
    fun parseErrorIsLoggedAndRethrown() {
        val broken = mockk<MangaDto> { every { data } throws IllegalStateException("boom") }
        shouldThrow<IllegalStateException> {
            parser.parseIntoMetadata(MangaDexSearchMetadata(), broken, noExtras, preferences())
        }.message shouldBe "boom"
    }

    @Test
    fun parseToMangaWithoutSavedManga() {
        parseFull().title shouldBe "Title"
        inserted.isEmpty() shouldBe true
    }

    @Test
    fun parseToMangaWithSavedManga() {
        savedManga = Manga.create().copy(id = 9L, url = "/manga/uuid-1", source = 1L)
        parseFull()
        inserted.single().metadata.mangaId shouldBe 9L
        savedFlat = MangaDexSearchMetadata().apply {
            mangaId = 9L
            mdUuid = "old"
        }.flatten()
        parseFull()
        inserted.size shouldBe 2
    }

    @Test
    fun chapterListParse() {
        val groups = mapOf("g1" to "Group One", "g2" to "no group")
        val chapters = listOf(
            chapterData(id = "c1", relationships = listOf(RelationshipDto("g1", "scanlation_group"))),
            chapterData(id = "c2", attributes = chapterAttributes(title = null, volume = null, chapter = " ")),
            chapterData(id = "c3", attributes = chapterAttributes(title = "Only title", volume = null, chapter = null)),
            chapterData(
                id = "c4",
                attributes = chapterAttributes(title = "", volume = "1", chapter = "2"),
                relationships = listOf(
                    RelationshipDto("g2", "scanlation_group"),
                    RelationshipDto("gx", "scanlation_group"),
                ),
            ),
            chapterData(id = "future", attributes = chapterAttributes(publishAt = "2999-01-01T00:00:00+000")),
            chapterData(
                id = "ext",
                attributes = chapterAttributes(externalUrl = "https://x", publishAt = "2999-01-01T00:00:00+000"),
            ),
        )
        val parsed = parser.chapterListParse(chapters, groups)
        parsed.map { it.url } shouldContainExactly
            listOf("/chapter/c1", "/chapter/c2", "/chapter/c3", "/chapter/c4", "/chapter/ext")
        parsed.map { it.name } shouldContainExactly
            listOf("Vol.1 Ch.2 - Title", "Oneshot", "Only title", "Vol.1 Ch.2 ", "Vol.1 Ch.2 - Title")
        parsed.map { it.scanlator } shouldContainExactly
            listOf("Group One", "No Group", "No Group", "No Group", "No Group")
        parsed[0].date_upload shouldBe 1_609_556_645_000L
    }

    @Test
    fun chapterParseForMangaId() {
        val chapter = exh.md.dto.ChapterDto("ok", chapterData(relationships = listOf(RelationshipDto("m1", "Manga"))))
        parser.chapterParseForMangaId(chapter) shouldBe "m1"
        parser.chapterParseForMangaId(exh.md.dto.ChapterDto("ok", chapterData())).shouldBeNull()
        val other = exh.md.dto.ChapterDto("ok", chapterData(relationships = listOf(RelationshipDto("g", "group"))))
        parser.chapterParseForMangaId(other).shouldBeNull()
        with(parser) { StringBuilder().appends("a").toString() } shouldBe "a "
        parser.metaClass shouldBe MangaDexSearchMetadata::class
    }
}
