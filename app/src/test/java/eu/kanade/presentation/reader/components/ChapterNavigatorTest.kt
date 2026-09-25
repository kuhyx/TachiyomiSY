package eu.kanade.presentation.reader.components

import androidx.compose.ui.test.swipe
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.setSlider
import io.kotest.matchers.collections.shouldContain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class ChapterNavigatorTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private var page by mutableIntStateOf(1)

    private fun show(type: ChapterNavigatorType, total: Int = 10, enabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                ChapterNavigator(
                    type = type,
                    navigation = ChapterNavigation(
                        onNextChapter = { events += "next" },
                        enabledNext = enabled,
                        onPreviousChapter = { events += "previous" },
                        enabledPrevious = enabled,
                        onPageIndexChange = {
                            page = it + 1
                            events += "page $it"
                        },
                        onPageIndexChangeFinished = { events += "finished" },
                    ),
                    currentPage = page,
                    currentPageText = "$page",
                    totalPages = total,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun buttons() {
        compose.onNodeWithContentDescription("Previous chapter").performClick()
        compose.onNodeWithContentDescription("Next chapter").performClick()
    }

    @Test
    fun leftToRight() {
        show(ChapterNavigatorType.HORIZONTAL_LTR)
        buttons()
        compose.setSlider(index = 0, value = 4f)
        events shouldContain "page 3"
        events shouldContain "next"
    }

    @Test
    fun rightToLeftSwapsButtons() {
        show(ChapterNavigatorType.HORIZONTAL_RTL)
        buttons()
        events shouldContain "previous"
    }

    @Test
    @Config(qualifiers = "sw720dp-night")
    fun verticalOnTablets() {
        show(ChapterNavigatorType.VERTICAL_LEFT)
        buttons()
        compose.setSlider(index = 0, value = 6f)
        events shouldContain "page 5"
    }

    @Test
    fun singlePageHasNoSlider() {
        show(ChapterNavigatorType.HORIZONTAL_LTR, total = 1, enabled = false)
        compose.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun verticalSinglePage() {
        show(ChapterNavigatorType.VERTICAL_RIGHT, total = 1)
        compose.onNodeWithContentDescription("Next chapter").performClick()
        events shouldContain "next"
    }

    @Test
    fun previewRenders() {
        compose.setContent { ChapterNavigatorPreview() }
        compose.setSlider(index = 0, value = 3f)
        compose.onNodeWithText("10").assertExists()
    }
}
