package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.tapOutsidePopup
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChapterDownloadIndicatorTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<ChapterDownloadAction>()

    private fun setIndicator(state: Download.State, progress: Int = 0, enabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                ChapterDownloadIndicator(
                    enabled = enabled,
                    downloadStateProvider = { state },
                    downloadProgressProvider = { progress },
                    onClick = { actions += it },
                )
            }
        }
    }

    private fun indicator(): SemanticsNodeInteraction = compose.onAllNodes(hasClickAction()).onFirst()

    @Test
    fun notDownloadedStarts() {
        setIndicator(Download.State.NOT_DOWNLOADED)
        indicator().performClick()
        indicator().performTouchInput { longClick() }
        actions shouldContainExactly listOf(ChapterDownloadAction.START, ChapterDownloadAction.START_NOW)
    }

    @Test
    fun queuedOffersStartNowAndCancel() {
        setIndicator(Download.State.QUEUE)
        indicator().performClick()
        compose.onNodeWithText("Start downloading now").performClick()
        indicator().performClick()
        compose.onNodeWithText("Cancel").performClick()
        indicator().performTouchInput { longClick() }
        indicator().performClick()
        compose.tapOutsidePopup()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
        actions shouldContainExactly listOf(
            ChapterDownloadAction.START_NOW,
            ChapterDownloadAction.CANCEL,
            ChapterDownloadAction.CANCEL,
        )
    }

    @Test
    fun downloadingAtZeroSpins() {
        setIndicator(Download.State.DOWNLOADING)
        indicator().assertExists()
    }

    @Test
    fun downloadingBelowHalf() {
        setIndicator(Download.State.DOWNLOADING, progress = 30)
        compose.mainClock.advanceTimeBy(FILL_MS)
        indicator().assertExists()
    }

    @Test
    fun downloadingPastHalf() {
        setIndicator(Download.State.DOWNLOADING, progress = 80)
        compose.mainClock.advanceTimeBy(FILL_MS)
        indicator().assertExists()
    }

    @Test
    fun downloadedOffersDelete() {
        setIndicator(Download.State.DOWNLOADED)
        indicator().performClick()
        compose.onNodeWithText("Delete").performClick()
        indicator().performTouchInput { longClick() }
        compose.tapOutsidePopup()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        actions shouldContainExactly listOf(ChapterDownloadAction.DELETE)
    }

    @Test
    fun errorRetries() {
        setIndicator(Download.State.ERROR)
        indicator().performClick()
        indicator().performTouchInput { longClick() }
        actions shouldContainExactly listOf(ChapterDownloadAction.START, ChapterDownloadAction.START)
    }

    @Test
    fun disabledIgnoresTaps() {
        setIndicator(Download.State.ERROR, enabled = false)
        indicator().assertIsNotEnabled()
    }

    private companion object {
        const val FILL_MS = 2000L
    }
}
