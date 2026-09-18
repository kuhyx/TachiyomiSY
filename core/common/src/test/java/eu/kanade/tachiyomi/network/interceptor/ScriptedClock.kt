package eu.kanade.tachiyomi.network.interceptor

import android.os.SystemClock
import io.mockk.every
import io.mockk.mockkStatic

/**
 * Scripted readings for `SystemClock.elapsedRealtime()`: every read hands out the next value (the last one
 * repeats once the script runs dry) and runs the hook registered for that read, so a test can cancel a call
 * or interrupt the thread at the exact moment the limiter looks at the clock.
 */
internal class ScriptedClock(vararg readings: Long) {
    private val pending = ArrayDeque(readings.toList())
    private val hooks = mutableMapOf<Int, () -> Unit>()
    private var last = 0L
    var reads: Int = 0
        private set

    fun onRead(index: Int, hook: () -> Unit) {
        hooks[index] = hook
    }

    fun read(): Long {
        reads += 1
        hooks[reads]?.invoke()
        val value = pending.removeFirstOrNull() ?: last
        last = value
        return value
    }
}

private const val PACKAGE = "eu.kanade.tachiyomi.network.interceptor"
internal const val RATE_LIMIT_KT: String = "$PACKAGE.RateLimitInterceptorKt"
internal const val HOST_RATE_LIMIT_KT: String = "$PACKAGE.SpecificHostRateLimitInterceptorKt"

/** Routes the limiter's clock through a [ScriptedClock] with [readings]; undo with `unmockkAll()`. */
internal fun installClock(vararg readings: Long): ScriptedClock {
    val clock = ScriptedClock(*readings)
    mockkStatic(SystemClock::class)
    every { SystemClock.elapsedRealtime() } answers { clock.read() }
    return clock
}
