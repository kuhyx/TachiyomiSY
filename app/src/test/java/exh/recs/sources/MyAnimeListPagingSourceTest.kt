package exh.recs.sources

import eu.kanade.tachiyomi.source.online.CannedServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.sy.SYMR

private const val RECS = """{"data":[{"entry":{"title":"Rec One","url":"https://mal/1",""" +
    """"images":{"webp":{"image_url":"https://img/1.webp"},"jpg":{"image_url":"https://img/1.jpg"}}}},""" +
    """{"entry":{"title":"Rec Two","url":"https://mal/2","images":{"jpg":{"image_url":"https://img/2.jpg"}}}},""" +
    """{"entry":{"title":"Rec Three","url":"https://mal/3"}}]}"""

internal class MyAnimeListPagingSourceTest {
    private val server = CannedServer()
    private val stub = RecsStub(server.client)
    private val source = MyAnimeListPagingSource(sourceManga(title = "Needle"))

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun identity() {
        source.name shouldBe "MyAnimeList"
        source.category shouldBe SYMR.strings.community_recommendations
        source.associatedTrackerId shouldBe stub.trackerManager.myAnimeList.id
    }

    @Test
    fun recsById() {
        server.body = RECS
        val recs = runBlocking { source.getRecsById("55") }
        server.request().url.toString() shouldBe "https://api.jikan.moe/v4/manga/55/recommendations"
        recs.map { it.title } shouldContainExactly listOf("Rec One", "Rec Two", "Rec Three")
        recs[0].thumbnail_url shouldBe "https://img/1.webp"
        recs[1].thumbnail_url shouldBe "https://img/2.jpg"
        recs[2].thumbnail_url.shouldBeNull()
        recs[0].url shouldBe "https://mal/1"
        recs.all { it.initialized } shouldBe true
    }

    @Test
    fun imageFallbacks() {
        source.getImage(buildJsonObject { put("webp", buildJsonObject { put("image_url", "w") }) }) shouldBe "w"
        source.getImage(buildJsonObject { put("jpg", buildJsonObject { put("image_url", "j") }) }) shouldBe "j"
        source.getImage(buildJsonObject { }).shouldBeNull()
        source.getImage(buildJsonObject { put("webp", buildJsonObject { }) }).shouldBeNull()
    }

    @Test
    fun recsBySearchResolvesId() {
        server.queue += """{"data":[{"mal_id":"77"},{"mal_id":"78"}]}"""
        server.queue += RECS
        runBlocking { source.getRecsBySearch("Needle") }.size shouldBe 3
        server.request(0).url.toString() shouldBe "https://api.jikan.moe/v4/manga?q=Needle"
        server.request(1).url.toString() shouldBe "https://api.jikan.moe/v4/manga/77/recommendations"
    }

    @Test
    fun recsBySearchWithoutResults() {
        server.body = """{"data":[]}"""
        shouldThrow<NoSuchElementException> { runBlocking { source.getRecsBySearch("Needle") } }
    }

    @Test
    fun requestNextPageRoutes() {
        server.body = RECS
        stub.tracks = listOf(track(trackerId = stub.trackerManager.myAnimeList.id, remoteId = 9L))
        runBlocking { source.requestNextPage(1) }.mangas.size shouldBe 3
        server.request().url.toString() shouldBe "https://api.jikan.moe/v4/manga/9/recommendations"
    }
}
