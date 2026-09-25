package eu.kanade.presentation.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.kevinnzou.web.LoadingState
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebViewLoadingIndicatorTest {
    @get:Rule
    val compose = createComposeRule()

    private fun bars(state: LoadingState, animated: Boolean? = null): Int {
        compose.setContent {
            Box {
                if (animated == null) WebViewLoadingIndicator(state) else WebViewLoadingIndicator(state, animated)
            }
        }
        compose.waitForIdle()
        val spinning = compose.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        val halfway = compose.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.5f, 0f..1f)))
        return spinning.fetchSemanticsNodes().size + halfway.fetchSemanticsNodes().size
    }

    @Test
    fun initializingIsIndeterminate() {
        bars(LoadingState.Initializing) shouldBe 1
    }

    @Test
    fun loadingShowsProgress() {
        bars(LoadingState.Loading(0.5f)) shouldBe 1
    }

    @Test
    fun animatedLoadingShowsProgress() {
        bars(LoadingState.Loading(0.5f), animated = true) shouldBe 1
    }

    @Test
    fun finishedHidesTheBar() {
        bars(LoadingState.Finished) shouldBe 0
    }
}
