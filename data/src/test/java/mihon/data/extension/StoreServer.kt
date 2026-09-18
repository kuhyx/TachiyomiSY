package mihon.data.extension

import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.service.ExtensionStoreService
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.OkHttpClient
import okio.Buffer
import okio.GzipSink
import okio.buffer
import kotlin.coroutines.cancellation.CancellationException

/** A local HTTP server plus an [ExtensionStoreService] pointed at it. */
internal class StoreServer : AutoCloseable {
    private val server = MockWebServer().apply { start() }

    /** The service's JSON: unknown keys are ignored and absent nullable fields are null. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** A service whose HTTP client is a plain [OkHttpClient]. */
    val service: ExtensionStoreService = ExtensionStoreService(
        network = mockk<NetworkHelper> { every { client } returns OkHttpClient() },
        json = json,
        protoBuf = ProtoBuf,
    )

    /** The absolute url of [path] on this server. */
    fun url(path: String): String = server.url(path).toString()

    /** Queues a text response. */
    fun enqueue(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    /** Queues a binary response. */
    fun enqueue(body: Buffer) {
        server.enqueue(MockResponse.Builder().code(200).body(body).build())
    }

    /** Queues a protobuf-encoded store. */
    fun enqueueProto(store: NetworkExtensionStore) {
        enqueue(Buffer().write(ProtoBuf.encodeToByteArray(NetworkExtensionStore.serializer(), store)))
    }

    /** Queues a JSON-encoded store. */
    fun enqueueJson(store: NetworkExtensionStore) {
        enqueue(json.encodeToString(NetworkExtensionStore.serializer(), store))
    }

    /** The path of the next request the server received. */
    fun takePath(): String = server.takeRequest().url.encodedPath

    override fun close() {
        server.close()
    }
}

/** [text] compressed with gzip, as a response body. */
internal fun gzipped(text: String): Buffer = Buffer().also { sink ->
    GzipSink(sink).buffer().use { it.writeUtf8(text) }
}

/** A service whose every HTTP call throws [CancellationException] instead of running. */
internal fun cancellingService(json: Json): ExtensionStoreService {
    val call = mockk<Call> { every { enqueue(any()) } throws CancellationException("cancelled") }
    val httpClient = mockk<OkHttpClient> { every { newCall(any()) } returns call }
    return ExtensionStoreService(
        network = mockk<NetworkHelper> { every { client } returns httpClient },
        json = json,
        protoBuf = ProtoBuf,
    )
}

/** One legacy `index.min.json` entry as the stores serve it. */
internal const val LEGACY_INDEX_JSON: String =
    """[{"name":"Tachiyomi: Demo","pkg":"eu.kanade.tachiyomi.extension.en.demo","apk":"demo.apk","lang":"en",""" +
        """"code":3,"version":"1.4.3","nsfw":1}]"""

/** A legacy `repo.json` whose `index_v2` is [indexV2] (omitted when null). */
internal fun legacyRepoJson(indexV2: String? = null): String {
    val pointer = if (indexV2 == null) "" else """"index_v2":"$indexV2","""
    return """{$pointer"meta":{"name":"Legacy","shortName":"LEG","website":"https://legacy.example",""" +
        """"signingKeyFingerprint":"FFFF"}}"""
}
