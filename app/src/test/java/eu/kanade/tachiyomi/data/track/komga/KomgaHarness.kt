package eu.kanade.tachiyomi.data.track.komga

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.jsonResponse
import eu.kanade.tachiyomi.data.track.mockServerClient
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest

/** A [Komga] tracker and its [KomgaApi] wired to a [MockWebServer] through Koin. */
internal class KomgaHarness {
    val server: MockWebServer = MockWebServer()
    val koin: TrackKoin = TrackKoin(mockServerClient(server))
    lateinit var tracker: Komga
    lateinit var api: KomgaApi

    fun start() {
        server.start()
        koin.start()
        tracker = Komga(TRACK_ID)
        api = KomgaApi(TRACK_ID, koin.networkHelper.client)
    }

    fun stop() {
        koin.stop()
        server.close()
    }

    fun enqueue(fixtureName: String, code: Int = 200) {
        server.enqueue(jsonResponse(fixture("eu/kanade/tachiyomi/data/track/komga/$fixtureName"), code))
    }

    fun enqueueRaw(body: String, code: Int = 200) {
        server.enqueue(jsonResponse(body, code))
    }

    fun takeRequest(): RecordedRequest = server.takeRequest()

    /** A progress body with the given book split; the read count is what is left. */
    fun enqueueProgress(booksCount: Int, unread: Int) {
        enqueueRaw(
            """{"booksCount":$booksCount,"booksReadCount":${booksCount - unread},"booksUnreadCount":$unread,""" +
                """"booksInProgressCount":0,"lastReadContinuousNumberSort":1.5,"maxNumberSort":$booksCount.0}""",
        )
    }

    companion object {
        const val TRACK_ID = 6L
        const val SERIES_URL = "http://komga.local/api/v1/series/series-1"
        const val READLIST_URL = "http://komga.local/api/v1/readlists/readlist-1"
    }
}
