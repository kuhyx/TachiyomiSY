package eu.kanade.presentation.track

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

/** The grid's date row needs both date actions, and an item's own defaults. */
@RunWith(RobolectricTestRunner::class)
internal class TrackDetailsGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun oneDateActionHidesTheRow() {
        compose.setContent {
            MaterialTheme {
                TrackDetailsGrid(
                    status = MR.strings.reading,
                    onStatusClick = { events += "status" },
                    chapters = "3",
                    onChaptersClick = { events += "chapters" },
                    score = null,
                    onScoreClick = null,
                    startDate = "2024-01-02",
                    onStartDateClick = { events += "start" },
                    endDate = null,
                    onEndDateClick = null,
                )
            }
        }
        compose.onNodeWithText("2024-01-02").assertDoesNotExist()
        compose.onNodeWithText("Reading").performClick()
        events shouldContainExactly listOf("status")
    }

    @Test
    fun anItemWithItsDefaults() {
        compose.setContent {
            MaterialTheme {
                TrackDetailsItem(text = "Shown", onClick = { events += "item" })
            }
        }
        compose.onNodeWithText("Shown").performClick()
        events shouldContainExactly listOf("item")
    }
}
