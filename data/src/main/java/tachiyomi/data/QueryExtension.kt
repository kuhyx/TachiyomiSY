package tachiyomi.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/*
 * Every repository reads generated rows and maps them with a one-argument
 * mapper (`Mangas -> Manga`), so the row shape is declared once, in the
 * query. These helpers keep that a single chained call at every site.
 */

/** Every row, re-emitted whenever the queried tables change. */
public fun <T : Any> Query<T>.subscribeToList(
    context: CoroutineContext = EmptyCoroutineContext,
): Flow<List<T>> = asFlow().mapToList(context)

/** [subscribeToList] with each row mapped by [transform]. */
public fun <T : Any, R> Query<T>.subscribeToList(transform: (T) -> R): Flow<List<R>> =
    subscribeToList().map { rows -> rows.map(transform) }

/** The single row, re-emitted whenever the queried tables change; throws when there is none. */
public fun <T : Any> Query<T>.subscribeToOne(
    context: CoroutineContext = EmptyCoroutineContext,
): Flow<T> = asFlow().mapToOne(context)

/** [subscribeToOne] mapped by [transform]. */
public fun <T : Any, R> Query<T>.subscribeToOne(transform: (T) -> R): Flow<R> = subscribeToOne().map(transform)

/** The single row or null, re-emitted whenever the queried tables change. */
public fun <T : Any> Query<T>.subscribeToOneOrNull(
    context: CoroutineContext = EmptyCoroutineContext,
): Flow<T?> = asFlow().mapToOneOrNull(context)

/** [subscribeToOneOrNull] mapped by [transform]. */
public fun <T : Any, R> Query<T>.subscribeToOneOrNull(transform: (T) -> R): Flow<R?> =
    subscribeToOneOrNull().map { row -> row?.let(transform) }

/** Every row, each mapped by [transform]. */
public suspend fun <T : Any, R> ExecutableQuery<T>.awaitList(transform: (T) -> R): List<R> =
    awaitAsList().map(transform)

/** The single row mapped by [transform]; throws when there is none. */
public suspend fun <T : Any, R> ExecutableQuery<T>.awaitOne(transform: (T) -> R): R = transform(awaitAsOne())

/** The single row mapped by [transform], or null. */
public suspend fun <T : Any, R> ExecutableQuery<T>.awaitOneOrNull(transform: (T) -> R): R? =
    awaitAsOneOrNull()?.let(transform)
