package eu.kanade.tachiyomi.util

import rx.Observable

/** Suspends until the observable emits its single value. */
public expect suspend fun <T> Observable<T>.awaitSingle(): T
