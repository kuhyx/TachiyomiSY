package eu.kanade.tachiyomi.ui.category.biometric

import android.content.Context
import android.text.format.DateFormat
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal data class TimeRange(val startTime: Duration, val endTime: Duration) {
    override fun toString(): String {
        val startHour = startTime.inWholeHours
        val startMinute = (startTime - startHour.hours).inWholeMinutes
        val endHour = endTime.inWholeHours
        val endMinute = (endTime - endHour.hours).inWholeMinutes
        return String.format(Locale.ROOT, "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    fun toPreferenceString(): String = "${startTime.inWholeMinutes},${endTime.inWholeMinutes}"

    operator fun contains(other: Duration): Boolean = other in startTime..endTime

    companion object {
        fun fromPreferenceString(timeRange: String): TimeRange? {
            val index = timeRange.indexOf(',')
            return if (index != -1) {
                TimeRange(
                    timeRange.substring(0, index).toDoubleOrNull()?.minutes ?: return null,
                    timeRange.substring(index + 1).toDoubleOrNull()?.minutes ?: return null,
                )
            } else {
                return null
            }
        }
    }
}

/** The range in the device's time format, e.g. `8:00 AM - 5:30 PM`. */
internal fun TimeRange.getFormattedString(context: Context): String {
    val startDate = Date(startTime.inWholeMilliseconds)
    val endDate = Date(endTime.inWholeMilliseconds)
    val format = DateFormat.getTimeFormat(context)
    format.timeZone = SimpleTimeZone(0, "UTC")

    return format.format(startDate) + " - " + format.format(endDate)
}

/** Whether either end of this range falls inside [other]. */
internal fun TimeRange.conflictsWith(other: TimeRange): Boolean =
    startTime in other.startTime..other.endTime || endTime in other.startTime..other.endTime
