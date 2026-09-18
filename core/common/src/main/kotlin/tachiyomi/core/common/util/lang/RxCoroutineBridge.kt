package tachiyomi.core.common.util.lang

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Emitter
import rx.Observable
import rx.Subscriber
import rx.Subscription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Util functions for bridging RxJava and coroutines. Taken from TachiyomiEH/SY.
 */

/** Suspends until the observable emits its single value. */
public suspend fun <T> Observable<T>.awaitSingle(): T = single().awaitOne()

@OptIn(InternalCoroutinesApi::class)
private suspend fun <T> Observable<T>.awaitOne(): T = suspendCancellableCoroutine { cont ->
    cont.unsubscribeOnCancellation(
        subscribe(
            object : Subscriber<T>() {
                override fun onStart() {
                    request(1)
                }

                override fun onNext(t: T) {
                    cont.resume(t)
                }

                override fun onCompleted() {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            IllegalStateException(
                                "Should have invoked onNext",
                            ),
                        )
                    }
                }

                override fun onError(e: Throwable) {
                    /*
                     * Rx1 observable throws NoSuchElementException if cancellation happened before
                     * element emission. To mitigate this we try to atomically resume continuation with exception:
                     * if resume failed, then we know that continuation successfully cancelled itself
                     */
                    val token = cont.tryResumeWithException(e)
                    if (token != null) {
                        cont.completeResume(token)
                    }
                }
            },
        ),
    )
}

internal fun <T> CancellableContinuation<T>.unsubscribeOnCancellation(sub: Subscription) =
    invokeOnCancellation { sub.unsubscribe() }

/** Wraps a suspending [block] as a cold observable. */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
public fun <T> runAsObservable(
    backpressureMode: Emitter.BackpressureMode = Emitter.BackpressureMode.NONE,
    block: suspend () -> T,
): Observable<T> {
    return Observable.create(
        { emitter ->
            val job = AppScope.launch(Dispatchers.Unconfined, start = CoroutineStart.ATOMIC) {
                try {
                    emitter.onNext(block())
                    emitter.onCompleted()
                } catch (_: CancellationException) {
                    // Normal cancellation, not an error.
                    emitter.onCompleted()
                } catch (e: Throwable) {
                    emitter.onError(e)
                }
            }
            emitter.setCancellation { job.cancel() }
        },
        backpressureMode,
    )
}
