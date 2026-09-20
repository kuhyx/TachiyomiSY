package tachiyomi.presentation.core.util

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import org.junit.rules.ExternalResource

/**
 * Installs a composition tracer for the test, so `isTraceInProgress()` inside every composable
 * is true and the `traceEventStart`/`traceEventEnd` arms run; counts the events it received.
 */
@OptIn(InternalComposeTracingApi::class)
internal class ComposeTracerRule : ExternalResource() {
    var started: Int = 0
        private set

    private val tracer = object : CompositionTracer {
        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            started += 1
        }

        override fun traceEventEnd() = Unit

        override fun isTraceInProgress(): Boolean = true
    }

    override fun before() {
        Composer.setTracer(tracer)
    }

    override fun after() {
        Composer.setTracer(null)
    }
}
