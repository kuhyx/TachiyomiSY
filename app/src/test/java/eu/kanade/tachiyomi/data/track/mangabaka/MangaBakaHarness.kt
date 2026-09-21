package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.jsonResponse
import eu.kanade.tachiyomi.data.track.mangabaka.dto.mangaBakaOAuth
import eu.kanade.tachiyomi.data.track.mockServerClient
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import kotlin.time.Clock

/** A [MangaBaka] tracker and its [MangaBakaApi] wired to a [MockWebServer] through Koin. */
internal class MangaBakaHarness {
    val server: MockWebServer = MockWebServer()
    val koin: TrackKoin = TrackKoin(mockServerClient(server))
    lateinit var tracker: MangaBaka
    lateinit var api: MangaBakaApi

    fun start(loggedIn: Boolean = true) {
        server.start()
        koin.start()
        tracker = MangaBaka(TRACK_ID)
        if (loggedIn) {
            tracker.saveToken(mangaBakaOAuth(expiresAt = Clock.System.now().epochSeconds + 7200))
        }
        api = MangaBakaApi(TRACK_ID, koin.networkHelper.client, MangaBakaInterceptor(tracker))
    }

    fun stop() {
        koin.stop()
        server.close()
    }

    fun enqueue(fixtureName: String, code: Int = 200) {
        server.enqueue(jsonResponse(fixture("eu/kanade/tachiyomi/data/track/mangabaka/$fixtureName"), code))
    }

    fun enqueueRaw(body: String, code: Int = 200) {
        server.enqueue(jsonResponse(body, code))
    }

    fun takeRequest(): RecordedRequest = server.takeRequest()

    /** A library entry in [state]; the rest of the entry is unset. */
    fun enqueueEntry(state: String) {
        enqueueRaw(
            """{"data":{"state":"$state","start_date":null,"finish_date":null,"is_private":false,""" +
                """"progress_chapter":null,"rating":null}}""",
        )
    }

    fun enqueueProfile(ratingSteps: Int, nickname: String? = null, preferred: String? = null) {
        val nick = nickname?.let { "\"$it\"" } ?: "null"
        val pref = preferred?.let { "\"$it\"" } ?: "null"
        enqueueRaw(
            """{"data":{"id":"uid","rating_steps":$ratingSteps,"nickname":$nick,"preferred_username":$pref}}""",
        )
    }

    companion object {
        const val TRACK_ID = 11L
    }
}
