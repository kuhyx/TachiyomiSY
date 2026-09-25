package eu.kanade.tachiyomi.ui.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

private const val AWAIT_MILLIS = 10_000L

/**
 * The first value of this flow matching [predicate]. Screen models load on the real IO dispatcher
 * (`launchIO`), so their state is awaited from the outside rather than scheduled.
 */
internal fun <T> Flow<T>.await(predicate: (T) -> Boolean): T =
    runBlocking { withTimeout(AWAIT_MILLIS) { first(predicate) } }

/** Runs `screenModelScope` (Main) eagerly in the calling thread. */
internal fun mainUnconfined() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

internal fun mainReset() {
    Dispatchers.resetMain()
}
