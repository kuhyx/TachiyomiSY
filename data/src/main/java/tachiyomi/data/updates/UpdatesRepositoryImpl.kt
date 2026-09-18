package tachiyomi.data.updates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.data.Database
import tachiyomi.data.UpdatesFilter
import tachiyomi.data.awaitList
import tachiyomi.data.getUpdatesQuery
import tachiyomi.data.subscribeToList
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository

/** [UpdatesRepository] on the SQLDelight updates view and the hand-written merged-aware updates query (SY). */
public class UpdatesRepositoryImpl(
    private val database: Database,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return database.updatesViewQueries
            .getUpdatesByReadStatus(read = read, after = after, limit = limit)
            .awaitList(UpdatesMapper::mapUpdates)
    }

    override fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>> {
        val filter = UpdatesFilter(
            after = after,
            limit = limit,
            // invert because unread in Kotlin -> read column in SQL
            read = unread?.let { !it },
            started = started?.toLong(),
            bookmarked = bookmarked,
            hideExcludedScanlators = hideExcludedScanlators.toLong(),
        )
        // SY: the generated query only drives change notifications; the rows come from the merged-aware query.
        return database.updatesViewQueries
            .getRecentUpdatesWithFilters(
                after = filter.after,
                limit = filter.limit,
                read = filter.read,
                started = filter.started,
                bookmarked = filter.bookmarked,
                hideExcludedScanlators = filter.hideExcludedScanlators,
            )
            .subscribeToList()
            .map { getUpdatesQuery(filter).awaitList(UpdatesMapper::mapUpdates) }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return database.updatesViewQueries
            .getUpdatesByReadStatus(read = read, after = after, limit = limit)
            .subscribeToList(UpdatesMapper::mapUpdates)
    }
}
