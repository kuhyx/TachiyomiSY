package exh.md.utils

import java.util.Locale

internal enum class FollowStatus(val long: Long) {
    UNFOLLOWED(long = 0L),
    READING(long = 1L),
    COMPLETED(long = 2L),
    ON_HOLD(long = 3L),
    PLAN_TO_READ(long = 4L),
    DROPPED(long = 5L),
    RE_READING(long = 6L),
    ;

    fun toDex(): String = this.name.lowercase(Locale.US)

    companion object {
        fun fromDex(
            value: String?,
        ): FollowStatus = entries.firstOrNull { it.name.lowercase(Locale.US) == value } ?: UNFOLLOWED
        fun fromLong(value: Long): FollowStatus = entries.firstOrNull { it.long == value } ?: UNFOLLOWED
    }
}
