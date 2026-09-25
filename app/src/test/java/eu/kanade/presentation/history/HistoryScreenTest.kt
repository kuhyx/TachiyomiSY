package eu.kanade.presentation.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
internal class HistoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val dialogs = mutableListOf<HistoryScreenModel.Dialog?>()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(state: HistoryScreenModel.State) {
        compose.setContent {
            MaterialTheme {
                HistoryScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onSearchQueryChange = { events += "search $it" },
                    onClickCover = { events += "cover $it" },
                    onClickResume = { manga, chapter -> events += "resume $manga $chapter" },
                    onClickFavorite = { events += "favorite $it" },
                    onDialogChange = { dialogs += it },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun loadingShowsTheTitle() {
        show(HistoryScreenModel.State())
        compose.onNodeWithText("History").assertExists()
    }

    @Test
    fun emptyWithoutQuery() {
        show(HistoryScreenModel.State(list = emptyList()))
        compose.onNodeWithText("Nothing read recently").assertExists()
    }

    @Test
    fun emptyWithQuery() {
        show(HistoryScreenModel.State(searchQuery = "needle", list = emptyList()))
        compose.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun emptyQueryCountsAsNoQuery() {
        show(HistoryScreenModel.State(searchQuery = "", list = emptyList()))
        compose.onNodeWithText("Nothing read recently").assertExists()
    }

    @Test
    fun clearHistoryOpensTheDialog() {
        show(HistoryScreenModel.State(list = emptyList()))
        compose.onNodeWithContentDescription("Clear history").performClick()
        dialogs shouldContainExactly listOf(HistoryScreenModel.Dialog.DeleteAll)
    }

    @Test
    fun rowsForwardEveryClick() {
        val row = historyRow()
        show(
            HistoryScreenModel.State(
                list = listOf(HistoryUiModel.Header(LocalDate.now()), HistoryUiModel.Item(row)),
            ),
        )
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Title 3").performClick()
        compose.onNodeWithContentDescription("Add to library").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithContentDescription("", substring = false).performClick()
        events shouldContainExactly listOf("resume 3 300", "favorite 3", "cover 3")
        dialogs shouldContainExactly listOf(HistoryScreenModel.Dialog.Delete(row))
    }
}
