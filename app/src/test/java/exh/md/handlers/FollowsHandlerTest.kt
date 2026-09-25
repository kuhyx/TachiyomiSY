package exh.md.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.CannedServer
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.service.MangaDexAuthService
import exh.md.utils.FollowStatus
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okio.Buffer
import org.junit.jupiter.api.Test

private const val SECOND = """{"id":"m2","type":"manga","attributes":{"title":{"en":"Alpha"},"altTitles":[],""" +
    """"description":{},"links":null,"originalLanguage":"ja","lastVolume":null,"lastChapter":null,""" +
    """"contentRating":null,"publicationDemographic":null,"status":null,"year":null,"tags":[]},"relationships":[]}"""

internal class FollowsHandlerTest {
    private val server = CannedServer()
    private val service = MangaDexAuthService(server.client, headersOf())
    private val handler = FollowsHandler("en", service)

    private fun followsPage(offset: Int, total: Int, vararg data: String) =
        """{"limit":100,"offset":$offset,"total":$total,"data":[${data.joinToString(",")}]}"""

    @Test
    fun fetchFollowsPageSorted() {
        server.queue += followsPage(0, 300, SAMPLE_DATA_JSON, SECOND)
        server.queue += """{"statuses":{"m1":"reading","m2":"reading"}}"""
        val page = runBlocking { handler.fetchFollows(2) }
        server.request().url.queryParameter("offset") shouldBe "40"
        page.hasNextPage shouldBe true
        page.mangas.map { it.title } shouldContainExactly listOf("Alpha", "Title")
        page.mangasMetadata.map { (it as MangaDexSearchMetadata).followStatus } shouldBe listOf(1, 1)
    }

    @Test
    fun fetchFollowsLastPage() {
        server.queue += followsPage(100, 102, SAMPLE_DATA_JSON, SECOND)
        server.queue += """{"statuses":{"m1":"completed"}}"""
        val page = runBlocking { handler.fetchFollows(0) }
        page.hasNextPage shouldBe false
        page.mangas.map { it.title } shouldContainExactly listOf("Alpha", "Title")
        val statuses = page.mangasMetadata.map { (it as MangaDexSearchMetadata).followStatus }
        statuses shouldBe listOf(0, 2)
    }

    @Test
    fun fetchFollowsEmpty() {
        server.body = followsPage(0, 0)
        val page = runBlocking { handler.fetchFollows(0) }
        page.mangas.isEmpty() shouldBe true
        page.hasNextPage shouldBe false
        server.requests.size shouldBe 1
    }

    @Test
    fun fetchAllFollowsPages() {
        server.answers = { request ->
            when {
                request.url.encodedPath.endsWith("/status") -> """{"statuses":{"m1":"on_hold"}}"""
                request.url.queryParameter("offset") == "0" -> followsPage(0, 101, SAMPLE_DATA_JSON)
                else -> followsPage(100, 101, SECOND)
            }
        }
        val all = runBlocking { handler.fetchAllFollows() }
        all.map { it.first.title } shouldContainExactly listOf("Alpha", "Title")
        all.map { it.second.followStatus } shouldContainExactly listOf(0, 3)
    }

    @Test
    fun updateFollowStatus() {
        server.body = """{"result":"ok"}"""
        runBlocking { handler.updateFollowStatus("m1", FollowStatus.READING) } shouldBe true
        server.request(0).method shouldBe "POST"
        server.request(0).url.encodedPath shouldBe "/manga/m1/follow"
        Buffer().also { server.request(1).body?.writeTo(it) }.readUtf8().replace(Regex("\\s"), "") shouldBe
            """{"status":"reading"}"""
        server.body = """{"result":"error"}"""
        runBlocking { handler.updateFollowStatus("m1", FollowStatus.UNFOLLOWED) } shouldBe false
        server.request(2).method shouldBe "DELETE"
        Buffer().also { server.request(3).body?.writeTo(it) }.readUtf8().replace(Regex("\\s"), "") shouldBe
            """{"status":null}"""
    }

    @Test
    fun updateRating() {
        val track = Track.create(TrackerManager.MDLIST).apply { trackingUrl = "/manga/m1" }
        server.body = """{"result":"ok"}"""
        runBlocking { handler.updateRating(track) } shouldBe true
        server.request(0).method shouldBe "DELETE"
        track.score = 7.0
        runBlocking { handler.updateRating(track) } shouldBe true
        server.request(1).method shouldBe "POST"
        server.body = "broken"
        runBlocking { handler.updateRating(track) } shouldBe false
        server.body = """{"result":"error"}"""
        runBlocking { handler.updateRating(track) } shouldBe false
    }

    @Test
    fun fetchTrackingInfo() {
        server.answers = { request ->
            if (request.url.encodedPath.endsWith("/status")) {
                """{"status":"dropped"}"""
            } else {
                """{"ratings":{"m1":{"rating":6,"createdAt":"now"}}}"""
            }
        }
        val track = runBlocking { handler.fetchTrackingInfo("https://mangadex.org/title/m1") }
        track.status shouldBe FollowStatus.DROPPED.long
        track.score shouldBe 6.0
        track.trackingUrl shouldBe "https://mangadex.org/title/m1"
        track.title shouldBe ""
        server.answers = { request ->
            if (request.url.encodedPath.endsWith("/status")) """{"status":null}""" else """{"ratings":[]}"""
        }
        val unrated = runBlocking { handler.fetchTrackingInfo("/manga/m1") }
        unrated.status shouldBe FollowStatus.UNFOLLOWED.long
        unrated.score shouldBe 0.0
    }
}
