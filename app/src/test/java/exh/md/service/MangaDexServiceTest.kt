package exh.md.service

import eu.kanade.tachiyomi.source.online.CannedServer
import exh.md.dto.MangaDto
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.dto.sampleData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.Test

internal const val MANGA_JSON: String = """{"result":"ok","data":$SAMPLE_DATA_JSON}"""

internal class MangaDexServiceTest {
    private val server = CannedServer()
    private val service = MangaDexService(server.client, headersOf("X-Test", "1"))

    @Test
    fun viewMangas() {
        server.body = """{"limit":2,"offset":0,"total":2,"data":[$SAMPLE_DATA_JSON]}"""
        val list = runBlocking { service.viewMangas(listOf("a", "b")) }
        list.data shouldContainExactly listOf(sampleData)
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/manga?includes%5B%5D=cover_art&limit=2&ids%5B%5D=a&ids%5B%5D=b"
        server.request().header("X-Test") shouldBe "1"
        server.request().cacheControl.noCache shouldBe true
    }

    @Test
    fun viewManga() {
        server.body = MANGA_JSON
        runBlocking { service.viewManga("m1") } shouldBe MangaDto("ok", sampleData)
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/manga/m1?includes%5B%5D=cover_art&includes%5B%5D=author&includes%5B%5D=artist"
    }

    @Test
    fun mangasRating() {
        server.body = """{"statistics":{"m1":{"rating":{"average":7.5,"bayesian":7.1}}}}"""
        runBlocking { service.mangasRating("m1", "m2") }.statistics.keys shouldContainExactly setOf("m1")
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/statistics/manga?manga%5B%5D=m1&manga%5B%5D=m2"
    }

    @Test
    fun aggregateChapters() {
        server.body = """{"result":"ok","volumes":{}}"""
        runBlocking { service.aggregateChapters("m1", "en") }.volumes.isEmpty() shouldBe true
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/manga/m1/aggregate?translatedLanguage%5B%5D=en"
    }

    @Test
    fun randomAndRelated() {
        server.body = MANGA_JSON
        runBlocking { service.randomManga() }.data.id shouldBe "m1"
        server.request().url.toString() shouldBe "https://api.mangadex.org/manga/random"
        server.body = """{"response":"collection","data":[]}"""
        runBlocking { service.relatedManga("m1") }.data.isEmpty() shouldBe true
        server.request(1).url.toString() shouldBe "https://api.mangadex.org/manga/m1/relation"
    }

    @Test
    fun firstVolumeCover() {
        server.body = """{"data":[{"id":"c","attributes":{"fileName":"vol1.jpg"},"relationships":[]}]}"""
        runBlocking { service.fetchFirstVolumeCover(MangaDto("ok", sampleData)) } shouldBe "vol1.jpg"
        server.request().url.toString() shouldBe
            "https://api.mangadex.org/cover?order%5Bvolume%5D=asc&manga%5B%5D=m1&locales%5B%5D=ja&limit=1"
        server.body = """{"data":[]}"""
        runBlocking { service.fetchFirstVolumeCover(MangaDto("ok", sampleData)) }.shouldBeNull()
    }

    @Test
    fun splitString() {
        with(service) { "a,\n b,,c ".splitString() } shouldContainExactly listOf("a", "b", "c")
        with(service) { "".splitString() }.isEmpty() shouldBe true
    }
}
