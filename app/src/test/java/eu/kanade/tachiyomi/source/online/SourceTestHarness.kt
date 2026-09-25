package eu.kanade.tachiyomi.source.online

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import exh.log.EHLogLevel
import exh.pref.DelegateSourcePreferences
import exh.source.ExhPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.Type

/**
 * Everything an in-app source pulls from [Injekt], served from a [MockWebServer] and in-memory
 * preferences, for Robolectric tests. Every request the sources make is rewritten to [server]
 * (path, query and headers kept) so hard-coded base URLs never leave the JVM; the original
 * requests are recorded in [requests] and stay on the responses. [install] swaps the global scope
 * in; [uninstall] restores it.
 */
internal class SourceTestHarness {
    val server: MockWebServer = MockWebServer()
    val requests: MutableList<Request> = mutableListOf()
    val store: MemoPreferenceStore = sharedStore
    val exhPreferences: ExhPreferences = ExhPreferences(store)
    val networkPreferences: NetworkPreferences = NetworkPreferences(store)
    val delegatePreferences: DelegateSourcePreferences = DelegateSourcePreferences(store)
    val application: Application = ApplicationProvider.getApplicationContext()
    val services: MutableMap<Type, Any> = mutableMapOf()

    /** The origin the server answers on, without a trailing slash. */
    val baseUrl: String get() = server.url("/").toString().removeSuffix("/")

    /** Sends every request to [server], whatever host the source asked for. */
    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                requests += request
                val target = server.url("/")
                val url = request.url.newBuilder()
                    .scheme(target.scheme)
                    .host(target.host)
                    .port(target.port)
                    .build()
                // The response keeps the original request so `asJsoup()` sees the real location.
                chain.proceed(request.newBuilder().url(url).build()).newBuilder().request(request).build()
            },
        )
        .build()

    val networkHelper: NetworkHelper = networkHelperOf(client)

    private val registrar: InjektRegistrar = mockk {
        every { getInstance<Any>(any<Type>()) } answers { serve(firstArg()) }
        every { getInstanceOrNull<Any>(any<Type>()) } answers { services[firstArg()] }
    }
    private var previous: InjektScope? = null

    private fun serve(type: Type): Any = services[type] ?: when (type) {
        NetworkHelper::class.java -> networkHelper
        Application::class.java -> application
        NetworkPreferences::class.java -> networkPreferences
        DelegateSourcePreferences::class.java -> delegatePreferences
        ExhPreferences::class.java -> exhPreferences
        Json::class.java -> Json { ignoreUnknownKeys = true }
        else -> error("Injekt type not served by SourceTestHarness: $type")
    }

    /** Registers [instance] for [T] in the served graph. */
    inline fun <reified T : Any> serve(instance: T) {
        services[T::class.java] = instance
    }

    /** Starts the server, resets the shared preferences, initialises logging and swaps [Injekt] in. */
    fun install() {
        server.start()
        store.reset()
        networkPreferences.defaultUserAgent.set(USER_AGENT)
        installSilentXLog()
        EHLogLevel.init(application)
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    /** Restores the previous [Injekt] scope and stops the server. */
    fun uninstall() {
        previous?.let { Injekt = it }
        server.close()
    }

    /** Queues a response with [body] and [code] on the server. */
    fun enqueue(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    /** Queues [bytes] with [contentType] on the server. */
    fun enqueueBytes(bytes: ByteArray, contentType: String, code: Int = 200) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", contentType)
                .body(Buffer().write(bytes))
                .build(),
        )
    }

    /** Answers every request from [bodyFor] (path and query in, body out; null is a 404) instead of the queue. */
    fun answer(bodyFor: (String) -> String?) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = bodyFor(request.target)
                return if (body == null) {
                    MockResponse.Builder().code(404).build()
                } else {
                    MockResponse.Builder().code(200).body(body).build()
                }
            }
        }
    }

    /** The next request the server received: its target (path and query), headers and body. */
    fun takeRequest(): RecordedRequest = server.takeRequest()

    companion object {
        const val USER_AGENT: String = "app-test/1.0"

        /**
         * One store for the whole sandbox: statics such as the debug toggles' store resolve
         * it once through Injekt and keep it, so every harness must hand out the same object.
         */
        val sharedStore: MemoPreferenceStore = MemoPreferenceStore()
    }
}

/** A [NetworkHelper] whose only real part is [served]; the cookie jar is a relaxed mock. */
internal fun networkHelperOf(served: OkHttpClient): NetworkHelper = mockk {
    every { client } returns served
    every { isDebugBuild } returns false
    every { cookieJar } returns mockk(relaxed = true)
    every { defaultUserAgentProvider() } returns SourceTestHarness.USER_AGENT
}
