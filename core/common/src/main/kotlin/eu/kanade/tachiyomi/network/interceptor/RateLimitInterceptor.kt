package eu.kanade.tachiyomi.network.interceptor

import android.os.SystemClock
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

/**
 * An OkHttp interceptor that handles rate limiting.
 *
 * This uses `java.time` APIs and is the legacy method, kept
 * for compatibility reasons with existing extensions.
 *
 * Examples:
 *
 * permits = 5,  period = 1, unit = seconds  =>  5 requests per second
 * permits = 10, period = 2, unit = minutes  =>  10 requests per 2 minutes
 *
 * @since extension-lib 1.3
 *
 * @param permits [Int]   Number of requests allowed within a period of units.
 * @param period [Long]   The limiting duration. Defaults to 1.
 * @param unit [TimeUnit] The unit of time for the period. Defaults to seconds.
 */
@Deprecated("Use the version with kotlin.time APIs instead.")
public fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = addInterceptor(RateLimitInterceptor(null, permits, period.toDuration(unit.toDurationUnit())))

/**
 * An OkHttp interceptor that handles rate limiting.
 *
 * Examples:
 *
 * permits = 5,  period = 1.seconds  =>  5 requests per second
 * permits = 10, period = 2.minutes  =>  10 requests per 2 minutes
 *
 * @since extension-lib 1.5
 *
 * @param permits [Int]     Number of requests allowed within a period of units.
 * @param period [Duration] The limiting duration. Defaults to 1.seconds.
 */
public fun OkHttpClient.Builder.rateLimit(permits: Int, period: Duration = 1.seconds): OkHttpClient.Builder =
    addInterceptor(RateLimitInterceptor(null, permits, period))

/** We can probably accept domains or wildcards by comparing with [endsWith], etc. */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal class RateLimitInterceptor(
    private val host: String?,
    private val permits: Int,
    period: Duration,
) : Interceptor {

    private val requestQueue = ArrayDeque<Long>(permits)
    private val rateLimitMillis = period.inWholeMilliseconds
    private val fairLock = Semaphore(1, true)

    override fun intercept(chain: Interceptor.Chain): Response {
        val call = chain.call()
        if (call.isCanceled()) throw IOException("Canceled")

        val request = chain.request()
        when (host) {
            null, request.url.host -> {} // need rate limit
            else -> return chain.proceed(request)
        }

        val timestamp = acquireSlot(call)

        val response = chain.proceed(request)
        if (response.networkResponse == null) { // response is cached, remove it from queue
            synchronized(requestQueue) {
                if (requestQueue.isNotEmpty() && timestamp >= requestQueue.first) {
                    requestQueue.removeFirstOccurrence(timestamp)
                    (requestQueue as Object).notifyAll()
                }
            }
        }

        return response
    }

    private fun acquireSlot(call: Call): Long {
        try {
            fairLock.acquire()
        } catch (e: InterruptedException) {
            throw IOException(e)
        }
        try {
            synchronized(requestQueue) {
                waitForFreeSlot(call)
                val timestamp = SystemClock.elapsedRealtime()
                requestQueue.addLast(timestamp)
                return timestamp
            }
        } finally {
            fairLock.release()
        }
    }

    // Blocks while the queue is full; returns as soon as an expired entry has been dropped.
    private fun waitForFreeSlot(call: Call) {
        while (requestQueue.size >= permits) {
            val periodStart = SystemClock.elapsedRealtime() - rateLimitMillis
            val removedExpired = dropExpiredEntries(periodStart)
            if (call.isCanceled()) throw IOException("Canceled")
            if (removedExpired) break
            waitForFirstEntry(periodStart)
        }
    }

    private fun dropExpiredEntries(periodStart: Long): Boolean {
        var removed = false
        while (!requestQueue.isEmpty() && requestQueue.first <= periodStart) {
            requestQueue.removeFirst()
            removed = true
        }
        return removed
    }

    private fun waitForFirstEntry(periodStart: Long) {
        try { // wait for the first entry to expire, or notified by cached response
            (requestQueue as Object).wait(requestQueue.first - periodStart)
        } catch (_: InterruptedException) {
            // Woken early: the caller re-checks the queue.
        }
    }
}
