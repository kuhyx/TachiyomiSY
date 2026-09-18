package eu.kanade.tachiyomi.network

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class OkHttpExtensionsProgressTest {
    private val json = Json
    private val listener = RecordingListener()
    private val cacheDir = Files.createTempDirectory("progress-cache").toFile()
    private val cache = Cache(cacheDir, 1024L)
    private val client = OkHttpClient.Builder().cache(cache).build()
    private val server = MockWebServer()

    @BeforeEach
    fun setUp() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
        cache.close()
        cacheDir.deleteRecursively()
    }

    @Test
    fun jsonMimeIsUtf8Json() {
        jsonMime.type shouldBe "application"
        jsonMime.subtype shouldBe "json"
        jsonMime.charset() shouldBe Charsets.UTF_8
    }

    @Test
    fun resumedDownloadAddsRangeHeader() {
        val response = drive(GET(server.url("/page").toString()), existingSize = 3L)
        server.takeRequest().headers["Range"] shouldBe "bytes=3-"
        response.body.shouldBeInstanceOf<ProgressResponseBody>().string() shouldBe "12345"
        listener.updates.last() shouldBe ProgressUpdate(bytesRead = 8L, contentLength = 5L, done = true)
    }

    @Test
    fun existingRangeHeaderIsKept() {
        val request = GET(server.url("/page").toString()).newBuilder().header("Range", "bytes=1-").build()
        drive(request, existingSize = 3L).close()
        server.takeRequest().headers["Range"] shouldBe "bytes=1-"
    }

    @Test
    fun freshDownloadSendsNoRange() {
        drive(GET(server.url("/page").toString()), existingSize = null).body.string() shouldBe "12345"
        server.takeRequest().headers["Range"] shouldBe null
        listener.updates.last() shouldBe ProgressUpdate(bytesRead = 5L, contentLength = 5L, done = true)
    }

    @Test
    fun progressCallBypassesTheCache() {
        // A cacheable response fetched through the progress call must leave the client's cache untouched.
        val request = GET(server.url("/page").toString())
        drive(request, existingSize = null, cacheable = true).body.string() shouldBe "12345"
        cache.requestCount() shouldBe 0
        cache.size() shouldBe 0L
        server.enqueue(MockResponse.Builder().body("12345").addHeader("Cache-Control", "max-age=60").build())
        client.newCall(request).execute().use { it.body.string() shouldBe "12345" }
        cache.requestCount() shouldBe 1
        cache.networkCount() shouldBe 1
    }

    @Test
    fun decodesBodyWithDeserializer() {
        val response = cannedResponse(GET(TEST_URL), body = "[1, 2, 3]")
        response.parseAs(json, ListSerializer(Int.serializer())) shouldBe listOf(1, 2, 3)
    }

    // Executes the progress call against the server, which answers with a five-byte body.
    private fun drive(request: Request, existingSize: Long?, cacheable: Boolean = false): Response {
        val response = MockResponse.Builder().body("12345")
        if (cacheable) response.addHeader("Cache-Control", "max-age=60")
        server.enqueue(response.build())
        val call = if (existingSize == null) {
            client.newCachelessCallWithProgress(request, listener)
        } else {
            client.newCachelessCallWithProgress(request, listener, existingSize)
        }
        return call.execute()
    }
}
