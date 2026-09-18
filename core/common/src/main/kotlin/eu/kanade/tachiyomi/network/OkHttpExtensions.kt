package eu.kanade.tachiyomi.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Producer
import rx.Subscription
import java.io.IOException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resumeWithException

/** The JSON media type with UTF-8 charset. */
public val jsonMime: MediaType = "application/json; charset=utf-8".toMediaType()

/** Runs the call as an RxJava observable that emits the response. */
@OptIn(ExperimentalAtomicApi::class)
@Deprecated("Use suspend APIs instead")
public fun Call.asObservable(): Observable<Response> = Observable.unsafeCreate { subscriber ->
    // Since Call is a one-shot type, clone it for each new subscriber.
    val call = clone()

    // Wrap the call in a helper which handles both unsubscription and backpressure.
    val requestArbiter = object : Producer, Subscription {
        val boolean = AtomicBoolean(false)
        override fun request(n: Long) {
            if (n == 0L || !boolean.compareAndSet(expectedValue = false, newValue = true)) return

            try {
                val response = call.execute()
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(response)
                    subscriber.onCompleted()
                }
            } catch (e: Exception) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        }

        override fun unsubscribe() {
            call.cancel()
        }

        override fun isUnsubscribed(): Boolean = call.isCanceled()
    }

    subscriber.add(requestArbiter)
    subscriber.setProducer(requestArbiter)
}

/** Like [asObservable] but errors with [HttpException] on a non-2xx response. */
@Deprecated("Use suspend APIs instead")
public fun Call.asObservableSuccess(): Observable<Response> {
    @Suppress("DEPRECATION")
    return asObservable().doOnNext { response ->
        if (!response.isSuccessful) {
            response.close()
            throw HttpException(response.code)
        }
    }
}

// Based on https://github.com/square/okhttp/blob/master/okhttp-coroutines/src/main/kotlin/okhttp3/coroutines/ExecuteAsync.kt
// and https://github.com/gildor/kotlin-coroutines-okhttp
private suspend fun Call.await(callStack: Array<StackTraceElement>): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            try {
                this.cancel()
            } catch (_: Throwable) {
                // ignore
            }
        }

        this.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                val exception = IOException(e.message, e).apply { stackTrace = callStack }
                continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ ->
                    value.close()
                }
            }
        })
    }

/** Runs the call, cancelling it when the coroutine is cancelled. */
public suspend fun Call.await(): Response {
    val callStack = Exception("call site").stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}

/**
 * Similar to [await] but throws [HttpException] if [Response.isSuccessful] returns false.
 */
public suspend fun Call.awaitSuccess(): Response {
    val callStack = Exception("call site").stackTrace.run { copyOfRange(1, size) }
    val response = await(callStack)
    if (!response.isSuccessful) {
        response.close()
        throw HttpException(response.code).apply { stackTrace = callStack }
    }
    return response
}

/** A call that bypasses the cache and reports download progress to [listener]. */
public fun OkHttpClient.newCachelessCallWithProgress(
    request: Request,
    listener: ProgressListener,
    existingSize: Long = 0L,
): Call {
    val progressClient = newBuilder()
        .cache(null)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .apply {
                    if (existingSize > 0 && request.header("Range") == null) {
                        header("Range", "bytes=$existingSize-")
                    }
                }
                .build()

            val originalResponse = chain.proceed(request)
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body, listener, existingSize))
                .build()
        }
        .build()

    return progressClient.newCall(request)
}

/** Decodes the JSON body as [T] with the contextual [Json]. */
context(json: Json)
public inline fun <reified T> Response.parseAs(): T = json.decodeFromResponse(serializer(), this)

/** Decodes the JSON body of [response] with [deserializer]. */
@PublishedApi
internal fun <T> Json.decodeFromResponse(deserializer: DeserializationStrategy<T>, response: Response): T =
    response.body.source().use { decodeFromBufferedSource(deserializer, it) }
