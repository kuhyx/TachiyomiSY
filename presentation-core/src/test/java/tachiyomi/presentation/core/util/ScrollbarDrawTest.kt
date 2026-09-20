package tachiyomi.presentation.core.util

import android.view.View
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val TAG = "canvas"
private const val THICKNESS = 4f
private const val OFFSET = 3f

/** [onDrawScrollbar] painted through a real draw pass, once per orientation and direction. */
@RunWith(RobolectricTestRunner::class)
internal class ScrollbarDrawTest {
    @get:Rule
    val compose = createComposeRule()

    private var draws = 0
    private var alphaReads = 0
    private var drawnSize: Size? = null
    private var host: View? = null

    private val overflowing = FakeLazyListLayoutInfo(uniformItems(count = 4, size = 100), 16)
    private val fitting = FakeLazyListLayoutInfo(uniformItems(count = 3, size = 100), 3)

    private fun style(reverse: Boolean, atEnd: Boolean) = ScrollbarStyle(
        reverseDirection = reverse,
        atEnd = atEnd,
        thickness = THICKNESS,
        color = Color.Red,
        alpha = {
            alphaReads += 1
            1f
        },
    )

    private fun setCanvas(orientation: Orientation, style: ScrollbarStyle, metrics: LazyListScrollbarMetrics) {
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 100.dp)
                        .drawWithContent {
                            val draw = onDrawScrollbar(
                                orientation = orientation,
                                style = style,
                                metrics = metrics,
                                positionOffset = OFFSET,
                            )
                            drawnSize = size
                            drawContent()
                            draws += 1
                            draw()
                        }
                        .testTag(TAG),
                )
            }
        }
        compose.onNodeWithTag(TAG).assertIsDisplayed()
        compose.forceDraw(checkNotNull(host))
    }

    @Test
    fun verticalAtTheEnd() {
        val metrics = LazyListScrollbarMetrics(overflowing, Orientation.Vertical, false)
        setCanvas(Orientation.Vertical, style(reverse = false, atEnd = true), metrics)
        draws shouldBeGreaterThan 0
        alphaReads shouldBe draws
        checkNotNull(drawnSize).width shouldBe with(compose.density) { 200.dp.toPx() }
    }

    @Test
    fun verticalReversedAtTheStart() {
        val metrics = LazyListScrollbarMetrics(overflowing, Orientation.Vertical, true)
        setCanvas(Orientation.Vertical, style(reverse = true, atEnd = false), metrics)
        draws shouldBeGreaterThan 0
        alphaReads shouldBe draws
    }

    @Test
    fun horizontalAtTheEnd() {
        val metrics = LazyListScrollbarMetrics(overflowing, Orientation.Horizontal, false)
        setCanvas(Orientation.Horizontal, style(reverse = false, atEnd = true), metrics)
        draws shouldBeGreaterThan 0
        alphaReads shouldBe draws
    }

    @Test
    fun horizontalReversedAtTheStart() {
        val metrics = LazyListScrollbarMetrics(overflowing, Orientation.Horizontal, true)
        setCanvas(Orientation.Horizontal, style(reverse = true, atEnd = false), metrics)
        draws shouldBeGreaterThan 0
        alphaReads shouldBe draws
    }

    @Test
    fun hiddenScrollbarNeverReadsAlpha() {
        val metrics = LazyListScrollbarMetrics(fitting, Orientation.Vertical, false)
        setCanvas(Orientation.Vertical, style(reverse = false, atEnd = true), metrics)
        draws shouldBeGreaterThan 0
        alphaReads shouldBe 0
    }
}
