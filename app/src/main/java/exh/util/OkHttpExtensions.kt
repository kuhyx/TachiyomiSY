package exh.util

import okhttp3.Call
import okhttp3.Response
import rx.Observable
import rx.Producer
import rx.Subscription
import java.util.concurrent.atomic.AtomicBoolean

internal fun Call.asObservableWithStacktrace(): Observable<Pair<Exception, Response>> {
    // Record stacktrace at creation time for easier debugging
    //   asObservable is involved in a lot of crashes so this is worth the performance hit
    val asyncStackTrace = Exception("Async stacktrace")

    return Observable.unsafeCreate { subscriber ->
        // Since Call is a one-shot type, clone it for each new subscriber.
        val call = clone()

        // Wrap the call in a helper which handles both unsubscription and backpressure.
        val requestArbiter = object : AtomicBoolean(), Producer, Subscription {
            val executed = AtomicBoolean(false)

            override fun request(n: Long) {
                if (n == 0L || !compareAndSet(false, true)) return

                try {
                    val response = call.execute()
                    executed.set(true)
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(asyncStackTrace to response)
                        subscriber.onCompleted()
                    }
                } catch (expected: Throwable) {
                    // Any failure ends here and the fallback below applies.
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(expected.withRootCause(asyncStackTrace))
                    }
                }
            }

            override fun unsubscribe() {
                if (!executed.get()) {
                    call.cancel()
                }
            }

            override fun isUnsubscribed(): Boolean = call.isCanceled()
        }

        subscriber.add(requestArbiter)
        subscriber.setProducer(requestArbiter)
    }
}
