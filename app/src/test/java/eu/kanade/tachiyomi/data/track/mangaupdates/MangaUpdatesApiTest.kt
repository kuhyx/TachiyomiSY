package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MURating
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MangaUpdatesApiTest {

    private val server = FakeServer()
    private lateinit var api: MangaUpdatesApi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        api = MangaUpdatesApi(mockk(), server.client)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(score: Double = 8.5, status: Long = MangaUpdates.READING_LIST) = dbTrack(
        trackerId = TrackerManager.MANGAUPDATES,
        remoteId = 87_654_321L,
        status = status,
        lastChapterRead = 42.0,
        score = score,
    )

    @Test
    fun listItemWithRating() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "list_item.json"))
        server.enqueue(200, fixture("mangaupdates", "rating.json"))
        val (item, rating) = api.getSeriesListItem(track())
        item.listId shouldBe 2L
        rating shouldBe MURating(8.5)
        server.requests[0].url.toString() shouldBe "https://api.mangaupdates.com/v1/lists/series/87654321"
        server.requests[1].url.toString() shouldBe "https://api.mangaupdates.com/v1/series/87654321/rating"
    }

    @Test
    fun listItemWithoutRating() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "list_item.json"))
        server.enqueue(404)
        api.getSeriesListItem(track()).second.shouldBeNull()
        server.enqueue(404)
        shouldThrow<HttpException> { api.getSeriesListItem(track()) }
    }

    @Test
    fun addSeriesMarksReadingOnOk() = runSuspend {
        server.enqueue(200)
        val track = track(status = MangaUpdates.WISH_LIST).apply { lastChapterRead = 0.0 }
        api.addSeriesToList(track, hasReadChapters = true)
        track.status shouldBe MangaUpdates.READING_LIST
        track.lastChapterRead shouldBe 1.0
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/lists/series"
        server.bodies.single() shouldBe """[{"series":{"id":87654321},"list_id":0}]"""
    }

    @Test
    fun addSeriesToWishListOnOtherCode() = runSuspend {
        server.enqueue(201)
        val track = track(status = MangaUpdates.ON_HOLD_LIST)
        api.addSeriesToList(track, hasReadChapters = false)
        track.status shouldBe MangaUpdates.ON_HOLD_LIST
        track.lastChapterRead shouldBe 42.0
        server.bodies.single() shouldBe """[{"series":{"id":87654321},"list_id":1}]"""
    }

    @Test
    fun updateSeriesPutsRating() = runSuspend {
        server.enqueue(200)
        server.enqueue(200)
        api.updateSeriesListItem(track(score = 8.5))
        server.requests[0].url.toString() shouldBe "https://api.mangaupdates.com/v1/lists/series/update"
        server.bodies[0] shouldBe """[{"series":{"id":87654321},"list_id":0,"status":{"chapter":42}}]"""
        server.requests[1].method shouldBe "PUT"
        server.bodies[1] shouldBe """{"rating":8.5}"""
    }

    @Test
    fun updateSeriesDeletesZeroRating() = runSuspend {
        server.enqueue(200)
        server.enqueue(200)
        api.updateSeriesListItem(track(score = 0.0))
        server.requests[1].method shouldBe "DELETE"
        server.requests[1].url.toString() shouldBe "https://api.mangaupdates.com/v1/series/87654321/rating"
    }

    @Test
    fun updateSeriesSkipsNegative() = runSuspend {
        server.enqueue(200)
        api.updateSeriesListItem(track(score = -1.0))
        server.requests.size shouldBe 1
    }

    @Test
    fun deleteSeriesPostsIdList() = runSuspend {
        server.enqueue(200)
        api.deleteSeriesFromList(domainTrack(TrackerManager.MANGAUPDATES, remoteId = 87_654_321L))
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/lists/series/delete"
        server.bodies.single() shouldBe "[87654321]"
        MangaUpdatesApi.ratingUrl(track()) shouldBe "https://api.mangaupdates.com/v1/series/87654321/rating"
    }
}
