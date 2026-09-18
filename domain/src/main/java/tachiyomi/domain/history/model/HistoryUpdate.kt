package tachiyomi.domain.history.model

import java.util.Date

/**
 * One reading session to record for a chapter; upserting it sets the read time and adds
 * the session duration to the chapter's total.
 *
 * @property chapterId Id of the chapter that was read.
 * @property readAt When the session ended, stored as the chapter's last read time.
 * @property sessionReadDuration Milliseconds spent in this session, added to the stored total.
 */
public data class HistoryUpdate(
    val chapterId: Long,
    val readAt: Date,
    val sessionReadDuration: Long,
)

// SY -->

/** This row as an update that re-adds its whole duration; a null [History.readAt] becomes the epoch. */
public fun History.toHistoryUpdate(): HistoryUpdate {
    return HistoryUpdate(
        chapterId,
        readAt ?: Date(0),
        readDuration,
    )
}
// SY <--
