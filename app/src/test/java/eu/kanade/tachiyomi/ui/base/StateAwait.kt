package eu.kanade.tachiyomi.ui.base

import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.source.online.readMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val AWAIT_MILLIS = 10_000L
private const val SCOPE_DRAIN_MILLIS = 5_000L

/**
 * The first value of this flow matching [predicate]. Screen models load on the real IO dispatcher
 * (`launchIO`), so their state is awaited from the outside rather than scheduled.
 */
internal fun <T> Flow<T>.await(predicate: (T) -> Boolean): T =
    runBlocking { withTimeout(AWAIT_MILLIS) { first(predicate) } }

/** Runs `screenModelScope` (Main) eagerly in the calling thread; its delays run on the returned virtual clock. */
internal fun mainUnconfined(): TestCoroutineScheduler {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    return dispatcher.scheduler
}

/**
 * Restores the platform Main after first cancelling, draining and forgetting every coroutine scope
 * Voyager cached for screen models built outside a Navigator. Those models share one
 * `standalone:ScreenModelCoroutineScope` on Dispatchers.Main, so a `flowOn(IO)` producer still running
 * in it would otherwise resume onto a reset Main and fail a later suite's `runTest`. The drain is
 * bounded rather than asserted: a coroutine queued on a test dispatcher nobody advances never finishes.
 * Unlike the manga suites' `clearVoyagerScopes`, this touches no Looper, so plain JUnit tests can use it.
 */
internal fun mainReset() {
    try {
        forgetScreenModelScopes()
    } finally {
        Dispatchers.resetMain()
    }
}

private fun forgetScreenModelScopes() {
    val dependencies = checkNotNull(ScreenModelStore.readMember(ScreenModelStore::class, "dependencies"))
    val remove = dependencies::class.java.methods.first { it.name == "remove" && it.parameterCount == 1 }
    val entries = (dependencies as Map<*, *>).entries.toList()
    val jobs = entries.mapNotNull { (_, value) ->
        val scope = (value as? Pair<*, *>)?.first as? CoroutineScope
        scope?.coroutineContext?.get(Job)
    }
    try {
        jobs.forEach { it.cancel() }
        runBlocking { withTimeoutOrNull(SCOPE_DRAIN_MILLIS) { jobs.joinAll() } }
    } finally {
        entries.forEach { (key, _) -> remove.invoke(dependencies, key) }
    }
}
