package eu.kanade.tachiyomi.util

import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle as awaitSingleRx

/** Suspends until the observable emits its single value. */
public actual suspend fun <T> Observable<T>.awaitSingle(): T = awaitSingleRx()
