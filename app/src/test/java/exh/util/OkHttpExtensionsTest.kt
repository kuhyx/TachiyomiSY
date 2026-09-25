package exh.util

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.Response
import org.junit.jupiter.api.Test
import rx.Subscriber
import java.io.IOException

internal class OkHttpExtensionsTest {
    private val response = mockk<Response>()
    private val call = mockk<Call>(relaxed = true)

    private class Collecting(private val requestZeroFirst: Boolean = false) : Subscriber<Pair<Exception, Response>>() {
        var next: Pair<Exception, Response>? = null
        var error: Throwable? = null
        var completed = false

        override fun onStart() {
            if (requestZeroFirst) request(0)
        }

        fun requestMore(n: Long) = request(n)

        override fun onNext(t: Pair<Exception, Response>) {
            next = t
        }

        override fun onError(e: Throwable) {
            error = e
        }

        override fun onCompleted() {
            completed = true
        }
    }

    private fun stubCall(vararg answers: () -> Response) {
        every { call.clone() } returns call
        every { call.execute() } answers { answers.first()() }
    }

    @Test
    fun deliversResponseWithStacktrace() {
        stubCall({ response })
        val subscriber = Collecting()
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.next!!.second shouldBeSameInstanceAs response
        subscriber.next!!.first.message shouldBe "Async stacktrace"
        subscriber.completed.shouldBeTrue()
        subscriber.error.shouldBeNull()
        verify(exactly = 0) { call.cancel() }
    }

    @Test
    fun failureCarriesAsyncCause() {
        stubCall({ throw IOException("offline") })
        val subscriber = Collecting()
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.next.shouldBeNull()
        val error = subscriber.error.shouldBeInstanceOf<IOException>()
        error.message shouldBe "offline"
        error.cause!!.message shouldBe "Async stacktrace"
    }

    @Test
    fun zeroRequestWaitsForDemand() {
        stubCall({ response })
        val subscriber = Collecting(requestZeroFirst = true)
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.next.shouldBeNull()
        subscriber.requestMore(1)
        subscriber.next!!.second shouldBeSameInstanceAs response
        // A second request is ignored: the call is one-shot.
        subscriber.requestMore(1)
        verify(exactly = 1) { call.execute() }
    }

    @Test
    fun unsubscribedInExecuteIsSilent() {
        val subscriber = Collecting()
        stubCall({
            subscriber.unsubscribe()
            response
        })
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.next.shouldBeNull()
        subscriber.completed.shouldBeFalse()
        verify(exactly = 1) { call.cancel() }
    }

    @Test
    fun unsubscribedInFailureIsSilent() {
        val subscriber = Collecting()
        stubCall({
            subscriber.unsubscribe()
            throw IOException("offline")
        })
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.error.shouldBeNull()
    }

    @Test
    fun lateUnsubscribeDoesNotCancel() {
        stubCall({ response })
        every { call.isCanceled() } returns true
        val subscriber = Collecting()
        call.asObservableWithStacktrace().subscribe(subscriber)
        subscriber.unsubscribe()
        verify(exactly = 0) { call.cancel() }
        subscriber.isUnsubscribed.shouldBeTrue()
    }
}
