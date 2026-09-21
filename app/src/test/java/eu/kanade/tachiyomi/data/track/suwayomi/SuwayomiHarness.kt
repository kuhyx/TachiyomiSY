package eu.kanade.tachiyomi.data.track.suwayomi

import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.jsonResponse
import eu.kanade.tachiyomi.data.track.mockServerClient
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.sourceIdOf
import io.mockk.every
import io.mockk.mockk
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest

/**
 * A [Suwayomi] tracker and its [SuwayomiApi] wired to a [MockWebServer]: the API borrows the
 * Tachidesk source's client and base URL, so that source is a mock handing out the server client.
 */
internal class SuwayomiHarness {
    val server: MockWebServer = MockWebServer()
    val koin: TrackKoin = TrackKoin(mockServerClient(server))
    val preferences: SharedPreferences = mockk()
    lateinit var tracker: Suwayomi
    lateinit var api: SuwayomiApi

    fun start(deleteDownloads: Boolean = false) {
        server.start()
        koin.start()
        val sourceId = sourceIdOf(name = "Tachidesk", lang = "en", versionId = 1)
        val source = mockk<HttpSource>(moreInterfaces = arrayOf(ConfigurableSource::class)) {
            every { id } returns sourceId
            every { client } returns koin.networkHelper.client
            every { baseUrl } returns BASE_URL
        }
        every { koin.sourceManager.get(sourceId) } returns source
        every { koin.application.getSharedPreferences("source_$sourceId", 0) } returns preferences
        every { preferences.getBoolean("Tracker Delete", false) } returns deleteDownloads
        tracker = Suwayomi(TRACK_ID)
        api = SuwayomiApi(TRACK_ID)
    }

    fun stop() {
        koin.stop()
        server.close()
    }

    fun enqueue(fixtureName: String, code: Int = 200) {
        server.enqueue(jsonResponse(fixture("eu/kanade/tachiyomi/data/track/suwayomi/$fixtureName"), code))
    }

    fun enqueueRaw(body: String, code: Int = 200) {
        server.enqueue(jsonResponse(body, code))
    }

    fun takeRequest(): RecordedRequest = server.takeRequest()

    companion object {
        const val TRACK_ID = 9L
        const val BASE_URL = "http://suwayomi.local/"
    }
}
