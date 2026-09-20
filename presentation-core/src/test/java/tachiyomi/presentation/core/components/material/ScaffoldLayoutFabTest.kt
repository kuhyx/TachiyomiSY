package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Where the FAB lands, and what it does to the body's padding. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
internal class ScaffoldLayoutFabTest {
    @get:Rule
    val compose = createComposeRule()

    private var padding = PaddingValues(0.dp)

    private fun root(): DpRect = compose.onRoot().getBoundsInRoot()

    @Test
    fun endInLtr() {
        compose.setContent { ScaffoldHarness(fab = { Fab() }, onPadding = { padding = it }) }
        val root = root()
        compose.onNodeWithTag("fab")
            .assertLeftPositionInRootIsEqualTo(root.width - FAB_SPACING.dp - FAB_SIZE - RIGHT_INSET.dp)
            .assertTopPositionInRootIsEqualTo(root.height - BOTTOM_INSET.dp - FAB_SIZE - FAB_SPACING.dp)
        padding.calculateTopPadding() shouldBe TOP_INSET.dp
        padding.calculateBottomPadding() shouldBe BOTTOM_INSET.dp + FAB_SIZE + FAB_SPACING.dp
        padding.calculateStartPadding(LayoutDirection.Ltr) shouldBe LEFT_INSET.dp
        padding.calculateEndPadding(LayoutDirection.Ltr) shouldBe RIGHT_INSET.dp
    }

    @Test
    fun endInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ScaffoldHarness(hasStartBar = true, fab = { Fab() }, onPadding = { padding = it })
            }
        }
        val root = root()
        compose.onNodeWithTag("fab").assertLeftPositionInRootIsEqualTo(FAB_SPACING.dp + LEFT_INSET.dp)
        compose.onNodeWithTag("start").assertLeftPositionInRootIsEqualTo(root.width - START_BAR_WIDTH)
        padding.calculateStartPadding(LayoutDirection.Rtl) shouldBe START_BAR_WIDTH
        padding.calculateEndPadding(LayoutDirection.Rtl) shouldBe LEFT_INSET.dp
    }

    @Test
    fun center() {
        compose.setContent { ScaffoldHarness(fabPosition = FabPosition.Center, fab = { Fab() }) }
        val free = root().width - LEFT_INSET.dp - RIGHT_INSET.dp - FAB_SIZE
        compose.onNodeWithTag("fab").assertLeftPositionInRootIsEqualTo(LEFT_INSET.dp + free / 2)
    }

    @Test
    fun centerBesideStartBar() {
        compose.setContent {
            ScaffoldHarness(fabPosition = FabPosition.Center, hasStartBar = true, fab = { Fab() })
        }
        val free = root().width - LEFT_INSET.dp - RIGHT_INSET.dp - START_BAR_WIDTH - FAB_SIZE
        compose.onNodeWithTag("fab").assertLeftPositionInRootIsEqualTo(LEFT_INSET.dp + free / 2)
    }

    @Test
    fun aboveTheBottomBar() {
        compose.setContent {
            ScaffoldHarness(fab = { Fab() }, bottomBar = { BottomBar() }, onPadding = { padding = it })
        }
        val root = root()
        compose.onNodeWithTag("bottom").assertTopPositionInRootIsEqualTo(root.height - BAR_HEIGHT)
        compose.onNodeWithTag("fab")
            .assertTopPositionInRootIsEqualTo(root.height - BAR_HEIGHT - FAB_SIZE - FAB_SPACING.dp)
        padding.calculateBottomPadding() shouldBe BAR_HEIGHT + FAB_SIZE + FAB_SPACING.dp
    }

    @Test
    fun zeroWidthFabIsNotPlaced() {
        compose.setContent {
            ScaffoldHarness(
                fab = { TaggedBox(tag = "fab", width = 0.dp, height = 0.dp) },
                onPadding = { padding = it },
            )
        }
        compose.onNodeWithTag("fab").assertExists()
        padding.calculateBottomPadding() shouldBe BOTTOM_INSET.dp
    }

    @Test
    fun zeroHeightFabIsNotPlaced() {
        compose.setContent {
            ScaffoldHarness(
                fab = { TaggedBox(tag = "fab", width = 10.dp, height = 0.dp) },
                onPadding = { padding = it },
            )
        }
        compose.onNodeWithTag("fab").assertExists().assertLeftPositionInRootIsEqualTo(0.dp)
        padding.calculateBottomPadding() shouldBe BOTTOM_INSET.dp
    }
}
