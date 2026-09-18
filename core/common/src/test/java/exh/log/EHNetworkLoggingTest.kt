package exh.log

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.elvishew.xlog.LogLevel as XLogLevel

private const val TAG = "||EH-NETWORK-JSON"
private const val BODY = """{"ok":true}"""

/** Decodes any input as a [JsonElement]; only so `decodeFromString<Any>` has a serializer to find. */
private object AnyAsJsonElement : KSerializer<Any> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Any = JsonElement.serializer().deserialize(decoder)

    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeString(value.toString())
    }
}

internal class EHNetworkLoggingTest {
    private lateinit var printer: RecordingPrinter

    @BeforeEach
    fun setUp() {
        printer = installRecordingXLog()
    }

    @AfterEach
    fun tearDown() {
        setCurrentLogLevel(null)
        unmockkAll()
    }

    private fun loggedMessages(): List<String> {
        val builder = OkHttpClient.Builder().maybeInjectEHLogger()
        val interceptor = builder.interceptors().single().shouldBeInstanceOf<HttpLoggingInterceptor>()
        interceptor.level shouldBe HttpLoggingInterceptor.Level.BODY

        val request = Request.Builder().url("https://example.org/api").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(BODY.toResponseBody("application/json".toMediaType()))
            .build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.connection() } returns null
        every { chain.proceed(request) } returns response
        interceptor.intercept(chain).close()

        printer.lines.forEach { it.tag shouldBe TAG }
        return printer.lines.map { it.message }
    }

    @Test
    fun belowExtremeAddsNothing() {
        setCurrentLogLevel(EHLogLevel.EXTRA.ordinal)
        val builder = OkHttpClient.Builder()
        builder.maybeInjectEHLogger() shouldBe builder
        builder.interceptors().shouldBeEmpty()
    }

    @Test
    fun extremeLogsEveryLineAsDebug() {
        setCurrentLogLevel(EHLogLevel.EXTREME.ordinal)
        val messages = loggedMessages()
        messages shouldContain "--> GET https://example.org/api"
        messages shouldContain BODY
        printer.lines.map { it.level }.distinct() shouldBe listOf(XLogLevel.DEBUG)
    }

    @Test
    fun extremeLogsJsonBodiesAsJson() {
        setCurrentLogLevel(EHLogLevel.EXTREME.ordinal)
        mockkObject(Json)
        every { Json.serializersModule } returns SerializersModule { contextual(Any::class, AnyAsJsonElement) }
        val messages = loggedMessages()
        messages shouldContain "json:$BODY"
        messages shouldContain "--> GET https://example.org/api"
    }
}
