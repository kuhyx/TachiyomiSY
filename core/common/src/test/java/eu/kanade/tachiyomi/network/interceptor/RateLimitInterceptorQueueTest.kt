package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.network.networkResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import okhttp3.Call
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/** Queue bookkeeping that only shows when a second call runs while the first still holds its slot. */
internal class RateLimitInterceptorQueueTest {
    private val request = GET(TEST_URL)
    private val requestA = request.newBuilder().header("X-Turn", "A").build()

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun interruptedWaitRechecksQueue() {
        val clock = installClock(0, 50, 200, 200)
        clock.onRead(2) { Thread.currentThread().interrupt() }
        val server = CannedServer { networkResponse(it) }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 100.milliseconds))
        client.newCall(request).execute().close()
        client.newCall(request).execute().close()
        clock.reads shouldBe 4
        Thread.interrupted() shouldBe false
    }

    @Test
    fun cancelledWaiterEmptiesQueue() {
        // B runs inside A's server turn: it expires A's slot, is cancelled while checking the clock and
        // fails, so A's cached response finds nothing left to remove.
        val clock = installClock(0, 200)
        val callB = AtomicReference<Call>()
        val server = CannedServer { req ->
            if (req.header("X-Turn") == "A") {
                shouldThrow<IOException> { callB.get().execute() }.message shouldBe "Canceled"
            }
            cannedResponse(req)
        }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 100.milliseconds))
        callB.set(client.newCall(request))
        clock.onRead(2) { callB.get().cancel() }
        client.newCall(requestA).execute().close()
        clock.reads shouldBe 2
        server.requests.size shouldBe 1
    }

    @Test
    fun cachedResponseKeepsNewerSlot() {
        // B takes a fresh slot while A is in flight; A's cached response must not remove B's newer slot.
        val clock = installClock(0, 200, 200)
        val callB = AtomicReference<Call>()
        val server = CannedServer { req ->
            if (req.header("X-Turn") == "A") {
                callB.get().execute().close()
                cannedResponse(req)
            } else {
                networkResponse(req)
            }
        }
        val client = clientOf(server, RateLimitInterceptor(null, 1, 100.milliseconds))
        callB.set(client.newCall(request))
        client.newCall(requestA).execute().close()
        clock.reads shouldBe 3
        server.requests.size shouldBe 2
    }
}
