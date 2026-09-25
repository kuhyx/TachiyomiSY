package eu.kanade.presentation.history.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.history.historyRow
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.history.model.HistoryWithRelations

@RunWith(RobolectricTestRunner::class)
internal class HistoryItemTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(history: HistoryWithRelations) {
        compose.setContent {
            MaterialTheme {
                HistoryItem(history = history, onClickCover = {}, onClickResume = {}, onClickDelete = {}, onClickFavorite = {})
            }
        }
    }

    @Test
    fun favouritesHideTheAddButton() {
        show(historyRow(favorite = true))
        compose.onNodeWithContentDescription("Add to library").assertDoesNotExist()
    }

    @Test
    fun chapterNumberPrefixesTheTime() {
        show(historyRow(chapterNumber = 10.5))
        compose.onNodeWithText("Ch. 10.5", substring = true).assertExists()
    }

    @Test
    fun unknownChapterShowsOnlyTheTime() {
        show(historyRow(chapterNumber = -1.0, readAt = null))
        compose.onNodeWithText("Ch.", substring = true).assertDoesNotExist()
    }

    @Test
    fun everyProviderValueRenders() {
        val values = HistoryWithRelationsProvider().values.toList()
        values.size shouldBe 3
        var history by mutableStateOf(values.first())
        compose.setContent { HistoryItemPreviews(history) }
        values.forEach {
            history = it
            compose.waitForIdle()
        }
        compose.onNodeWithText("Test Title").assertExists()
    }
}
