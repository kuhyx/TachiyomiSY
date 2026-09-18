package eu.kanade.tachiyomi.network

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable
import rx.Producer
import rx.Subscriber
import rx.Subscription
import java.io.IOException

internal class OkHttpExtensionsRxTest {
    private val request = GET(TEST_URL)
    private val call: Call = mockk()
    private val clone: Call = mockk()

    @BeforeEach
    fun setUp() {
        every { call.clone() } returns clone
        every { clone.cancel() } just Runs
        every { clone.isCanceled() } returns false
    }

    @Test
    fun emitsTheExecutedResponse() {
        val response = cannedResponse(request)
        every { clone.execute() } returns response
        first(asObservable()) shouldBeSameInstanceAs response
        verify(exactly = 1) { clone.execute() }
    }

    @Test
    fun errorsWhenExecuteFails() {
        every { clone.execute() } throws IOException("down")
        val error = shouldThrow<RuntimeException> { first(asObservable()) }
        error.cause.shouldBeInstanceOf<IOException>().message shouldBe "down"
    }

    @Test
    fun dropsResponseAfterUnsubscribe() {
        val subscriber = CollectingSubscriber()
        every { clone.execute() } answers {
            subscriber.unsubscribe()
            cannedResponse(request)
        }
        asObservable().subscribe(subscriber)
        subscriber.values shouldBe emptyList()
        subscriber.isCompleted shouldBe false
    }

    @Test
    fun dropsErrorAfterUnsubscribe() {
        val subscriber = CollectingSubscriber()
        every { clone.execute() } answers {
            subscriber.unsubscribe()
            throw IOException("down")
        }
        asObservable().subscribe(subscriber)
        subscriber.errors shouldBe emptyList()
    }

    @Test
    fun executesOnceDespiteRerequests() {
        val subscriber = CollectingSubscriber(initialRequest = 0)
        every { clone.execute() } returns cannedResponse(request)
        asObservable().subscribe(subscriber)
        verify(exactly = 0) { clone.execute() }
        subscriber.requestMore(1)
        subscriber.requestMore(1)
        verify(exactly = 1) { clone.execute() }
        subscriber.values.size shouldBe 1
        subscriber.isCompleted shouldBe true
    }

    @Test
    fun unsubscribeCancelsClonedCall() {
        val subscriber = CollectingSubscriber(initialRequest = 0)
        asObservable().subscribe(subscriber)
        val arbiter = subscriber.lastProducer.shouldBeInstanceOf<Subscription>()
        arbiter.isUnsubscribed shouldBe false
        subscriber.unsubscribe()
        verify { clone.cancel() }
        every { clone.isCanceled() } returns true
        arbiter.isUnsubscribed shouldBe true
    }

    @Test
    fun successVariantPassesResponse() {
        val response = cannedResponse(request)
        every { clone.execute() } returns response
        first(asObservableSuccess()) shouldBeSameInstanceAs response
    }

    @Test
    fun successVariantFailsOnErrorCode() {
        val body = ClosableBody()
        every { clone.execute() } returns cannedResponse(request, code = 404).newBuilder().body(body).build()
        val error = shouldThrow<HttpException> { first(asObservableSuccess()) }
        error.code shouldBe 404
        body.isClosed shouldBe true
    }

    // Both extensions are deprecated, so they are reached reflectively to keep the build warning-free.
    private fun asObservable(): Observable<*> =
        invokeStatic(EXTENSIONS, "asObservable", listOf(call)) as Observable<*>

    private fun asObservableSuccess(): Observable<*> =
        invokeStatic(EXTENSIONS, "asObservableSuccess", listOf(call)) as Observable<*>

    private fun first(observable: Observable<*>): Response = observable.toBlocking().first() as Response

    // Collects everything it receives; [initialRequest] is requested in onStart, before the producer is set.
    private class CollectingSubscriber(private val initialRequest: Long? = null) : Subscriber<Any?>() {
        val values: MutableList<Any?> = mutableListOf()
        val errors: MutableList<Throwable> = mutableListOf()
        var isCompleted: Boolean = false
        var lastProducer: Producer? = null
            private set

        override fun onStart() {
            initialRequest?.let { request(it) }
        }

        override fun setProducer(p: Producer) {
            lastProducer = p
            super.setProducer(p)
        }

        override fun onNext(t: Any?) {
            values += t
        }

        override fun onError(e: Throwable) {
            errors += e
        }

        override fun onCompleted() {
            isCompleted = true
        }

        fun requestMore(n: Long) = request(n)
    }

    private companion object {
        const val EXTENSIONS = "eu.kanade.tachiyomi.network.OkHttpExtensionsKt"
    }
}
