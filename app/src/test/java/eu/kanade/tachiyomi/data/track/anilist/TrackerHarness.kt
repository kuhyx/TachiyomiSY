package eu.kanade.tachiyomi.data.track.anilist

import android.app.Application
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.jsonMime
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.track.interactor.InsertTrack
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import tachiyomi.domain.track.model.Track as DomainTrack

/** Shared test wiring for the tracker packages: one Koin graph, one preference store, a scripted HTTP layer. */
internal object TrackerHarness {
    val store: MapPreferenceStore = MapPreferenceStore()

    // One instance for the JVM: AnilistUtils.kt caches TrackPreferences in a top-level lazy.
    val trackPreferences: TrackPreferences = TrackPreferences(store)

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Restarts Koin with a clean store; [client] is what every tracker sees as `networkService.client`. */
    fun start(client: OkHttpClient = OkHttpClient()) {
        store.clear()
        stopKoin()
        val network = mockk<NetworkHelper>()
        every { network.client } returns client
        startKoin {
            modules(
                module {
                    single { trackPreferences }
                    single { network }
                    single { json }
                    single<AddTracks> { mockk() }
                    single<InsertTrack> { mockk() }
                    single<Application> { mockk() }
                },
            )
        }
    }

    fun stop() {
        stopKoin()
    }
}

/**
 * Walks a decoded DTO graph reading every property getter and calling the data-class members, so the
 * trivial generated code of the tracker DTOs counts as executed; nested tracker DTOs and lists recurse.
 */
internal fun exerciseDto(value: Any?) {
    when (value) {
        null -> return
        is Iterable<*> -> value.forEach(::exerciseDto)
        is Map<*, *> -> value.values.forEach(::exerciseDto)
        else -> if (value.javaClass.name.startsWith("eu.kanade.tachiyomi.data.track")) {
            value.toString()
            value.hashCode()
            check(value == value)
            value::class.memberProperties.forEach { property ->
                property.isAccessible = true
                exerciseDto(property.getter.call(value))
            }
            // `copy()` with every default is the one call that reaches the primary constructor of a decoded DTO.
            value::class.memberFunctions.firstOrNull { it.name == "copy" }?.let { copy ->
                copy.isAccessible = true
                copy.callBy(mapOf(checkNotNull(copy.instanceParameter) to value))
            }
        }
    }
}

/**
 * Runs a suspending test body on the IO dispatcher, so `withIOContext` stays undispatched and a call the
 * [FakeServer] answers inline completes without ever suspending; the Unit result keeps JUnit 4 tests void.
 */
internal fun runSuspend(block: suspend () -> Unit) {
    runBlocking(Dispatchers.IO) { block() }
}

/**
 * Reads a fixture body from `src/test/resources/eu/kanade/tachiyomi/data/track/<pkg>/<name>`, whether the
 * test runs from the module directory (Gradle, the loop) or from the repository root.
 */
internal fun fixture(pkg: String, name: String): String {
    val relative = "src/test/resources/eu/kanade/tachiyomi/data/track/$pkg/$name"
    val file = File(relative).takeIf(File::exists) ?: File("app/$relative")
    return file.readText()
}

/** An application interceptor answering each call with the next scripted response and recording the request. */
internal class FakeServer : Interceptor {
    private val queue = ArrayDeque<(Request) -> Response>()
    val requests: MutableList<Request> = mutableListOf()
    val bodies: MutableList<String> = mutableListOf()
    val lastRequest: Request get() = requests.last()

    // Enqueued calls run on the caller's thread, so a scripted answer resumes the coroutine synchronously.
    val client: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(InlineExecutor()))
        .addInterceptor(this)
        .build()

    fun enqueue(code: Int, body: String = "", headers: Headers = Headers.headersOf()) {
        queue.add { request -> response(request, code, body, headers) }
    }

    fun enqueueFailure(error: Throwable) {
        queue.add { throw error }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requests += request
        bodies += request.body?.let { body -> Buffer().also(body::writeTo).readUtf8() }.orEmpty()
        val answer = checkNotNull(queue.poll()) { "no scripted response for ${request.url}" }
        return answer(request)
    }
}

/** An executor that runs every task on the submitting thread. */
internal class InlineExecutor : AbstractExecutorService() {
    override fun execute(command: Runnable) = command.run()

    override fun shutdown() = Unit

    override fun shutdownNow(): MutableList<Runnable> = mutableListOf()

    override fun isShutdown(): Boolean = false

    override fun isTerminated(): Boolean = false

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}

internal fun response(request: Request, code: Int, body: String, headers: Headers = Headers.headersOf()): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("scripted")
        .headers(headers)
        .body(body.toResponseBody(jsonMime))
        .build()

internal fun dbTrack(
    trackerId: Long,
    remoteId: Long = 0L,
    libraryId: Long? = null,
    status: Long = 0L,
    lastChapterRead: Double = 0.0,
    totalChapters: Long = 0L,
    score: Double = 0.0,
): Track = Track.create(trackerId).apply {
    this.remoteId = remoteId
    this.libraryId = libraryId
    this.status = status
    this.lastChapterRead = lastChapterRead
    this.totalChapters = totalChapters
    this.score = score
}

internal fun domainTrack(
    trackerId: Long,
    remoteId: Long = 0L,
    libraryId: Long? = null,
    score: Double = 0.0,
    status: Long = 0L,
): DomainTrack = DomainTrack(
    id = 1L,
    mangaId = 2L,
    trackerId = trackerId,
    remoteId = remoteId,
    libraryId = libraryId,
    title = "title",
    lastChapterRead = 0.0,
    totalChapters = 0L,
    status = status,
    score = score,
    remoteUrl = "",
    startDate = 0L,
    finishDate = 0L,
    private = false,
)
