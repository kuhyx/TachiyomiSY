package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LAYOUT = "layout"
private const val SCROLLER = "scroller"

/** [FastScrollerLayout] sizes itself to its content and pins the scroller to the end edge. */
@RunWith(RobolectricTestRunner::class)
internal class FastScrollerLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private var contentHeightPx = -1
    private var constraints: Constraints? = null
    private var tick by mutableIntStateOf(0)

    @Test
    fun scrollerSitsAtTheContentEnd() {
        compose.setContent {
            MaterialTheme {
                FastScrollerLayout(
                    modifier = Modifier.testTag(LAYOUT),
                    content = { Box(modifier = Modifier.width(100.dp).height(80.dp)) },
                ) { contentHeight, incoming ->
                    contentHeightPx = contentHeight
                    constraints = incoming
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(with(LocalDensity.current) { contentHeight.toDp() })
                            .testTag(SCROLLER),
                    )
                }
            }
        }
        compose.onNodeWithTag(LAYOUT).assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(80.dp)
        compose.onNodeWithTag(SCROLLER).assertLeftPositionInRootIsEqualTo(90.dp).assertHeightIsEqualTo(80.dp)
        contentHeightPx shouldBe with(compose.density) { 80.dp.roundToPx() }
        checkNotNull(constraints).maxWidth shouldBeGreaterThan 0
    }

    @Test
    fun largestChildDecidesTheSize() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    FastScrollerLayout(
                        modifier = Modifier.testTag(LAYOUT),
                        content = {
                            Box(modifier = Modifier.size(width = 40.dp, height = 90.dp))
                            Box(modifier = Modifier.size(width = 120.dp, height = 30.dp))
                        },
                    ) { _, _ ->
                        Box(modifier = Modifier.size(8.dp).testTag(SCROLLER))
                        Box(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag(LAYOUT).assertWidthIsEqualTo(120.dp).assertHeightIsEqualTo(90.dp)
        compose.onNodeWithTag(SCROLLER).assertLeftPositionInRootIsEqualTo(104.dp)
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithTag(LAYOUT).assertWidthIsEqualTo(120.dp)
    }

    @Test
    fun emptySlotsCollapseToNothing() {
        compose.setContent {
            MaterialTheme {
                FastScrollerLayout(modifier = Modifier.testTag(LAYOUT), content = {}) { contentHeight, _ ->
                    contentHeightPx = contentHeight
                }
            }
        }
        compose.onNodeWithTag(LAYOUT).assertWidthIsEqualTo(0.dp).assertHeightIsEqualTo(0.dp)
        contentHeightPx shouldBe 0
    }
}
