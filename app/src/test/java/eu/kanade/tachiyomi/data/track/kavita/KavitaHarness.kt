package eu.kanade.tachiyomi.data.track.kavita

import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.jsonResponse
import eu.kanade.tachiyomi.data.track.mockServerClient
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.sourceIdOf
import io.mockk.every
import io.mockk.mockk
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest

/** A [Kavita] tracker and its [KavitaApi] wired to a [MockWebServer] through Koin. */
internal class KavitaHarness {
    val server: MockWebServer = MockWebServer()
    val koin: TrackKoin = TrackKoin(mockServerClient(server))
    lateinit var tracker: Kavita
    lateinit var api: KavitaApi

    fun start(authenticated: Boolean = true) {
        server.start()
        koin.start()
        tracker = Kavita(TRACK_ID)
        if (authenticated) {
            // The interceptor sees the request after it was pointed at the mock server.
            val apiUrl = server.url("/api").toString()
            tracker.authentications = OAuth(listOf(SourceAuth(sourceId = 1, apiUrl = apiUrl, jwtToken = "jwt")))
        }
        api = KavitaApi(koin.networkHelper.client, KavitaInterceptor(tracker))
    }

    fun stop() {
        koin.stop()
        server.close()
    }

    fun enqueue(fixtureName: String, code: Int = 200) {
        server.enqueue(jsonResponse(fixture("eu/kanade/tachiyomi/data/track/kavita/$fixtureName"), code))
    }

    fun enqueueRaw(body: String, code: Int = 200) {
        server.enqueue(jsonResponse(body, code))
    }

    fun takeRequest(): RecordedRequest = server.takeRequest()

    /** The extension's three sources, with the given `APIURL`/`APIKEY` preference pairs. */
    fun configureSources(vararg settings: Pair<String?, String?>) {
        settings.forEachIndexed { index, (apiUrl, apiKey) ->
            val sourceId = sourceIdOf(name = "kavita_${index + 1}", lang = "all", versionId = 1)
            val source = mockk<ConfigurableSource> { every { id } returns sourceId }
            val preferences = mockk<SharedPreferences> {
                every { getString("APIURL", "") } returns apiUrl
                every { getString("APIKEY", "") } returns apiKey
            }
            every { koin.sourceManager.get(sourceId) } returns source
            every { koin.application.getSharedPreferences("source_$sourceId", 0) } returns preferences
        }
    }

    companion object {
        const val TRACK_ID = 8L
        const val API_URL = "http://kavita.local/api"
        const val SERIES_URL = "http://kavita.local/api/Series/12"
    }
}
