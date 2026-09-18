package tachiyomi.domain.updates.interactor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retry
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Reads of the updates feed: chapters of library manga uploaded recently, newest first,
 * capped at 500 rows. Every read retries every 5 seconds for as long as the store throws a
 * `NullPointerException`; any other failure propagates.
 */
public class GetUpdates(
    private val repository: UpdatesRepository,
) {

    /** The feed entries with read state [read] uploaded after epoch millis [after]. */
    public suspend fun await(read: Boolean, after: Long): List<UpdatesWithRelations> {
        // SY -->
        while (true) {
            try {
                return repository.awaitWithRead(read, after, limit = LIMIT)
            } catch (expected: NullPointerException) {
                // The store is briefly unreadable while the app initialises; retry like the flows do.
                delay(RETRY_DELAY)
            }
        }
        // SY <--
    }

    /**
     * The feed entries uploaded after [instant] as a flow that re-emits on every change. Each of
     * [unread], [started] and [bookmarked] keeps only matching entries, or does not filter when null;
     * [hideExcludedScanlators] drops chapters from scanlators the manga excludes.
     */
    public fun subscribe(
        instant: Instant,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeAll(
            instant.toEpochMilli(),
            limit = LIMIT,
            unread = unread,
            started = started,
            bookmarked = bookmarked,
            hideExcludedScanlators = hideExcludedScanlators,
        )
            // SY -->
            .catchNPE()
        // SY <--
    }

    /** [await] as a flow that re-emits on every change. */
    public fun subscribe(read: Boolean, after: Long): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeWithRead(read, after, limit = LIMIT)
            // SY -->
            .catchNPE()
        // SY <--
    }

    // SY -->
    private fun <T> Flow<T>.catchNPE() = retry {
        if (it is NullPointerException) {
            delay(RETRY_DELAY)
            true
        } else {
            false
        }
    }
    // SY <--

    private companion object {
        const val LIMIT = 500L
        internal val RETRY_DELAY = 5.seconds
    }
}
