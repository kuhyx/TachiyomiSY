package eu.kanade.presentation.util

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class TimeUtilsTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun Duration.text(): String = toDurationString(context, fallback = "none")

    @Test
    fun zeroFallsBack() {
        Duration.ZERO.text() shouldBe "none"
    }

    @Test
    fun daysAndHoursDropTheRest() {
        (2.days + 3.hours + 4.minutes + 5.seconds).text() shouldBe "2d 3h"
    }

    @Test
    fun daysKeepMinutesWithoutHours() {
        (2.days + 4.minutes + 5.seconds).text() shouldBe "2d 4m"
    }

    @Test
    fun hoursKeepMinutes() {
        (3.hours + 4.minutes + 5.seconds).text() shouldBe "3h 4m"
    }

    @Test
    fun minutesKeepSeconds() {
        (4.minutes + 5.seconds).text() shouldBe "4m 5s"
        5.seconds.text() shouldBe "5s"
    }

    @Test
    fun neverBeforeTheEpoch() {
        compose.setContent { Text(relativeTimeSpanString(0L)) }
        compose.onNodeWithText("Never").assertExists()
    }

    @Test
    fun justNowWithinAMinute() {
        val now = System.currentTimeMillis()
        compose.setContent { Text(relativeTimeSpanString(now)) }
        compose.onNodeWithText("Just now").assertExists()
    }

    @Test
    fun olderTimesAreRelative() {
        val then = System.currentTimeMillis() - 3.hours.inWholeMilliseconds
        compose.setContent { Text(relativeTimeSpanString(then)) }
        compose.onNodeWithText("3 hours ago").assertExists()
    }
}
