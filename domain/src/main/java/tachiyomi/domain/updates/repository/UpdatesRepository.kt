package tachiyomi.domain.updates.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.model.UpdatesWithRelations

/** Reads of the updates feed: chapters of library manga, newest upload first. */
public interface UpdatesRepository {

    /** At most [limit] entries with read state [read] uploaded after epoch millis [after]. */
    public suspend fun awaitWithRead(read: Boolean, after: Long, limit: Long): List<UpdatesWithRelations>

    /**
     * At most [limit] entries uploaded after epoch millis [after], as a flow that re-emits on every
     * change. Each of [unread], [started] and [bookmarked] keeps only matching entries, or does not
     * filter when null; [hideExcludedScanlators] drops chapters from scanlators the manga excludes.
     */
    public fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>>

    /** [awaitWithRead] as a flow that re-emits on every change. */
    public fun subscribeWithRead(read: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>>
}
