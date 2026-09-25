package eu.kanade.tachiyomi.ui.category.biometric

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class TimeRangeTest {
    private val morning = TimeRange(8.hours + 5.minutes, 17.hours + 30.minutes)

    @Test
    fun toStringPadsHoursAndMinutes() {
        morning.toString() shouldBe "08:05 - 17:30"
    }

    @Test
    fun preferenceStringRoundTrips() {
        morning.toPreferenceString() shouldBe "485,1050"
        TimeRange.fromPreferenceString("485,1050") shouldBe morning
    }

    @Test
    fun malformedStringsParseToNull() {
        TimeRange.fromPreferenceString("485").shouldBeNull()
        TimeRange.fromPreferenceString("x,1050").shouldBeNull()
        TimeRange.fromPreferenceString("485,y").shouldBeNull()
    }

    @Test
    fun containsIsInclusive() {
        (morning.startTime in morning).shouldBeTrue()
        (morning.endTime in morning).shouldBeTrue()
        (1.hours in morning).shouldBeFalse()
    }

    @Test
    fun conflictsWhenEitherEndOverlaps() {
        TimeRange(7.hours, 9.hours).conflictsWith(morning).shouldBeTrue()
        TimeRange(17.hours, 20.hours).conflictsWith(morning).shouldBeTrue()
        TimeRange(1.hours, 2.hours).conflictsWith(morning).shouldBeFalse()
    }

    @Test
    fun dataClassMembers() {
        val copy = morning.copy(endTime = 18.hours)
        copy shouldNotBe morning
        copy.hashCode() shouldNotBe morning.hashCode()
        val item = TimeRangeItem(morning, "text")
        item.copy() shouldBe item
        item.hashCode() shouldBe TimeRangeItem(morning, "text").hashCode()
        item.toString() shouldBe "TimeRangeItem(timeRange=08:05 - 17:30, formattedString=text)"
    }
}
