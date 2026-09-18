package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.network.invokeStatic
import eu.kanade.tachiyomi.network.networkResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.unmockkAll
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class RateLimitInterceptorTest {
    private val request = GET(TEST_URL)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun buildersInstallTheLimiter() {
        val defaultPeriod = OkHttpClient.Builder().rateLimit(3).interceptors()
        defaultPeriod.single().shouldBeInstanceOf<RateLimitInterceptor>()
        val explicitPeriod = OkHttpClient.Builder().rateLimit(3, 2.seconds).interceptors()
        explicitPeriod.single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun legacyDefaultsToOneSecond() {
        // The compiler's `$default` bridge fills period and unit (mask bits 2 and 3) when omitted.
        val builder = OkHttpClient.Builder()
        invokeStatic(RATE_LIMIT_KT, "rateLimit\$default", listOf(builder, 3, 0L, null, 0b110, null))
        builder.interceptors().single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun legacyBuilderInstallsLimiter() {
        // Deprecated overload: reached reflectively so no deprecation warning is compiled.
        val builder = OkHttpClient.Builder()
        val result = invokeStatic(RATE_LIMIT_KT, "rateLimit", listOf(builder, 3, 2L, TimeUnit.SECONDS))
        result shouldBeSameInstanceAs builder
        builder.interceptors().single().shouldBeInstanceOf<RateLimitInterceptor>()
    }

    @Test
    fun cancelledCallFailsUnlimited() {
        val clock = installClock()
        val server = CannedServer { cannedResponse(it) }
        val call = clientOf(server, RateLimitInterceptor(null, 1, 1.seconds)).newCall(request)
        call.cancel()
        shouldThrow<IOException> { call.execute() }.message shouldBe "Canceled"
        clock.reads shouldBe 0
        server.requests shouldBe emptyList()
    }

    @Test
    fun otherHostsAreNotLimited() {
        val clock = installClock()
        val server = CannedServer { cannedResponse(it) }
        val client = clientOf(server, RateLimitInterceptor("api.example.com", 1, 1.seconds))
        client.newCall(GET("https://cdn.example.com/a")).execute().close()
        clock.reads shouldBe 0
        client.newCall(GET("https://api.example.com/a")).execute().close()
        clock.reads shouldBe 1
        server.requests.size shouldBe 2
    }

    @Test
    fun cachedResponseReleasesItsSlot() {
        // A third reading of 500 would let a (wrong) wait finish instead of hanging the test.
        val clock = installClock(0, 0, 500)
        val server = CannedServer { cannedResponse(it) }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 100.milliseconds))
        client.newCall(request).execute().close()
        client.newCall(request).execute().close()
        clock.reads shouldBe 2
        server.requests.size shouldBe 2
    }

    @Test
    fun networkResponseKeepsItsSlot() {
        // The second call waits the 50 ms left in the period, then finds the first slot expired.
        val clock = installClock(0, 50, 200, 200)
        val server = CannedServer { networkResponse(it) }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 100.milliseconds))
        client.newCall(request).execute().close()
        client.newCall(request).execute().close()
        clock.reads shouldBe 4
        server.requests.size shouldBe 2
    }

    @Test
    fun interruptedAcquireFailsAsIo() {
        installClock()
        val server = CannedServer { cannedResponse(it) }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 1.seconds))
        Thread.currentThread().interrupt()
        val error = shouldThrow<IOException> { client.newCall(request).execute() }
        error.cause.shouldBeInstanceOf<InterruptedException>()
        Thread.interrupted() shouldBe false
        server.requests shouldBe emptyList()
    }
}
