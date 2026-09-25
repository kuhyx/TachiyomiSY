package exh.md.service

import eu.kanade.tachiyomi.source.online.CannedServer
import exh.md.dto.AtHomeImageReportDto
import exh.md.dto.CHAPTER_DATA_JSON
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okio.Buffer
import org.junit.jupiter.api.Test

internal class MangaDexServiceChaptersTest {
    private val server = CannedServer()
    private val service = MangaDexService(server.client, headersOf("X-Test", "1"))

    @Test
    fun viewChaptersWithBlocks() {
        server.body = """{"limit":100,"offset":0,"total":1,"data":[$CHAPTER_DATA_JSON]}"""
        runBlocking { service.viewChapters("m1", "en", 50, "g1, g2", "u1") }.data.size shouldBe 1
        val url = server.request().url
        url.encodedPath shouldBe "/manga/m1/feed"
        url.queryParameter("limit") shouldBe "500"
        url.queryParameter("includes[]") shouldBe "scanlation_group"
        url.queryParameter("order[volume]") shouldBe "desc"
        url.queryParameter("order[chapter]") shouldBe "desc"
        url.queryParameterValues("contentRating[]") shouldBe listOf("safe", "suggestive", "erotica", "pornographic")
        url.queryParameter("translatedLanguage[]") shouldBe "en"
        url.queryParameter("offset") shouldBe "50"
        url.queryParameterValues("excludedGroups[]") shouldBe listOf("g1", "g2")
        url.queryParameterValues("excludedUploaders[]") shouldBe listOf("u1")
    }

    @Test
    fun viewChaptersWithoutBlocks() {
        server.body = """{"limit":100,"offset":0,"total":0,"data":[]}"""
        runBlocking { service.viewChapters("m1", "en", 0, "", "") }.data.isEmpty() shouldBe true
        server.request().url.queryParameterValues("excludedGroups[]").isEmpty() shouldBe true
    }

    @Test
    fun viewChapter() {
        server.body = """{"result":"ok","data":$CHAPTER_DATA_JSON}"""
        runBlocking { service.viewChapter("c1") }.data.id shouldBe "c1"
        server.request().url.toString() shouldBe "https://api.mangadex.org/chapter/c1"
        server.request().header("X-Test") shouldBe "1"
    }

    @Test
    fun atHomeImageReport() {
        server.body = """{"result":"ok"}"""
        val report = AtHomeImageReportDto(url = "u", success = true, bytes = 5, cached = false, duration = 9)
        runBlocking { service.atHomeImageReport(report) }.result shouldBe "ok"
        val request = server.request()
        request.method shouldBe "POST"
        request.url.toString() shouldBe "https://api.mangadex.network/report"
        Buffer().also { request.body?.writeTo(it) }.readUtf8().replace(Regex("\\s"), "") shouldBe
            """{"url":"u","success":true,"bytes":5,"cached":false,"duration":9}"""
    }

    @Test
    fun atHomeServer() {
        server.body = """{"baseUrl":"https://node","chapter":{"hash":"h","data":["1.png"],"dataSaver":["1.jpg"]}}"""
        val dto = runBlocking { service.getAtHomeServer("https://api.mangadex.org/at-home/server/c1") }
        dto.baseUrl shouldBe "https://node"
        dto.chapter.hash shouldBe "h"
        server.request().url.toString() shouldBe "https://api.mangadex.org/at-home/server/c1"
    }
}
