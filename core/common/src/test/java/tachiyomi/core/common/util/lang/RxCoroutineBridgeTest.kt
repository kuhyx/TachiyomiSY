package tachiyomi.core.common.util.lang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import rx.Emitter
import rx.Observable
import rx.Subscriber

internal class RxCoroutineBridgeTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // An observable whose single() is replaced, so the test drives the subscriber by hand.
    private fun withSingle(single: Observable<Int>): Observable<Int> {
        val source = mockk<Observable<Int>>()
        every { source.single() } returns single
        return source
    }

    @Test
    fun awaitSingleReturnsTheOnlyValue() {
        runBlocking { Observable.just(4).awaitSingle() } shouldBe 4
    }

    @Test
    fun awaitSingleRethrowsErrors() {
        val error = IllegalArgumentException("nope")
        shouldThrow<IllegalArgumentException> {
            runBlocking { Observable.error<Int>(error).awaitSingle() }
        } shouldBe error
    }

    @Test
    fun awaitSingleFailsOnEmpty() {
        shouldThrow<NoSuchElementException> {
            runBlocking { Observable.empty<Int>().awaitSingle() }
        }
    }

    @Test
    fun awaitSingleFailsWithoutOnNext() {
        val source = withSingle(Observable.unsafeCreate { it.onCompleted() })
        shouldThrow<IllegalStateException> {
            runBlocking { source.awaitSingle() }
        }.message shouldBe "Should have invoked onNext"
    }

    @Test
    fun cancellationUnsubscribes() {
        var subscriber: Subscriber<in Int>? = null
        val source = withSingle(Observable.unsafeCreate { subscriber = it })
        runBlocking {
            val job = launch { source.awaitSingle() }
            yield()
            subscriber?.isUnsubscribed shouldBe false
            job.cancel()
            job.join()
        }
        subscriber?.isUnsubscribed shouldBe true
    }

    @Test
    fun lateErrorAfterCancelIsIgnored() {
        var subscriber: Subscriber<in Int>? = null
        val source = withSingle(Observable.unsafeCreate { subscriber = it })
        runBlocking {
            val job = launch { source.awaitSingle() }
            yield()
            job.cancel()
            job.join()
        }
        subscriber?.onError(IllegalStateException("late"))
        subscriber?.isUnsubscribed shouldBe true
    }

    @Test
    fun runAsObservableEmitsResult() {
        runAsObservable { 5 }.toBlocking().single() shouldBe 5
        runAsObservable(Emitter.BackpressureMode.BUFFER) { "x" }.toBlocking().single() shouldBe "x"
    }

    @Test
    fun runAsObservableForwardsErrors() {
        val error = IllegalArgumentException("boom")
        shouldThrow<IllegalArgumentException> {
            runAsObservable<Int> { throw error }.toBlocking().single()
        } shouldBe error
    }

    @Test
    fun unsubscribeCancelsTheBlock() {
        var isCancelled = false
        val subscription = runAsObservable {
            try {
                awaitCancellation()
            } finally {
                isCancelled = true
            }
        }.subscribe()
        isCancelled shouldBe false
        subscription.unsubscribe()
        isCancelled shouldBe true
    }
}
