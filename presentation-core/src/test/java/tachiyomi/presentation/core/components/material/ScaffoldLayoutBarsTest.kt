package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
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

/** The bars and the snackbar: where they land and what the body is told to keep clear of. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
internal class ScaffoldLayoutBarsTest {
    @get:Rule
    val compose = createComposeRule()

    private var padding = PaddingValues(0.dp)

    private fun root(): DpRect = compose.onRoot().getBoundsInRoot()

    @Test
    fun nothingButInsets() {
        compose.setContent { ScaffoldHarness(onPadding = { padding = it }) }
        padding.calculateTopPadding() shouldBe TOP_INSET.dp
        padding.calculateBottomPadding() shouldBe BOTTOM_INSET.dp
        padding.calculateStartPadding(LayoutDirection.Ltr) shouldBe LEFT_INSET.dp
        padding.calculateEndPadding(LayoutDirection.Ltr) shouldBe RIGHT_INSET.dp
    }

    @Test
    fun topAndStartBars() {
        compose.setContent { ScaffoldHarness(hasTopBar = true, hasStartBar = true, onPadding = { padding = it }) }
        compose.onNodeWithTag("top")
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(root().width)
        compose.onNodeWithTag("start").assertLeftPositionInRootIsEqualTo(0.dp)
        padding.calculateTopPadding() shouldBe BAR_HEIGHT
        padding.calculateStartPadding(LayoutDirection.Ltr) shouldBe START_BAR_WIDTH
        padding.calculateEndPadding(LayoutDirection.Ltr) shouldBe RIGHT_INSET.dp
    }

    @Test
    fun bottomBarWithoutFab() {
        compose.setContent { ScaffoldHarness(bottomBar = { BottomBar() }, onPadding = { padding = it }) }
        compose.onNodeWithTag("bottom").assertTopPositionInRootIsEqualTo(root().height - BAR_HEIGHT)
        padding.calculateBottomPadding() shouldBe BAR_HEIGHT
    }

    @Test
    fun emptyBottomBarCountsAsNone() {
        compose.setContent { ScaffoldHarness(bottomBar = { BottomBar(height = 0.dp) }, onPadding = { padding = it }) }
        compose.onNodeWithTag("bottom").assertExists().assertTopPositionInRootIsEqualTo(root().height)
        padding.calculateBottomPadding() shouldBe BOTTOM_INSET.dp
    }

    @Test
    fun snackbarAlone() {
        compose.setContent { ScaffoldHarness(hasSnackbar = true) }
        val root = root()
        val free = root.width - LEFT_INSET.dp - RIGHT_INSET.dp - SNACKBAR_WIDTH
        compose.onNodeWithTag("snackbar")
            .assertLeftPositionInRootIsEqualTo(LEFT_INSET.dp + free / 2)
            .assertTopPositionInRootIsEqualTo(root.height - SNACKBAR_HEIGHT - BOTTOM_INSET.dp)
    }

    @Test
    fun snackbarAboveBottomBar() {
        compose.setContent { ScaffoldHarness(hasSnackbar = true, bottomBar = { BottomBar() }) }
        compose.onNodeWithTag("snackbar")
            .assertTopPositionInRootIsEqualTo(root().height - SNACKBAR_HEIGHT - BAR_HEIGHT)
    }

    @Test
    fun snackbarAboveFab() {
        compose.setContent { ScaffoldHarness(hasSnackbar = true, fab = { Fab() }) }
        val fabOffset = BOTTOM_INSET.dp + FAB_SIZE + FAB_SPACING.dp
        compose.onNodeWithTag("snackbar")
            .assertTopPositionInRootIsEqualTo(root().height - SNACKBAR_HEIGHT - fabOffset)
    }

    @Test
    fun snackbarBesideStartBar() {
        compose.setContent { ScaffoldHarness(hasSnackbar = true, hasStartBar = true) }
        val free = root().width - LEFT_INSET.dp - RIGHT_INSET.dp - START_BAR_WIDTH - SNACKBAR_WIDTH
        compose.onNodeWithTag("snackbar").assertLeftPositionInRootIsEqualTo(LEFT_INSET.dp + free / 2)
    }
}
