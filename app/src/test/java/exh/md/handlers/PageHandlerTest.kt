package exh.md.handlers

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.InjektStub
import exh.md.dto.CHAPTER_DATA_JSON
import exh.md.dto.ChapterDto
import exh.md.dto.chapterAttributes
import exh.md.dto.chapterData
import exh.md.dto.dtoJson
import exh.md.service.MangaDexService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val AT_HOME = "https://api.mangadex.org/at-home/server"

/** A source whose first superclass carries the extension's `helper.tokenTracker`, as MangaDex's does. */
internal class TokenHelper {
    val tokenTracker: HashMap<String, Long> = HashMap()
}

internal open class HelperHolder(val helper: Any)

internal class TrackingSource(helper: Any) : HelperHolder(helper), Source by mockk<Source>()

internal class PageHandlerTest {
    private val stub = InjektStub()
    private val server = CannedServer()
    private val service = MangaDexService(server.client, headersOf())
    private val mangaPlus = mockk<MangaPlusHandler>()
    private val bilibili = mockk<BilibiliHandler>()
    private val azuki = mockk<AzukiHandler>()
    private val mangaHot = mockk<MangaHotHandler>()
    private val namicomi = mockk<NamicomiHandler>()
    private val comikey = mockk<ComikeyHandler>()
    private val handler = PageHandler(
        service,
        PageHandler.ExternalHandlers(
            mangaPlus = mangaPlus,
            comikey = comikey,
            bilibili = bilibili,
            azuki = azuki,
            mangaHot = mangaHot,
            namicomi = namicomi,
        ),
    )
    private val external = listOf(Page(0, "https://ext/1"))

    @BeforeEach
    fun setUp() = stub.install()

    @AfterEach
    fun tearDown() {
        unmockkAll()
        stub.uninstall()
    }

    private fun chapter(scanlator: String?) = SChapter(url = "/chapter/c1", name = "c", scanlator = scanlator)

    private fun chapterJson(pages: Int): String {
        val attributes = chapterAttributes(externalUrl = "https://ext/x", pages = pages, chapter = "3")
        val data = chapterData(attributes = attributes)
        return dtoJson.encodeToString(ChapterDto.serializer(), ChapterDto("ok", data))
    }

    private fun serveExternal() {
        server.body = chapterJson(pages = 0)
    }

    @Test
    fun atHomePagesWithDataSaver() {
        server.queue += """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
        server.queue += """{"baseUrl":"https://node","chapter":{"hash":"h","data":["1.png"],"dataSaver":["1.jpg"]}}"""
        val source = TrackingSource(TokenHelper())
        val pages = runBlocking {
            handler.fetchPageList(chapter(null), usePort443Only = true, dataSaver = true, source)
        }
        pages.single().imageUrl shouldBe "/data-saver/h/1.jpg"
        pages.single().url.startsWith("https://node,$AT_HOME/c1?forcePort443=true,") shouldBe true
        server.request(1).url.toString() shouldBe "$AT_HOME/c1?forcePort443=true"
        (source.helper as TokenHelper).tokenTracker.keys shouldContainExactly setOf("$AT_HOME/c1?forcePort443=true")
    }

    @Test
    fun atHomePagesFullQuality() {
        server.queue += """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
        server.queue += """{"baseUrl":"https://node","chapter":{"hash":"h","data":["1.png"],"dataSaver":["1.jpg"]}}"""
        val pages = runBlocking {
            handler.fetchPageList(chapter(null), usePort443Only = false, dataSaver = false, TrackingSource(Any()))
        }
        pages.single().imageUrl shouldBe "/data/h/1.png"
        server.request(1).url.toString() shouldBe "$AT_HOME/c1"
        server.queue += """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
        server.queue += """{"baseUrl":"https://node","chapter":{"hash":"h","data":[],"dataSaver":[]}}"""
        runBlocking { handler.fetchPageList(chapter(null), usePort443Only = false, dataSaver = false, mockk<Source>()) }
            .isEmpty() shouldBe true
    }

    @Test
    fun externalMangaPlusAndBilibili() {
        coEvery { mangaPlus.fetchPageList("https://ext/x", dataSaver = true) } returns external
        serveExternal()
        runBlocking { handler.fetchPageList(chapter("MangaPlus"), false, true, mockk()) } shouldBe external
        // Bilibili's page functions are extensions, so they are stubbed statically.
        mockkStatic("exh.md.handlers.BilibiliPagesKt")
        coEvery { bilibili.fetchPageList("https://ext/x", "3") } returns external
        serveExternal()
        runBlocking { handler.fetchPageList(chapter("Bilibili Comics"), false, false, mockk()) } shouldBe external
    }

    @Test
    fun externalAzukiHotNamicomi() {
        coEvery { azuki.fetchPageList("https://ext/x") } returns external
        serveExternal()
        runBlocking { handler.fetchPageList(chapter("Azuki Manga"), false, false, mockk()) } shouldBe external
        coEvery { mangaHot.fetchPageList("https://ext/x") } returns external
        serveExternal()
        runBlocking { handler.fetchPageList(chapter("MangaHot"), false, false, mockk()) } shouldBe external
        coEvery { namicomi.fetchPageList("https://ext/x", dataSaver = false) } returns external
        serveExternal()
        runBlocking { handler.fetchPageList(chapter("Namicomi"), false, false, mockk()) } shouldBe external
    }

    @Test
    fun externalUnsupported() {
        serveExternal()
        shouldThrow<IllegalArgumentException> {
            runBlocking { handler.fetchPageList(chapter("Unknown"), false, false, mockk()) }
        }.message shouldBe "Unknown not supported"
        server.queue += chapterJson(pages = 5)
        server.queue += """{"baseUrl":"https://node","chapter":{"hash":"h","data":["1.png"],"dataSaver":[]}}"""
        runBlocking { handler.fetchPageList(chapter("MangaPlus"), false, false, mockk()) }.size shouldBe 1
    }

    @Test
    fun imageCallsByHost() {
        val real = PageHandler(
            service,
            PageHandler.ExternalHandlers(
                mangaPlus = MangaPlusHandler(server.client),
                comikey = ComikeyHandler(server.client, "ua"),
                bilibili = BilibiliHandler(server.client),
                azuki = AzukiHandler(server.client, "ua"),
                mangaHot = MangaHotHandler(server.client, "ua"),
                namicomi = NamicomiHandler(server.client, "ua"),
            ),
        )
        val hosts = listOf(
            "https://mangaplus.example/1",
            "https://comikey.example/1",
            "https://x/bfs/comic/1",
            "https://azuki.example/1",
            "https://mangahot.example/1",
            "https://namicomi.example/1",
        )
        for (host in hosts) {
            real.getImageCall(Page(0, "u", host), 0L)?.request()?.url?.toString() shouldBe host
        }
        real.getImageCall(Page(0, "u", "https://MANGAPLUS.example/2"), 0L)?.request()?.url?.host shouldBe
            "mangaplus.example"
        real.getImageCall(Page(0, "u", "https://comikey.example/2"), 0L)?.request()?.header("User-Agent") shouldBe "ua"
        real.getImageCall(Page(0, "u", "https://other/1"), 5L).shouldBeNull()
        real.getImageCall(Page(0, "u", null), 0L).shouldBeNull()
    }

    @Test
    fun imageUrlRoutesBilibili() {
        val page = Page(0, "https://x/bfs/comic/1")
        mockkStatic("exh.md.handlers.BilibiliPagesKt")
        coEvery { bilibili.getImageUrl(page) } returns "https://token/1"
        runBlocking { handler.getImageUrl(page) { "super" } } shouldBe "https://token/1"
        runBlocking { handler.getImageUrl(Page(0, "https://other")) { "super:${it.url}" } } shouldBe
            "super:https://other"
    }

    @Test
    fun accessibleMember() {
        TokenHelper::class.accessibleMember("tokenTracker")?.name shouldBe "tokenTracker"
        TokenHelper::class.accessibleMember("missing").shouldBeNull()
    }
}
