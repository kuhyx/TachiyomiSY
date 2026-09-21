package eu.kanade.tachiyomi.data.track

import android.app.Application
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.LogcatLogger
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.InsertTrack
import java.io.File
import tachiyomi.domain.track.model.Track as DomainTrack

/**
 * Everything a tracker pulls out of Injekt, registered in Koin for one test. The preferences are
 * real (in-memory); the interactors, the source manager and the application are mockk stubs.
 */
internal class TrackKoin(client: OkHttpClient = OkHttpClient()) {
    val store: MapPreferenceStore = MapPreferenceStore()
    val trackPreferences: TrackPreferences = TrackPreferences(store)
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    val application: Application = mockk(relaxed = true)
    val addTracks: AddTracks = mockk()
    val insertTrack: InsertTrack = mockk()
    val sourceManager: SourceManager = mockk()
    val networkHelper: NetworkHelper = mockk<NetworkHelper>().also { every { it.client } returns client }

    /** Every message the code under test logged while this Koin was up. */
    val logged: MutableList<String> = mutableListOf()

    private val logger = object : LogcatLogger {
        override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

        override fun log(priority: LogPriority, tag: String, message: String) {
            logged += message
        }
    }

    fun start(vararg extra: Module) {
        LogcatLogger.install()
        LogcatLogger.loggers += logger
        startKoin {
            modules(
                module {
                    single { trackPreferences }
                    single { sourcePreferences }
                    single { json }
                    single { application }
                    single { addTracks }
                    single { insertTrack }
                    single { sourceManager }
                    single { networkHelper }
                },
                *extra,
            )
        }
    }

    fun stop() {
        stopKoin()
        LogcatLogger.loggers -= logger
        LogcatLogger.uninstall()
    }
}

/** A client whose every request is redirected to [server], keeping the path and query. */
internal fun mockServerClient(server: MockWebServer): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val base = server.url("/")
        val url = chain.request().url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        chain.proceed(chain.request().newBuilder().url(url).build())
    }
    .build()

internal fun jsonResponse(body: String, code: Int = 200): MockResponse = MockResponse(code = code, body = body)

/**
 * A fixture under `app/src/test/resources`. The classloader copy is Gradle's; the file fallback is
 * for the out-of-Gradle loop, which runs from `app/`.
 */
internal fun fixture(path: String): String {
    val resource = Thread.currentThread().contextClassLoader?.getResource(path)
    return resource?.readText() ?: File("src/test/resources/$path").readText()
}

internal fun dbTrack(
    trackerId: Long,
    remoteId: Long = 0,
    status: Long = 0,
    lastChapterRead: Double = 0.0,
    totalChapters: Long = 0,
    score: Double = 0.0,
    trackingUrl: String = "",
): Track = Track.create(trackerId).also {
    it.remoteId = remoteId
    it.status = status
    it.lastChapterRead = lastChapterRead
    it.totalChapters = totalChapters
    it.score = score
    it.trackingUrl = trackingUrl
}

internal fun RecordedRequest.bodyText(): String = body?.utf8().orEmpty()

internal fun domainTrack(
    id: Long = 1L,
    trackerId: Long = 42L,
    score: Double = 0.0,
    remoteUrl: String = "",
): DomainTrack = DomainTrack(
    id = id,
    mangaId = 10L,
    trackerId = trackerId,
    remoteId = 5L,
    libraryId = null,
    title = "Title",
    lastChapterRead = 0.0,
    totalChapters = 0L,
    status = 0L,
    score = score,
    remoteUrl = remoteUrl,
    startDate = 0L,
    finishDate = 0L,
    private = false,
)
