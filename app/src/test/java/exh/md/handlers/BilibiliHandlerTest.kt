package exh.md.handlers

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.cannedResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val COMIC = """{"code":0,"data":{"id":42,"title":"Comic","ep_list":[""" +
    """{"id":1,"is_locked":false,"ord":1.0,"pub_time":"t","title":"First"},""" +
    """{"id":2,"is_locked":true,"ord":2.0,"pub_time":"t","title":"Locked"},""" +
    """{"id":3,"is_locked":false,"ord":2.5,"pub_time":"t","title":"Half"}]}}"""

internal class BilibiliHandlerTest {
    private val stub = InjektStub()
    private val server = CannedServer()
    private lateinit var handler: BilibiliHandler

    @BeforeEach
    fun setUp() {
        stub.install()
        handler = BilibiliHandler(server.client)
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    private fun body(index: Int): String = Buffer().also { server.request(index).body?.writeTo(it) }.readUtf8()

    @Test
    fun urlHelpers() {
        handler.baseUrl shouldBe "https://www.bilibilicomics.com"
        handler.headers["Origin"] shouldBe "https://www.bilibilicomics.com"
        handler.getMangaUrl("https://www.bilibilicomics.com/detail/mc42?from=x") shouldBe "/detail/mc42"
        handler.getChapterUrl("https://www.bilibilicomics.com/mc42/7?from=x") shouldBe "/mc42/7"
    }

    @Test
    fun chapterList() {
        server.body = COMIC
        val chapters = runBlocking { handler.getChapterList("/detail/mc42") }
        chapters.map { it.url } shouldContainExactly listOf("/mc42/1", "/mc42/3")
        chapters.map { it.name } shouldContainExactly listOf("Ep. 1 - First", "Ep. 2.5 - Half")
        chapters[1].chapter_number shouldBe 2.5f
        val request = server.request()
        request.url.toString() shouldBe
            "https://www.bilibilicomics.com/twirp/comic.v1.Comic/ComicDetail?device=pc&platform=web"
        request.header("Referer") shouldBe "https://www.bilibilicomics.com/detail/mc42"
        request.header("Content-Type") shouldBe "application/json;charset=UTF-8"
        body(0) shouldBe """{"comic_id":42}"""
        handler.chapterListParse(cannedResponse("""{"code":1,"msg":"nope"}""")).isEmpty() shouldBe true
    }

    @Test
    fun pageListFromChapterUrl() {
        server.body = """{"code":0,"data":{"images":[{"path":"/bfs/comic/1.jpg"},{"path":"/bfs/comic/2.jpg"}]}}"""
        val pages = runBlocking { handler.fetchPageList("https://www.bilibilicomics.com/mc42/7", "1") }
        pages.map { it.url } shouldContainExactly listOf("/bfs/comic/1.jpg", "/bfs/comic/2.jpg")
        pages.map { it.index } shouldContainExactly listOf(0, 1)
        server.request().url.encodedPath shouldBe "/twirp/comic.v1.Comic/GetImageIndex"
        server.request().header("Referer") shouldBe "https://www.bilibilicomics.com/mc42/7"
        body(0) shouldBe """{"ep_id":7}"""
        handler.pageListParse(cannedResponse("""{"code":5}""")).isEmpty() shouldBe true
    }

    @Test
    fun pageListFromMangaUrl() {
        server.queue += COMIC
        server.queue += """{"code":0,"data":{"images":[{"path":"/bfs/comic/3.jpg"}]}}"""
        val pages = runBlocking { handler.fetchPageList("https://www.bilibilicomics.com/detail/mc42", "2.5") }
        pages.single().url shouldBe "/bfs/comic/3.jpg"
        body(1) shouldBe """{"ep_id":3}"""
        server.queue += COMIC
        shouldThrow<NoSuchElementException> {
            runBlocking { handler.fetchPageList("https://www.bilibilicomics.com/detail/mc42", "9") }
        }.message shouldBe "Unknown chapter 9"
    }

    @Test
    fun imageUrlWithToken() {
        server.body = """{"code":0,"data":[{"token":"tok","url":"https://img/1.jpg"}]}"""
        runBlocking { handler.getImageUrl(Page(0, "/bfs/comic/1.jpg")) } shouldBe "https://img/1.jpg?token=tok"
        server.request().url.encodedPath shouldBe "/twirp/comic.v1.Comic/ImageToken"
        body(0) shouldBe """{"urls":"[\"/bfs/comic/1.jpg\"]"}"""
    }

    @Test
    fun dtoDefaults() {
        BilibiliHandler.BilibiliResultDto<String>() shouldBe BilibiliHandler.BilibiliResultDto(0, null, "")
        BilibiliHandler.BilibiliReader().images.isEmpty() shouldBe true
        BilibiliHandler.BilibiliComicDto(title = "t").copy(id = 3).id shouldBe 3
        BilibiliHandler.BilibiliPageDto("t", "u").copy(token = "x").token shouldBe "x"
        BilibiliHandler.BilibiliImageDto("p").copy(path = "q").path shouldBe "q"
        val episode =
            BilibiliHandler.BilibiliEpisodeDto(id = 1, isLocked = false, order = 1f, publicationTime = "t", title = "x")
        episode.copy(isLocked = true).isLocked shouldBe true
    }
}
