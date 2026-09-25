package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderOverlaysTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun overlaysForEveryInput() {
        var brightness by mutableStateOf(-50)
        compose.setContent {
            Box {
                ReaderContentOverlay(brightness = brightness, color = 0x55FF0000, colorBlendMode = BlendMode.Multiply)
                ReaderContentOverlay(brightness = 10, color = 0x55FF0000, colorBlendMode = null, modifier = Modifier)
                ReaderContentOverlay(brightness = 0, color = null, colorBlendMode = null)
            }
        }
        compose.waitForIdle()
        brightness = -20
        compose.waitForIdle()
    }

    @Test
    fun pageIndicatorNeedsPages() {
        compose.setContent {
            MaterialTheme {
                Box {
                    ReaderPageIndicator(currentPage = 0, totalPages = 5)
                    ReaderPageIndicator(currentPage = 2, totalPages = 0)
                    ReaderPageIndicator(currentPage = 3, totalPages = 7, modifier = Modifier)
                    ReaderPageIndicatorPreview()
                }
            }
        }
        compose.onAllNodesWithText("3 / 7").fetchSemanticsNodes().size shouldBe 2
        compose.onAllNodesWithText("10 / 69").fetchSemanticsNodes().size shouldBe 2
        compose.onNodeWithText("0 / 5").assertDoesNotExist()
    }

    @Test
    fun navigatorTypesKnowTheirAxis() {
        ChapterNavigatorType.entries.map { it.isHorizontal() } shouldBe listOf(true, true, false, false)
    }
}
