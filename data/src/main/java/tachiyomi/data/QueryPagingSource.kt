package tachiyomi.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlin.properties.Delegates

/**
 * Offset-based [PagingSource] over a SQLDelight query that invalidates itself whenever the current
 * page's query reports a change; a query failure becomes a paging error.
 *
 * @param RowType The row type the query maps to.
 * @property countQuery Query for the total row count, used to compute the item placeholders.
 * @property queryProvider Query for one page, given `(limit, offset)`.
 */
@Suppress("unused")
public class QueryPagingSource<RowType : Any>(
    public val countQuery: () -> Query<Long>,
    public val queryProvider: (Long, Long) -> Query<RowType>,
) : PagingSource<Long, RowType>(), Query.Listener {

    override val jumpingSupported: Boolean = true

    private var currentQuery: Query<RowType>? by Delegates.observable(null) { _, old, new ->
        old?.removeListener(this)
        new?.addListener(this)
    }

    init {
        registerInvalidatedCallback {
            currentQuery?.removeListener(this)
            currentQuery = null
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, RowType> {
        try {
            val key = params.key ?: 0L
            val loadSize = params.loadSize
            val count = countQuery().awaitAsOne()

            val limit = loadSize.toLong()
            val offset = if (params is LoadParams.Prepend) key - loadSize else key

            val data = queryProvider(limit, offset)
                .also { currentQuery = it }
                .awaitAsList()

            val prevKey = if (params is LoadParams.Append) offset - loadSize else offset
            val nextKey = offset + loadSize

            return LoadResult.Page(
                data = data,
                prevKey = if (offset <= 0L || prevKey < 0L) null else prevKey,
                nextKey = if (offset + loadSize >= count) null else nextKey,
                itemsBefore = maxOf(0L, offset).toInt(),
                itemsAfter = maxOf(0L, count - (offset + loadSize)).toInt(),
            )
        } catch (expected: Exception) {
            // Any query failure becomes a paging error the UI shows.
            return LoadResult.Error(throwable = expected)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, RowType>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override fun queryResultsChanged() {
        invalidate()
    }
}
