package eu.kanade.tachiyomi.data.track.komga

import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KomgaApiTest {

    private val harness = KomgaHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun seriesUsesV2Progress() = runTest {
        harness.enqueue("series.json")
        harness.enqueue("progress_v2.json")
        val track = harness.api.getTrackSearch(KomgaHarness.SERIES_URL)
        track.trackerId shouldBe 6L
        track.title shouldBe "Komga Title"
        track.summary shouldBe "A summary"
        track.publishingStatus shouldBe "ONGOING"
        track.coverUrl shouldBe "${KomgaHarness.SERIES_URL}/thumbnail"
        track.trackingUrl shouldBe KomgaHarness.SERIES_URL
        track.totalChapters shouldBe 10L
        track.status shouldBe Komga.READING
        track.lastChapterRead shouldBe 4.5

        val series = harness.takeRequest()
        series.url.encodedPath shouldBe "/api/v1/series/series-1"
        series.headers["User-Agent"].orEmpty() shouldStartWith "TachiyomiSY v"
        harness.takeRequest().url.encodedPath shouldBe "/api/v2/series/series-1/read-progress/tachiyomi"
    }

    @Test
    fun readListUsesV1Progress() = runTest {
        harness.enqueue("readlist.json")
        harness.enqueue("progress_v1.json")
        val track = harness.api.getTrackSearch(KomgaHarness.READLIST_URL)
        track.title shouldBe "My Read List"
        track.summary shouldBe ""
        track.totalChapters shouldBe 2L
        track.status shouldBe Komga.COMPLETED
        track.lastChapterRead shouldBe 2.0
        harness.takeRequest().url.encodedPath shouldBe "/api/v1/readlists/readlist-1"
        harness.takeRequest().url.encodedPath shouldBe "/api/v1/readlists/readlist-1/read-progress/tachiyomi"
    }

    @Test
    fun statusFollowsTheBookCounts() = runTest {
        harness.enqueue("series.json")
        harness.enqueueProgress(booksCount = 3, unread = 3)
        harness.api.getTrackSearch(KomgaHarness.SERIES_URL).status shouldBe Komga.UNREAD

        harness.enqueue("series.json")
        harness.enqueueProgress(booksCount = 3, unread = 0)
        harness.api.getTrackSearch(KomgaHarness.SERIES_URL).status shouldBe Komga.COMPLETED

        harness.enqueue("series.json")
        harness.enqueueProgress(booksCount = 3, unread = 1)
        harness.api.getTrackSearch(KomgaHarness.SERIES_URL).status shouldBe Komga.READING
    }

    @Test
    fun errorsAreLoggedAndRethrown() = runTest {
        harness.enqueueRaw("", code = 404)
        shouldThrow<HttpException> { harness.api.getTrackSearch(KomgaHarness.SERIES_URL) }.code shouldBe 404
        harness.koin.logged.last() shouldStartWith "Could not get item: ${KomgaHarness.SERIES_URL}"
    }

    @Test
    fun updateSeriesProgress() = runTest {
        harness.enqueueRaw("")
        harness.enqueue("series.json")
        harness.enqueue("progress_v2.json")
        val track = dbTrack(trackerId = 6L, lastChapterRead = 3.5, trackingUrl = KomgaHarness.SERIES_URL)
        harness.api.updateProgress(track).lastChapterRead shouldBe 4.5
        val put = harness.takeRequest()
        put.method shouldBe "PUT"
        put.url.encodedPath shouldBe "/api/v2/series/series-1/read-progress/tachiyomi"
        put.headers["Content-Type"] shouldBe "application/json; charset=utf-8"
        put.bodyText() shouldBe """{"lastBookNumberSortRead":3.5}"""
    }

    @Test
    fun updateReadListProgress() = runTest {
        harness.enqueueRaw("")
        harness.enqueue("readlist.json")
        harness.enqueue("progress_v1.json")
        val track = dbTrack(trackerId = 6L, lastChapterRead = 1.9, trackingUrl = KomgaHarness.READLIST_URL)
        harness.api.updateProgress(track).status shouldBe Komga.COMPLETED
        val put = harness.takeRequest()
        put.url.encodedPath shouldBe "/api/v1/readlists/readlist-1/read-progress/tachiyomi"
        put.bodyText() shouldBe """{"lastBookRead":1}"""

        harness.enqueueRaw("", code = 500)
        shouldThrow<HttpException> { harness.api.updateProgress(track) }.code shouldBe 500
    }
}
