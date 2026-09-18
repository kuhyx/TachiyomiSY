package tachiyomi.domain.history.model

import java.util.Date

/**
 * A history row: when a chapter was last read and for how long in total.
 *
 * @property id Row id.
 * @property chapterId Id of the chapter this row belongs to.
 * @property readAt When the chapter was last read; null when never or after a reset.
 * @property readDuration Total milliseconds spent reading the chapter, summed over every session.
 */
public data class History(
    val id: Long,
    val chapterId: Long,
    val readAt: Date?,
    val readDuration: Long,
) {
    /** The factory for a blank history row. */
    public companion object {
        /** A row with no chapter, no read time and every id and duration set to -1. */
        public fun create(): History = History(
            id = -1L,
            chapterId = -1L,
            readAt = null,
            readDuration = -1L,
        )
    }
}
