package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.hikka.dto.hikkaOAuth
import eu.kanade.tachiyomi.data.track.jsonResponse
import eu.kanade.tachiyomi.data.track.mockServerClient
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest

/** A [Hikka] tracker and its [HikkaApi] wired to a [MockWebServer] through Koin. */
internal class HikkaHarness {
    val server: MockWebServer = MockWebServer()
    val koin: TrackKoin = TrackKoin(mockServerClient(server))
    lateinit var tracker: Hikka
    lateinit var api: HikkaApi

    fun start(loggedIn: Boolean = true) {
        server.start()
        koin.start()
        tracker = Hikka(TRACK_ID)
        if (loggedIn) {
            tracker.saveOAuth(hikkaOAuth(expiration = System.currentTimeMillis() / 1000 + 7200))
        }
        api = HikkaApi(TRACK_ID, koin.networkHelper.client, HikkaInterceptor(tracker))
    }

    fun stop() {
        koin.stop()
        server.close()
    }

    fun enqueue(fixtureName: String, code: Int = 200) {
        server.enqueue(jsonResponse(fixture("eu/kanade/tachiyomi/data/track/hikka/$fixtureName"), code))
    }

    fun enqueueRaw(body: String, code: Int = 200) {
        server.enqueue(jsonResponse(body, code))
    }

    fun takeRequest(): RecordedRequest = server.takeRequest()

    companion object {
        const val TRACK_ID = 10L
        const val TRACKING_URL = "https://hikka.io/manga/test-manga-abc123"
    }
}
