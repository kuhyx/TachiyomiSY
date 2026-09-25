package eu.kanade.tachiyomi.util.lang

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

@RunWith(RobolectricTestRunner::class)
internal class DateExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val iso = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun timestampStringsCombineDateAnd() {
        val dateTime = LocalDateTime.of(2024, 3, 9, 14, 5)
        val stamp = dateTime.toDateTimestampString(iso)
        stamp shouldContain "2024-03-09 "
        stamp shouldContain DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(dateTime)
        Date(0).toTimestampString().isNotEmpty() shouldBe true
    }

    @Test
    fun convertsEpochMillisBetween() {
        val millis = 1_700_000_000_000L
        millis.convertEpochMillisZone(ZoneOffset.UTC, ZoneOffset.UTC) shouldBe millis
        millis.convertEpochMillisZone(ZoneOffset.ofHours(2), ZoneOffset.UTC) shouldBe millis + 2 * 3_600_000L
    }

    @Test
    fun localDates() {
        val millis = 1_700_000_000_000L
        millis.toLocalDate() shouldBe LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        Instant.ofEpochMilli(millis).toLocalDate() shouldBe millis.toLocalDate()
        Instant.ofEpochMilli(millis).toLocalDate(ZoneOffset.UTC) shouldBe LocalDate.of(2023, 11, 14)
    }

    @Test
    fun relativeStringsPerDistance() {
        val today = LocalDate.now()
        today.toRelativeString(context, relative = false, dateFormat = iso) shouldBe iso.format(today)
        today.toRelativeString(context) shouldBe "Today"
        today.minusDays(1).toRelativeString(context) shouldBe "Yesterday"
        today.minusDays(3).toRelativeString(context) shouldBe "3 days ago"
        today.minusDays(7).toRelativeString(context, dateFormat = iso) shouldBe iso.format(today.minusDays(7))
        today.plusDays(1).toRelativeString(context) shouldBe "Tomorrow"
        today.plusDays(4).toRelativeString(context) shouldBe "In 4 days"
        today.plusDays(8).toRelativeString(context, dateFormat = iso) shouldBe iso.format(today.plusDays(8))
    }
}
