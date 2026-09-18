package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import okhttp3.Response
import org.junit.jupiter.api.Test
import java.io.IOException

internal class UncaughtExceptionInterceptorTest {
    private val request = GET(TEST_URL)

    @Test
    fun passesResponsesThrough() {
        val server = CannedServer { cannedResponse(it, code = 204) }
        execute(server).use { it.code shouldBe 204 }
    }

    @Test
    fun rethrowsIoExceptionsUnchanged() {
        val failure = IOException("socket closed")
        val server = CannedServer { throw failure }
        val error = shouldThrow<IOException> { execute(server) }
        error shouldBeSameInstanceAs failure
    }

    @Test
    fun wrapsOtherExceptionsAsIo() {
        val failure = IllegalStateException("parser blew up")
        val server = CannedServer { throw failure }
        val error = shouldThrow<IOException> { execute(server) }
        error.cause.shouldBeInstanceOf<IllegalStateException>() shouldBeSameInstanceAs failure
    }

    private fun execute(server: CannedServer): Response =
        clientOf(server, UncaughtExceptionInterceptor()).newCall(request).execute()
}
