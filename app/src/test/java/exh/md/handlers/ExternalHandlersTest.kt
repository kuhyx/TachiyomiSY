package exh.md.handlers

import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test

private const val UA = "test-agent"
private const val WEB = "https://mangaplus.shueisha.co.jp"
private const val OG_42 =
    """<html><head><meta property="og:url" content="https://comikey.com/comics/x/42/"></head></html>"""

internal class ExternalHandlersTest {
    private val server = CannedServer()

    @Test
    fun azukiPages() {
        val handler = AzukiHandler(server.client, UA)
        handler.baseUrl shouldBe "https://www.azuki.co"
        handler.headers["User-Agent"] shouldBe UA
        server.body = """{"pages":[{"image_wm":{"webp":[{"url":"small"},{"url":"https://img/1.webp"}]}}]}"""
        val pages = runBlocking { handler.fetchPageList("https://www.azuki.co/series/x/read/ch1?ref=1") }
        server.request().url.toString() shouldBe "https://production.api.azuki.co/chapter/ch1/pages/v0"
        server.request().header("User-Agent") shouldBe UA
        pages.map { it.imageUrl } shouldContainExactly listOf("https://img/1.webp")
        pages.single().url shouldBe "https://img/1.webp"
    }

    @Test
    fun mangaHotPages() {
        val handler = MangaHotHandler(server.client, UA)
        handler.baseUrl shouldBe "https://mangahot.jp"
        server.body = """{"content":{"contentUrls":["https://img/1.jpg","https://img/2.jpg"]}}"""
        val pages = runBlocking { handler.fetchPageList("https://mangahot.jp/viewer/123?x=1") }
        server.request().url.toString() shouldBe "https://api.mangahot.jp/v1/works/storyDetail/123"
        pages.map { it.imageUrl } shouldContainExactly listOf("https://img/1.jpg", "https://img/2.jpg")
        pages[1].index shouldBe 1
    }

    @Test
    fun namicomiPagesByQuality() {
        val handler = NamicomiHandler(server.client, UA)
        server.body = """{"data":{"baseUrl":"https://cdn","hash":"h","high":[{"filename":"a.png"}],""" +
            """"low":[{"filename":"a.jpg"}]}}"""
        val high = runBlocking { handler.fetchPageList("https://namicomi.com/en/chapter/abc?x=1", dataSaver = false) }
        server.request().url.toString() shouldBe "https://api.namicomi.com/images/chapter/abc?newQualities=true"
        server.request().header("User-Agent") shouldBe UA
        high.single().url shouldBe "https://cdn/chapter/abc/h/high/a.png"
        val low = runBlocking { handler.fetchPageList("https://namicomi.com/en/chapter/abc", dataSaver = true) }
        low.single().imageUrl shouldBe "https://cdn/chapter/abc/h/low/a.jpg"
    }

    @Test
    fun comikeyPagesWhenAllowed() {
        val handler = ComikeyHandler(server.client, UA)
        handler.baseUrl shouldBe "https://comikey.com"
        handler.headers["User-Agent"] shouldBe UA
        server.queue += OG_42
        server.queue += """{"ok":true,"href":"https://comikey.com/sapi/manifest.json"}"""
        server.queue += """{"readingOrder":[{"href":"https://img/1.jpg"},{"href":"https://img/2.jpg"}]}"""
        val pages = runBlocking { handler.fetchPageList("https://comikey.com/read/slug/guid-1/") }
        server.request(0).url.toString() shouldBe "https://comikey.com/read/slug"
        server.request(1).url.toString() shouldBe
            "https://comikey.com/sapi/comics/42/read?format=json&content=EPI-guid-1"
        server.request(2).url.toString() shouldBe "https://comikey.com/sapi/manifest.json"
        server.request(2).header("User-Agent") shouldBe UA
        pages.map { it.imageUrl } shouldContainExactly listOf("https://img/1.jpg", "https://img/2.jpg")
    }

    @Test
    fun comikeyPagesWhenForbidden() {
        val handler = ComikeyHandler(server.client, UA)
        server.queue += OG_42
        server.queue += """{"ok":false}"""
        val pages = runBlocking { handler.fetchPageList("https://comikey.com/read/slug/guid-1/") }
        pages.single().url.startsWith("https://fakeimg.pl/") shouldBe true
        server.queue += OG_42.replace("42", "7")
        server.queue += """{"href":"x"}"""
        runBlocking { handler.fetchPageList("https://comikey.com/read/slug/guid-2/") }.size shouldBe 1
        server.body = OG_42.replace("42", "7")
        runBlocking { handler.getMangaId("slug") } shouldBe 7
        server.queue += OG_42
        server.queue += """{"ok":true}"""
        shouldThrow<NullPointerException> { runBlocking { handler.fetchPageList("https://comikey.com/read/s/g/") } }
        handler.pageListParse(cannedResponse("""{"readingOrder":[]}""")).isEmpty() shouldBe true
    }

    @Test
    fun mangaPlusPages() {
        val stub = InjektStub().apply { install() }
        try {
            val handler = MangaPlusHandler(server.client)
            handler.headers["Origin"] shouldBe WEB
            handler.headers["SESSION-TOKEN"]?.isNotBlank() shouldBe true
            server.body = """{"success":{"mangaViewer":{"pages":[""" +
                """{"mangaPage":{"imageUrl":"https://i/1","width":1,"height":2,"encryptionKey":"ab"}},{},""" +
                """{"mangaPage":{"imageUrl":"https://i/2","width":1,"height":2}}]}}}"""
            val pages = runBlocking { handler.fetchPageList("$WEB/viewer/1000", dataSaver = true) }
            val request = server.request()
            request.url.toString() shouldBe "https://jumpg-webapi.tokyo-cdn.com/api/manga_viewer" +
                "?chapter_id=1000&split=yes&img_quality=low&format=json"
            request.header("Referer") shouldBe "$WEB/viewer/1000"
            pages.map { it.imageUrl } shouldContainExactly listOf("https://i/1#ab", "https://i/2")
            pages.map { it.url }.toSet() shouldBe setOf("$WEB/viewer/1000")
            runBlocking { handler.fetchPageList("1000", dataSaver = false) }
            server.request(1).url.queryParameter("img_quality") shouldBe "super_high"
            server.body = """{"success":null}"""
            shouldThrow<java.io.IOException> { runBlocking { handler.fetchPageList("1000", dataSaver = false) } }
        } finally {
            stub.uninstall()
        }
    }

    // The canned interceptor answers before the handler's own, so the image interceptor is driven directly.
    private fun MangaPlusHandler.intercept(url: String, contentType: String?): Response {
        val request = Request.Builder().url(url).build()
        val canned = cannedResponse("\u0001\u0002\u0003", url).newBuilder()
            .apply { if (contentType != null) header("Content-Type", contentType) }
            .build()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(request) } returns canned
        }
        return invokeDeclared(MangaPlusHandler::class, "imageIntercept", listOf(chain)) as Response
    }

    @Test
    fun mangaPlusDecryptsImages() {
        val stub = InjektStub().apply { install() }
        try {
            val handler = MangaPlusHandler(server.client)
            handler.client.interceptors.size shouldBe server.client.interceptors.size + 3
            handler.intercept("https://cdn/x.png", "image/png").body.bytes() shouldBe byteArrayOf(1, 2, 3)
            val keyed = handler.intercept("https://cdn/x.png#0102", "image/png")
            keyed.body.contentType().toString() shouldBe "image/png"
            keyed.body.bytes() shouldBe byteArrayOf(0, 0, 2)
            handler.intercept("https://cdn/y.png#ff", null).body.contentType().toString() shouldBe "image/jpeg"
            handler.intercept("https://cdn/y.png#", null).body.bytes() shouldBe byteArrayOf(1, 2, 3)
        } finally {
            stub.uninstall()
        }
    }
}
