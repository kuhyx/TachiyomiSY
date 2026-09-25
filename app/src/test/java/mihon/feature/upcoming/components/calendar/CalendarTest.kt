package mihon.feature.upcoming.components.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class CalendarTest {
    @get:Rule
    val compose = createComposeRule()

    private val clicked = mutableListOf<LocalDate>()

    private fun title(month: YearMonth) = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(month)

    @Test
    fun theMonthSlidesBothWays() {
        val start = YearMonth.now()
        var month by mutableStateOf(start)
        compose.setContent {
            MaterialTheme {
                Calendar(
                    selectedYearMonth = month,
                    events = mapOf(LocalDate.now() to 2),
                    setSelectedYearMonth = { month = it },
                    onClickDay = { clicked += it },
                )
            }
        }
        compose.onNodeWithText(title(start)).assertExists()
        compose.onNodeWithText(LocalDate.now().dayOfMonth.toString()).performClick()
        compose.onNodeWithContentDescription("Next Month").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(title(start.plusMonths(1L))).assertExists()
        compose.onNodeWithContentDescription("Previous Month").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(title(start)).assertExists()
        clicked shouldContainExactly listOf(LocalDate.now())
    }

    @Test
    fun aDayCapsItsIndicators() {
        compose.setContent {
            MaterialTheme {
                CalendarDay(date = LocalDate.of(2000, 1, 2), events = 9, onDayClick = {})
                CalendarIndicator(index = 0, size = 56.dp, color = Color.Red)
            }
        }
        compose.onNodeWithText("2").assertExists()
    }

    @Test
    fun theHeaderPreview() {
        compose.setContent { MaterialTheme { CalenderHeaderPreview() } }
        compose.onNodeWithText(title(YearMonth.now())).assertExists()
    }
}
