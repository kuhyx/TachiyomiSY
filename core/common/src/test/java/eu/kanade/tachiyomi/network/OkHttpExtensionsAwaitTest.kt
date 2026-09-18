package eu.kanade.tachiyomi.network

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.junit.jupiter.api.Test
import java.io.IOException

internal class OkHttpExtensionsAwaitTest {
    private val request = GET(TEST_URL)
    private val okCall: Call = mockk()
    private val callbacks = mutableListOf<Callback>()

    @Test
    fun awaitReturnsTheResponse() {
        val response = cannedResponse(request)
        every { okCall.enqueue(any()) } answers { firstArg<Callback>().onResponse(okCall, response) }
        runBlocking { okCall.await() } shouldBeSameInstanceAs response
    }

    @Test
    fun awaitWrapsFailuresWithCallSite() {
        val cause = IOException("boom")
        every { okCall.enqueue(any()) } answers { firstArg<Callback>().onFailure(okCall, cause) }
        val error = shouldThrow<IOException> { runBlocking { okCall.await() } }
        error.message shouldBe "boom"
        // Coroutine stack-trace recovery (on under -ea) copies the wrapper once more on the way out.
        generateSequence(error.cause) { it.cause }.last() shouldBeSameInstanceAs cause
        error.stackTrace.first().className shouldContain "OkHttpExtensionsAwaitTest"
    }

    @Test
    fun cancellationCancelsTheCall() {
        every { okCall.enqueue(capture(callbacks)) } just Runs
        every { okCall.cancel() } just Runs
        val body = ClosableBody()
        val response = cannedResponse(request).newBuilder().body(body).build()
        cancelWhileEnqueued()
        callbacks.single().onFailure(okCall, IOException("late"))
        callbacks.single().onResponse(okCall, response)
        verify { okCall.cancel() }
        body.isClosed shouldBe true
    }

    @Test
    fun cancellationIgnoresCancelError() {
        every { okCall.enqueue(capture(callbacks)) } just Runs
        every { okCall.cancel() } throws IllegalStateException("cancel failed")
        cancelWhileEnqueued()
        callbacks.single().onFailure(okCall, IOException("late"))
        verify { okCall.cancel() }
    }

    @Test
    fun awaitSuccessReturnsResponse() {
        val response = cannedResponse(request)
        every { okCall.enqueue(any()) } answers { firstArg<Callback>().onResponse(okCall, response) }
        runBlocking { okCall.awaitSuccess() } shouldBeSameInstanceAs response
    }

    @Test
    fun awaitSuccessThrowsOnErrorCode() {
        val body = ClosableBody()
        val response: Response = cannedResponse(request, code = 500).newBuilder().body(body).build()
        every { okCall.enqueue(any()) } answers { firstArg<Callback>().onResponse(okCall, response) }
        val error = shouldThrow<HttpException> { runBlocking { okCall.awaitSuccess() } }
        error.code shouldBe 500
        body.isClosed shouldBe true
        error.stackTrace.first().className shouldContain "OkHttpExtensionsAwaitTest"
    }

    // Suspends in await(), cancels the coroutine while the okCall is enqueued and lets it finish.
    private fun cancelWhileEnqueued() = runBlocking {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { okCall.await() }
        job.cancel()
        job.join()
    }
}
