package eu.kanade.tachiyomi.util

import rx.Observable

public expect suspend fun <T> Observable<T>.awaitSingle(): T
