package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChapterTransitionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previousChapterWithAGap() {
        val current = ReaderChapter(previewChapter(name = "Current", scanlator = "Group", chapterNumber = 10.0))
        val previous = ReaderChapter(
            previewChapter(name = "Earlier", scanlator = "Group", chapterNumber = 5.0).copy(scanlator = null),
        )
        compose.setContent {
            MaterialTheme {
                ChapterTransition(
                    transition = ChapterTransition.Prev(current, previous),
                    currChapterDownloaded = true,
                    goingToChapterDownloaded = false,
                )
            }
        }
        compose.onNodeWithText("Previous:").assertExists()
        compose.onNodeWithText("Current:").assertExists()
        compose.onNodeWithText("Skipping 4 chapters", substring = true).assertExists()
        compose.onNodeWithContentDescription("Downloaded", useUnmergedTree = true).assertExists()
    }

    @Test
    fun everyPreviewRenders() {
        compose.setContent {
            Column {
                TransitionTextPreview()
                TransitionTextLongTitlePreview()
                TransitionTextWithGapPreview()
                TransitionTextNoNextPreview()
                TransitionNoPreviousPreview()
            }
        }
        compose.onAllNodesWithText("There's no next chapter").fetchSemanticsNodes().size shouldBe 1
        compose.onAllNodesWithText("There's no previous chapter").fetchSemanticsNodes().size shouldBe 1
    }
}
