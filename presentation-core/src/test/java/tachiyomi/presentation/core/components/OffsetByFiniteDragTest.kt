package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val TAG = "dragged"

/** [offsetByFiniteDrag] follows a laid-out drag offset and stays put while the anchors are unknown. */
@RunWith(RobolectricTestRunner::class)
internal class OffsetByFiniteDragTest {
    @get:Rule
    val compose = createComposeRule()

    private fun topWithOffset(offset: Float): Dp {
        val state = mockk<AnchoredDraggableState<Int>>()
        every { state.offset } returns offset
        compose.setContent {
            Box {
                Box(modifier = Modifier.size(10.dp).offsetByFiniteDrag(state).testTag(TAG))
            }
        }
        return compose.onNodeWithTag(TAG).getBoundsInRoot().top
    }

    @Test
    fun finiteOffsetMovesTheSheet() {
        topWithOffset(40.4f) shouldBe 40.dp
    }

    @Test
    fun nanOffsetLeavesTheSheet() {
        topWithOffset(Float.NaN) shouldBe 0.dp
    }

    @Test
    fun swipeDismissThresholdIs56Dp() {
        SwipeDismissThreshold shouldBe 56.dp
    }

    @Test
    fun infiniteOffsetLeavesTheSheet() {
        topWithOffset(Float.POSITIVE_INFINITY) shouldBe 0.dp
    }
}
