package mihon.core.designsystem.utils

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A window [widthPx] wide; the height never matters to the width breakpoints. */
private class FixedWindowInfo(widthPx: Int) : WindowInfo {
    override val isWindowFocused: Boolean = true
    override val containerSize: IntSize = IntSize(width = widthPx, height = 1000)
}

@RunWith(RobolectricTestRunner::class)
internal class WindowSizeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun breakpointsAt(widthPx: Int): Pair<Boolean, Boolean> {
        var medium = false
        var expanded = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f),
                LocalWindowInfo provides FixedWindowInfo(widthPx),
            ) {
                medium = isMediumWidthWindow()
                expanded = isExpandedWidthWindow()
            }
        }
        compose.waitForIdle()
        return medium to expanded
    }

    @Test
    fun compactWindow() {
        breakpointsAt(widthPx = 360) shouldBe (false to false)
    }

    @Test
    fun mediumWindow() {
        breakpointsAt(widthPx = 700) shouldBe (true to false)
    }

    @Test
    fun expandedWindow() {
        breakpointsAt(widthPx = 1200) shouldBe (true to true)
    }

    @Test
    fun breakpointsOnTheBoundary() {
        breakpointsAt(widthPx = 600) shouldBe (false to false)
        MediumWidthWindowSize shouldBe 600.dp
        ExpandedWidthWindowSize shouldBe 840.dp
    }
}
