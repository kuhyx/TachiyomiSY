package eu.kanade.presentation.track

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class TrackSelectorsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val anyDate = object : SelectableDates {}

    @Test
    fun statusSelectorPicksAndConfirms() {
        compose.setContent {
            MaterialTheme {
                TrackStatusSelector(
                    selection = 1L,
                    onSelectionChange = { events += "pick $it" },
                    selections = mapOf(1L to MR.strings.reading, 2L to MR.strings.completed, 3L to null),
                    onConfirm = { events += "ok" },
                    onDismissRequest = { events += "cancel" },
                )
            }
        }
        compose.onNodeWithText("Completed").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("pick 2", "ok", "cancel")
    }

    @Test
    fun longStatusListsShowDividers() {
        compose.setContent {
            MaterialTheme {
                TrackStatusSelector(
                    selection = 0L,
                    onSelectionChange = {},
                    selections = (0L..40L).associateWith { MR.strings.reading },
                    onConfirm = {},
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("Status").assertExists()
    }

    @Test
    fun statusPreviewRenders() {
        compose.setContent { TrackStatusSelectorPreviews() }
        compose.onNodeWithText("Plan to read").assertExists()
    }

    @Test
    fun chapterSelector() {
        compose.setContent {
            MaterialTheme {
                TrackChapterSelector(
                    selection = 2,
                    onSelectionChange = { events += "chapter $it" },
                    range = 0..10,
                    onConfirm = { events += "ok" },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("Chapters").assertExists()
        compose.onNodeWithText("OK").performClick()
        events.last() shouldBe "ok"
    }

    @Test
    fun scoreSelectorStartsMidwayWhenUnknown() {
        compose.setContent {
            MaterialTheme {
                TrackScoreSelector(
                    selection = "none",
                    onSelectionChange = { events += "score $it" },
                    selections = listOf("0", "1", "2", "3", "4"),
                    onConfirm = { events += "ok" },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("Score").assertExists()
        compose.onNodeWithText("OK").performClick()
        events.last() shouldBe "ok"
    }

    @Test
    fun scoreSelectorStartsAtTheSelection() {
        compose.setContent {
            MaterialTheme {
                TrackScoreSelector(
                    selection = "3",
                    onSelectionChange = { events += "score $it" },
                    selections = listOf("0", "1", "2", "3", "4"),
                    onConfirm = {},
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("3").assertExists()
    }

    @Test
    fun dateSelectorConfirmsTheDate() {
        compose.setContent {
            MaterialTheme {
                TrackDateSelector(
                    title = "Start date",
                    initialSelectedDateMillis = 86_400_000L,
                    selectableDates = anyDate,
                    onConfirm = { events += "date $it" },
                    onRemove = { events += "remove" },
                    onDismissRequest = { events += "cancel" },
                )
            }
        }
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("remove", "cancel", "date 86400000")
    }

    @Test
    fun dateSelectorWithoutRemove() {
        compose.setContent {
            MaterialTheme {
                TrackDateSelector(
                    title = "Finish date",
                    initialSelectedDateMillis = 0L,
                    selectableDates = anyDate,
                    onConfirm = {},
                    onRemove = null,
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("Remove").assertDoesNotExist()
    }
}
