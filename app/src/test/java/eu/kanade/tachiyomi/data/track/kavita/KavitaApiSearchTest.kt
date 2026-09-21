package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [KavitaApi.getTrackSearch] and [KavitaApi.updateProgress]: series, volumes and latest chapter. */
internal class KavitaApiSearchTest {

    private val harness = KavitaHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun enqueueSeries(pagesRead: Int, volumes: String = "volumes.json") {
        harness.enqueueRaw(
            """{"id":12,"name":"Kavita Series","thumbnail_url":"https://kavita.local/thumb.jpg",""" +
                """"pages":300,"pagesRead":$pagesRead,"format":1,"libraryId":3}""",
        )
        harness.enqueue(volumes)
    }

    private fun volume(chapters: String): String =
        """[{"id":1,"number":1,"name":"","pages":0,"pagesRead":0,"lastModified":"","created":"",""" +
            """"seriesId":12,"chapters":[$chapters]}]"""

    @Test
    fun searchAssemblesTheTrack() = runTest {
        enqueueSeries(pagesRead = 150)
        harness.enqueue("chapter.json")
        val track = harness.api.getTrackSearch(KavitaHarness.SERIES_URL)
        track.title shouldBe "Kavita Series"
        track.coverUrl shouldBe "https://kavita.local/thumb.jpg"
        track.trackingUrl shouldBe KavitaHarness.SERIES_URL
        track.totalChapters shouldBe 7L
        track.status shouldBe Kavita.READING
        track.lastChapterRead shouldBe 7.5

        val series = harness.takeRequest()
        series.url.encodedPath shouldBe "/api/Series/12"
        series.headers["Authorization"] shouldBe "Bearer jwt"
        series.headers["User-Agent"].orEmpty().startsWith("TachiyomiSY v") shouldBe true
        val volumes = harness.takeRequest()
        volumes.url.encodedPath shouldBe "/api/Series/volumes"
        volumes.url.queryParameter("seriesId") shouldBe "12"
        val latest = harness.takeRequest()
        latest.url.encodedPath shouldBe "/api/Tachiyomi/latest-chapter"
        latest.url.queryParameter("seriesId") shouldBe "12"
    }

    @Test
    fun statusFollowsPagesRead() = runTest {
        enqueueSeries(pagesRead = 300)
        harness.enqueueRaw("", code = 204)
        val done = harness.api.getTrackSearch(KavitaHarness.SERIES_URL)
        done.status shouldBe Kavita.COMPLETED
        done.lastChapterRead shouldBe 0.0

        enqueueSeries(pagesRead = 0)
        harness.enqueueRaw("", code = 204)
        harness.api.getTrackSearch(KavitaHarness.SERIES_URL).status shouldBe Kavita.UNREAD
    }

    @Test
    fun volumesWithoutChaptersCount() = runTest {
        enqueueSeries(pagesRead = 1, volumes = "volumes_only.json")
        harness.enqueueRaw("", code = 204)
        harness.api.getTrackSearch(KavitaHarness.SERIES_URL).totalChapters shouldBe 2L
    }

    @Test
    fun volumeErrorsPropagate() = runTest {
        harness.enqueue("series.json")
        harness.enqueueRaw(volume("""{"id":1,"number":null}"""))
        shouldThrow<NullPointerException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }

        harness.enqueue("series.json")
        harness.enqueueRaw("not json", code = 500)
        shouldThrow<SerializationException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }

        harness.enqueue("series.json")
        harness.enqueueRaw(volume("""{"id":1,"number":"2"},{"id":2,"number":null}"""))
        shouldThrow<NullPointerException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }

        harness.enqueue("series.json")
        harness.enqueueRaw(volume(""))
        shouldThrow<NoSuchElementException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }
        harness.koin.logged.any { it.startsWith("Exception fetching Total Chapters.") } shouldBe true
        harness.koin.logged.last().startsWith("Could not get item: ") shouldBe true
    }

    @Test
    fun latestChapterErrorsPropagate() = runTest {
        enqueueSeries(pagesRead = 1)
        harness.enqueueRaw("""{"number":null}""")
        shouldThrow<NullPointerException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }
    }

    @Test
    fun seriesErrorsPropagate() = runTest {
        harness.enqueueRaw("", code = 404)
        shouldThrow<HttpException> { harness.api.getTrackSearch(KavitaHarness.SERIES_URL) }.code shouldBe 404
    }

    @Test
    fun updateMarksThenRefreshes() = runTest {
        harness.enqueueRaw("")
        enqueueSeries(pagesRead = 150)
        harness.enqueue("chapter.json")
        val track = dbTrack(trackerId = 8L, lastChapterRead = 5.0, trackingUrl = KavitaHarness.SERIES_URL)
        val refreshed = harness.api.updateProgress(track)
        refreshed.lastChapterRead shouldBe 7.5
        val mark = harness.takeRequest()
        mark.method shouldBe "POST"
        mark.url.encodedPath shouldBe "/api/Tachiyomi/mark-chapter-until-as-read"
        mark.url.queryParameter("seriesId") shouldBe "12"
        mark.url.queryParameter("chapterNumber") shouldBe "5.0"

        harness.enqueueRaw("", code = 500)
        shouldThrow<HttpException> { harness.api.updateProgress(track) }.code shouldBe 500
    }
}
