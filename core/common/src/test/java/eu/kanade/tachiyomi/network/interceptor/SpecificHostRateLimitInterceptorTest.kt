package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.invokeStatic
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.unmockkAll
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

internal class SpecificHostRateLimitInterceptorTest {
    private val apiUrl = "https://api.example.com/v1".toHttpUrl()
    private val server = CannedServer { cannedResponse(it) }
    private lateinit var clock: ScriptedClock

    @BeforeEach
    fun setUp() {
        clock = installClock()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun urlBuilderLimitsOnlyThatHost() {
        val client = OkHttpClient.Builder().rateLimitHost(apiUrl, 2).addInterceptor(server).build()
        client.newCall(GET("https://cdn.example.com/img")).execute().close()
        clock.reads shouldBe 0
        client.newCall(GET("https://api.example.com/v2")).execute().close()
        clock.reads shouldBe 1
    }

    @Test
    fun urlBuilderAcceptsAPeriod() {
        val builder = OkHttpClient.Builder().rateLimitHost(apiUrl, 2, 5.seconds)
        builder.interceptors().single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun stringBuilderLimitsOnlyHost() {
        val client = OkHttpClient.Builder().rateLimitHost(apiUrl.toString(), 2).addInterceptor(server).build()
        client.newCall(GET("https://cdn.example.com/img")).execute().close()
        clock.reads shouldBe 0
        client.newCall(GET("https://api.example.com/v2")).execute().close()
        clock.reads shouldBe 1
    }

    @Test
    fun stringBuilderAcceptsAPeriod() {
        val builder = OkHttpClient.Builder().rateLimitHost("https://api.example.com/v1", 2, 5.seconds)
        builder.interceptors().single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun unparseableUrlLimitsEveryHost() {
        val client = OkHttpClient.Builder().rateLimitHost("not a url", 2).addInterceptor(server).build()
        client.newCall(GET("https://cdn.example.com/img")).execute().close()
        clock.reads shouldBe 1
    }

    @Test
    fun legacyDefaultsToOneSecond() {
        // The compiler's `$default` bridge fills period and unit (mask bits 3 and 4) when omitted.
        val builder = OkHttpClient.Builder()
        invokeStatic(HOST_RATE_LIMIT_KT, "rateLimitHost\$default", listOf(builder, apiUrl, 2, 0L, null, 0b1100, null))
        builder.interceptors().single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun legacyBuilderLimitsOnlyHost() {
        // Deprecated overload: reached reflectively so no deprecation warning is compiled.
        val builder = OkHttpClient.Builder()
        val result = invokeStatic(HOST_RATE_LIMIT_KT, "rateLimitHost", listOf(builder, apiUrl, 2, 1L, TimeUnit.SECONDS))
        result shouldBeSameInstanceAs builder
        val client = builder.addInterceptor(server).build()
        client.newCall(GET("https://cdn.example.com/img")).execute().close()
        clock.reads shouldBe 0
        client.newCall(GET("https://api.example.com/v2")).execute().close()
        clock.reads shouldBe 1
    }
}
