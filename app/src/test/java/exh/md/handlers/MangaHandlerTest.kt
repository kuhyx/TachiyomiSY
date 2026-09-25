package exh.md.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.sManga
import exh.md.dto.CHAPTER_DATA_JSON
import exh.md.dto.MangaDto
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.dto.dtoJson
import exh.md.service.MangaDexService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata

private const val MANGA = """{"result":"ok","data":$SAMPLE_DATA_JSON}"""
private const val AGGREGATE =
    """{"result":"ok","volumes":{"1":{"volume":"1","count":"1","chapters":{"10":{"chapter":"10","count":"1"}}}}}"""
private const val STATS = """{"statistics":{"m1":{"rating":{"average":8.0,"bayesian":7.5}}}}"""
private const val COVER = """{"data":[{"id":"c","attributes":{"fileName":"vol1.jpg"},"relationships":[]}]}"""

internal class MangaHandlerTest {
    private val stub = InjektStub()
    private val server = CannedServer()
    private val service = MangaDexService(server.client, headersOf())
    private val handler = MangaHandler("en", service, ApiMangaParser("en"))

    @BeforeEach
    fun setUp() {
        stub.install()
        stub.serve<GetManga>(mockk { coEvery { await(any<String>(), any()) } returns null })
        stub.serve<GetFlatMetadataById>(mockk { coEvery { await(any()) } returns null })
        stub.serve<InsertFlatMetadata>(mockk())
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    private fun answerByPath() {
        server.answers = { request ->
            when {
                request.url.encodedPath.endsWith("/aggregate") -> AGGREGATE
                request.url.encodedPath.startsWith("/statistics") -> STATS
                request.url.encodedPath.startsWith("/cover") -> COVER
                request.url.encodedPath.startsWith("/manga/uuid-1/feed") ->
                    """{"limit":100,"offset":0,"total":1,"data":[$CHAPTER_DATA_JSON]}"""
                else -> MANGA
            }
        }
    }

    @Test
    fun detailsWithEverything() {
        answerByPath()
        val manga = runBlocking {
            handler.getMangaDetails(sManga("/manga/uuid-1"), 1L, preferences(tryUsingFirstVolumeCover = true))
        }
        manga.title shouldBe "Title"
        manga.thumbnail_url shouldBe "https://uploads.mangadex.org/covers/m1/vol1.jpg.512.jpg"
        manga.status shouldBe eu.kanade.tachiyomi.source.model.SManga.ONGOING
        server.requests.map { it.url.encodedPath }.toSet() shouldBe
            setOf("/manga/uuid-1", "/manga/uuid-1/aggregate", "/statistics/manga", "/cover")
    }

    @Test
    fun detailsWithFailingExtras() {
        server.answers = { request ->
            when {
                request.url.encodedPath.endsWith("/aggregate") -> "not json"
                request.url.encodedPath.startsWith("/statistics") -> "not json"
                else -> MANGA
            }
        }
        val manga = runBlocking { handler.getMangaDetails(sManga("/manga/uuid-1"), 1L, preferences()) }
        manga.thumbnail_url.shouldBeNull()
        manga.status shouldBe eu.kanade.tachiyomi.source.model.SManga.ONGOING
    }

    @Test
    fun chapterList() {
        answerByPath()
        val chapters = runBlocking { handler.getChapterList(sManga("/manga/uuid-1"), "g1", "") }
        chapters.single().name shouldBe "Vol.1 Ch.2 - Title"
        chapters.single().scanlator shouldBe "No Group"
        server.request().url.queryParameterValues("excludedGroups[]") shouldBe listOf("g1")
        val rx = handler.fetchChapterListObservable(sManga("/manga/uuid-1"), "", "")
        (rx.toBlocking().first() as List<*>).size shouldBe 1
    }

    @Test
    fun chapterListWithGroups() {
        val withGroup = CHAPTER_DATA_JSON.replace(
            """"relationships":[]""",
            """"relationships":[{"id":"g1","type":"scanlation_group","attributes":{"name":"Team"}}]""",
        )
        server.body = """{"limit":100,"offset":0,"total":1,"data":[$withGroup]}"""
        runBlocking { handler.getChapterList(sManga("/manga/uuid-1"), "", "") }.single().scanlator shouldBe "Team"
    }

    @Test
    fun randomAndFromChapter() {
        server.body = MANGA
        runBlocking { handler.fetchRandomMangaId() } shouldBe "m1"
        server.body = """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
        runBlocking { handler.getMangaFromChapterId("c1") }.shouldBeNull()
        val withManga =
            CHAPTER_DATA_JSON.replace(""""relationships":[]""", """"relationships":[{"id":"m9","type":"manga"}]""")
        server.body = """{"result":"ok","data":$withManga}"""
        runBlocking { handler.getMangaFromChapterId("c1") } shouldBe "m9"
    }

    @Test
    fun metadataFromTrack() {
        answerByPath()
        val track = Track.create(TrackerManager.MDLIST).apply { trackingUrl = "https://mangadex.org/title/uuid-1" }
        val manga = runBlocking { handler.getMangaMetadata(track, 1L, preferences(tryUsingFirstVolumeCover = true)) }
        manga.url shouldBe "/manga/m1"
        manga.thumbnail_url shouldBe "https://uploads.mangadex.org/covers/m1/vol1.jpg.512.jpg"
        val plain = runBlocking { handler.getMangaMetadata(track, 1L, preferences()) }
        plain.thumbnail_url.shouldBeNull()
        server.requests.count { it.url.encodedPath.startsWith("/cover") } shouldBe 1
    }

    @Test
    fun aggregateCancellation() {
        // Only a cancelled coroutine reaches the rethrow arm: the network layer turns any other failure into IO.
        val cancelling = mockk<MangaDexService>()
        coEvery { cancelling.viewManga(any()) } returns dtoJson.decodeFromString(MangaDto.serializer(), MANGA)
        coEvery { cancelling.mangasRating(*anyVararg()) } throws IllegalStateException("no stats")
        coEvery { cancelling.aggregateChapters(any(), any()) } throws CancellationException("stop")
        val cancelled = MangaHandler("en", cancelling, ApiMangaParser("en"))
        shouldThrow<CancellationException> {
            runBlocking { cancelled.getMangaDetails(sManga("/manga/uuid-1"), 1L, preferences()) }
        }.message shouldBe "stop"
    }
}
