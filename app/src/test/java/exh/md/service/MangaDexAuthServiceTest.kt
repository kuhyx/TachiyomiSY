package exh.md.service

import eu.kanade.tachiyomi.source.online.CannedServer
import exh.md.dto.ReadingStatusDto
import exh.md.dto.SAMPLE_DATA_JSON
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okio.Buffer
import org.junit.jupiter.api.Test

internal class MangaDexAuthServiceTest {
    private val server = CannedServer()
    private val service = MangaDexAuthService(server.client, headersOf("Authorization", "Bearer t"))

    private fun body(index: Int = 0): String =
        Buffer().also { server.request(index).body?.writeTo(it) }.readUtf8().replace(Regex("\\s"), "")

    @Test
    fun userFollowList() {
        server.body = """{"limit":100,"offset":20,"total":1,"data":[$SAMPLE_DATA_JSON]}"""
        runBlocking { service.userFollowList(20) }.offset shouldBe 20
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/user/follows/manga?limit=100&offset=20&includes[]=cover_art"
        server.request().header("Authorization") shouldBe "Bearer t"
    }

    @Test
    fun readingStatuses() {
        server.body = """{"status":"reading"}"""
        runBlocking { service.readingStatusForManga("m1") }.status shouldBe "reading"
        server.request().url.toString() shouldBe "https://api.mangadex.org/manga/m1/status"
        server.body = """{"statuses":{"m1":"reading"}}"""
        runBlocking { service.readingStatusAllManga() }.statuses["m1"] shouldBe "reading"
        server.request(1).url.toString() shouldBe "https://api.mangadex.org/manga/status"
        runBlocking { service.readingStatusByType("completed") }.statuses.size shouldBe 1
        server.request(2).url.toString() shouldBe "https://api.mangadex.org/manga/status?status=completed"
    }

    @Test
    fun readChapters() {
        server.body = """{"data":["c1"]}"""
        runBlocking { service.readChaptersForManga("m1") }.data shouldBe listOf("c1")
        server.request().url.toString() shouldBe "https://api.mangadex.org/manga/m1/read"
    }

    @Test
    fun updateReadingStatus() {
        server.body = """{"result":"ok"}"""
        runBlocking { service.updateReadingStatusForManga("m1", ReadingStatusDto("reading")) }.result shouldBe "ok"
        server.request().method shouldBe "POST"
        server.request().url.toString() shouldBe "https://api.mangadex.org/manga/m1/status"
        body() shouldBe """{"status":"reading"}"""
    }

    @Test
    fun markChapters() {
        server.body = """{"result":"ok"}"""
        runBlocking { service.markChapterRead("c1") }.result shouldBe "ok"
        server.request().method shouldBe "POST"
        server.request().url.toString() shouldBe "https://api.mangadex.org/chapter/c1/read"
        runBlocking { service.markChapterUnRead("c1") }.result shouldBe "ok"
        server.request(1).method shouldBe "DELETE"
        server.request(1).url.toString() shouldBe "https://api.mangadex.org/chapter/c1/read"
        server.request(1).header("Authorization") shouldBe "Bearer t"
    }

    @Test
    fun followAndUnfollow() {
        server.body = """{"result":"ok"}"""
        runBlocking { service.followManga("m1") }.result shouldBe "ok"
        server.request().method shouldBe "POST"
        server.request().url.toString() shouldBe "https://api.mangadex.org/manga/m1/follow"
        runBlocking { service.unfollowManga("m1") }.result shouldBe "ok"
        server.request(1).method shouldBe "DELETE"
        server.request(1).url.toString() shouldBe "https://api.mangadex.org/manga/m1/follow"
    }

    @Test
    fun ratings() {
        server.body = """{"result":"ok"}"""
        runBlocking { service.updateMangaRating("m1", 8) }.result shouldBe "ok"
        server.request().url.toString() shouldBe "https://api.mangadex.org/rating/m1"
        body() shouldBe """{"rating":8}"""
        runBlocking { service.deleteMangaRating("m1") }.result shouldBe "ok"
        server.request(1).method shouldBe "DELETE"
        server.request(1).url.toString() shouldBe "https://api.mangadex.org/rating/m1"
        server.body = """{"ratings":{"m1":{"rating":8,"createdAt":"now"}}}"""
        runBlocking { service.mangasRating("m1", "m2") }.ratings.toString().contains("m1") shouldBe true
        server.request(2).url.toString() shouldBe "https://api.mangadex.org/rating?manga%5B%5D=m1&manga%5B%5D=m2"
    }

    @Test
    fun similarService() {
        server.body = """{"id":"m1","title":{"en":"One"},"contentRating":"safe","matches":[],"updatedAt":"now"}"""
        runBlocking { SimilarService(server.client).getSimilarManga("m1") }.id shouldBe "m1"
        server.request().url.toString() shouldBe "https://api.similarmanga.com/similar/m1.json"
    }
}
