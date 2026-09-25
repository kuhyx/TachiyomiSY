package exh.recs.sources

import eu.kanade.tachiyomi.source.online.CannedServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException
import tachiyomi.i18n.sy.SYMR

private const val SERIES = """{"recommendations":[{"series_name":"Community","series_url":"https://mu/1",""" +
    """"series_image":{"url":{"original":"https://img/1.jpg"}}}],""" +
    """"category_recommendations":[{"series_name":"Similar","series_url":"https://mu/2"},""" +
    """{"series_name":"NoImage","series_url":"https://mu/3","series_image":{"url":{}}}]}"""

internal class MangaUpdatesPagingSourceTest {
    private val server = CannedServer()
    private val stub = RecsStub(server.client)
    private val community = MangaUpdatesCommunityPagingSource(sourceManga(title = "Needle"))
    private val similar = MangaUpdatesSimilarPagingSource(sourceManga(title = "Needle"))

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun identity() {
        community.name shouldBe "MangaUpdates"
        similar.name shouldBe "MangaUpdates"
        community.category shouldBe SYMR.strings.community_recommendations
        similar.category shouldBe SYMR.strings.similar_titles
        community.associatedTrackerId shouldBe stub.trackerManager.mangaUpdates.id
    }

    @Test
    fun communityRecsById() {
        server.body = SERIES
        val recs = runBlocking { community.getRecsById("5") }
        server.request().url.toString() shouldBe "https://api.mangaupdates.com/v1/series/5"
        recs.single().title shouldBe "Community"
        recs.single().url shouldBe "https://mu/1"
        recs.single().thumbnail_url shouldBe "https://img/1.jpg"
        recs.single().initialized shouldBe true
    }

    @Test
    fun similarRecsById() {
        server.body = SERIES
        val recs = runBlocking { similar.getRecsById("5") }
        recs.map { it.title } shouldContainExactly listOf("Similar", "NoImage")
        recs[0].thumbnail_url.shouldBeNull()
        recs[1].thumbnail_url.shouldBeNull()
    }

    @Test
    fun recsBySearch() {
        server.queue += """{"results":[{"record":{"series_id":"11"}}]}"""
        server.queue += SERIES
        runBlocking { community.getRecsBySearch("Needle") }.size shouldBe 1
        val post = server.request(0)
        post.method shouldBe "POST"
        post.url.toString() shouldBe "https://api.mangaupdates.com/v1/series/search"
        Buffer().also { post.body?.writeTo(it) }.readUtf8() shouldBe """{"search":"Needle","stype":"title"}"""
        server.request(1).url.toString() shouldBe "https://api.mangaupdates.com/v1/series/11"
    }

    @Test
    fun recsBySearchWithoutResults() {
        server.body = """{"results":[]}"""
        shouldThrow<NoResultsException> { runBlocking { community.getRecsBySearch("Needle") } }
    }

    @Test
    fun requestNextPageRoutes() {
        server.body = SERIES
        stub.tracks = listOf(track(trackerId = stub.trackerManager.mangaUpdates.id, remoteId = 3L))
        runBlocking { community.requestNextPage(1) }.mangas.single().title shouldBe "Community"
        server.request().url.toString() shouldBe "https://api.mangaupdates.com/v1/series/3"
    }
}
