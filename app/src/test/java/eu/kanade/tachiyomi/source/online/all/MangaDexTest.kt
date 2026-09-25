package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.sManga
import exh.md.dto.CHAPTER_DATA_JSON
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.utils.MdLang
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal const val MD_MANGA_JSON: String = """{"result":"ok","data":$SAMPLE_DATA_JSON}"""
internal const val MD_CHAPTER_JSON: String = """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
internal const val MD_AT_HOME_JSON: String =
    """{"baseUrl":"https://node","chapter":{"hash":"h","data":["1.png"],"dataSaver":["1.jpg"]}}"""

@RunWith(RobolectricTestRunner::class)
internal class MangaDexTest {
    private val harness = SourceTestHarness()
    private lateinit var fixture: MangaDexFixture
    private val source get() = fixture.source

    @Before
    fun setUp() {
        harness.install()
        fixture = MangaDexFixture(harness)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun identity() {
        source.lang shouldBe "en"
        source.mdLang shouldBe MdLang.ENGLISH
        MangaDexFixture(harness, lang = "pt-BR").source.mdLang shouldBe MdLang.PORTUGUESE_BR
        MangaDexFixture(harness, lang = "xx").source.mdLang shouldBe MdLang.ENGLISH
        source.matchingHosts shouldContainExactly listOf("mangadex.org", "www.mangadex.org")
        source.headers["User-Agent"] shouldStartWith "TachiyomiSY v"
        source.baseHttpClient.interceptors.contains(fixture.interceptor) shouldBe true
        source.metaClass shouldBe MangaDexSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe MangaDexSearchMetadata::class.java
        source.requiresLogin shouldBe false
        source.twoFactorAuth shouldBe LoginSource.AuthSupport.NOT_SUPPORTED
        source.trackPreferences shouldBe fixture.trackPreferences
        source.mdList shouldBe fixture.mdList
    }

    @Test
    fun mapUrls() {
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://mangadex.org/Title/abc/slug")) } shouldBe "/manga/abc"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://mangadex.org/manga/abc")) } shouldBe "/manga/abc"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://mangadex.org/group/abc")) }.shouldBeNull()
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://mangadex.org")) }.shouldBeNull()
        source.mapUrlToChapterUrl(Uri.parse("https://mangadex.org/Chapter/c1")) shouldBe
            "https://api.mangadex.org/chapter/c1"
        source.mapUrlToChapterUrl(Uri.parse("https://mangadex.org/chapter")).shouldBeNull()
        source.mapUrlToChapterUrl(Uri.parse("https://mangadex.org/title/x")).shouldBeNull()
        source.mapUrlToChapterUrl(Uri.parse("https://mangadex.org")).shouldBeNull()
    }

    @Test
    fun chapterUrlToMangaUrl() {
        runBlocking { source.mapChapterUrlToMangaUrl(Uri.parse("https://mangadex.org/chapter")) }.shouldBeNull()
        harness.enqueue(MD_CHAPTER_JSON)
        runBlocking { source.mapChapterUrlToMangaUrl(Uri.parse("https://mangadex.org/chapter/c1")) }.shouldBeNull()
        harness.takeRequest().target shouldBe "/chapter/c1"
        val withManga =
            CHAPTER_DATA_JSON.replace(""""relationships":[]""", """"relationships":[{"id":"m9","type":"manga"}]""")
        harness.enqueue("""{"result":"ok","data":$withManga}""")
        runBlocking { source.mapChapterUrlToMangaUrl(Uri.parse("https://mangadex.org/chapter/c1")) } shouldBe
            "/manga/m9"
    }

    @Test
    fun latestUpdatesDropFutureFlag() {
        harness.enqueue("body")
        runBlocking { source.getLatestUpdates(3) }.mangas.single().url shouldBe "/latest/body"
        harness.takeRequest().target shouldBe "/latest?page=3"
        harness.enqueue("rx")
        val rx = source.invokeDeclared(MangaDex::class, "fetchLatestUpdates", listOf(1))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/latest/rx"
    }

    @Test
    fun detailsAndChapters() {
        harness.answer { target ->
            when {
                target.startsWith("/manga/m1/feed") ->
                    """{"limit":100,"offset":0,"total":1,"data":[$CHAPTER_DATA_JSON]}"""
                target.startsWith("/manga/m1/aggregate") -> """{"result":"ok","volumes":{}}"""
                target.startsWith("/statistics") -> """{"statistics":{}}"""
                target.startsWith("/manga/m1") -> MD_MANGA_JSON
                else -> null
            }
        }
        runBlocking { source.getMangaDetails(sManga("/manga/m1")) }.title shouldBe "Title"
        val rxDetails = source.invokeDeclared(MangaDex::class, "fetchMangaDetails", listOf(sManga("/manga/m1")))
        ((rxDetails as rx.Observable<*>).toBlocking().first() as eu.kanade.tachiyomi.source.model.SManga).title shouldBe
            "Title"
        val rxChapters = source.invokeDeclared(MangaDex::class, "fetchChapterList", listOf(sManga("/manga/m1")))
        ((rxChapters as rx.Observable<*>).toBlocking().first() as List<*>).size shouldBe 1
    }

    @Test
    fun pagesAndImages() {
        harness.answer { target ->
            when {
                target.startsWith("/chapter/c1") -> MD_CHAPTER_JSON
                target.startsWith("/at-home/server/c1") -> MD_AT_HOME_JSON
                else -> null
            }
        }
        val chapter = SChapter(url = "/chapter/c1", name = "c")
        runBlocking { source.getPageList(chapter) }.single().imageUrl shouldBe "/data/h/1.png"
        val rx = source.invokeDeclared(MangaDex::class, "fetchPageList", listOf(chapter))
        ((rx as rx.Observable<*>).toBlocking().first() as List<*>).size shouldBe 1
        runBlocking { source.getImageUrl(Page(0, "https://x/other")) } shouldBe "resolved:https://x/other"
        val rxImage = source.invokeDeclared(MangaDex::class, "fetchImageUrl", listOf(Page(0, "https://x/other")))
        (rxImage as rx.Observable<*>).toBlocking().first() shouldBe "resolved:https://x/other"
        runBlocking { source.getImage(Page(0, "u", "https://x/plain.png"), 0L) }.body.string() shouldBe
            "image:u"
        harness.answer { "bytes" }
        runBlocking { source.getImage(Page(0, "u", "https://mangaplus/1.png"), 0L) }.body.string() shouldBe "bytes"
    }

    @Test
    fun parseIntoMetadataUsesParser() {
        val meta = MangaDexSearchMetadata()
        val dto = exh.md.dto.dtoJson.decodeFromString(exh.md.dto.MangaDto.serializer(), MD_MANGA_JSON)
        val stats = exh.md.dto.StatisticsMangaDto(exh.md.dto.StatisticsMangaRatingDto(average = 8.0, bayesian = 7.5))
        runBlocking { source.parseIntoMetadata(meta, Triple(dto, emptyList(), stats)) }
        meta.mdUuid shouldBe "m1"
        meta.rating shouldBe 7.5f
        meta.title shouldBe "Title"
    }

    @Test
    fun randomAndFollowsPassThrough() {
        harness.answer { target ->
            when {
                target.startsWith("/manga/random") -> MD_MANGA_JSON
                target.startsWith("/user/follows") -> """{"limit":100,"offset":0,"total":0,"data":[]}"""
                target.startsWith("/manga/status") -> """{"statuses":{}}"""
                else -> null
            }
        }
        runBlocking { source.fetchRandomMangaUrl() } shouldBe "m1"
        runBlocking { source.fetchFollows(1) }.mangas.isEmpty() shouldBe true
        runBlocking { source.fetchAllFollows() }.isEmpty() shouldBe true
    }
}
