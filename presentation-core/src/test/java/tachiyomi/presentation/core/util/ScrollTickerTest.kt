package tachiyomi.presentation.core.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Test

/** [ScrollTicker] ticks once per consumed scroll along its own axis and consumes nothing itself. */
internal class ScrollTickerTest {
    private val scrolled = MutableSharedFlow<Unit>(replay = 1)

    private fun post(orientation: Orientation, consumed: Offset): Offset =
        ScrollTicker(orientation, scrolled).onPostScroll(consumed, Offset.Zero, NestedScrollSource.UserInput)

    @Test
    fun verticalConsumptionTicks() {
        post(Orientation.Vertical, Offset(0f, 12f)) shouldBe Offset.Zero
        scrolled.replayCache.size shouldBe 1
    }

    @Test
    fun xMoveIgnoredByVerticalTicker() {
        post(Orientation.Vertical, Offset(12f, 0f)) shouldBe Offset.Zero
        scrolled.replayCache.size shouldBe 0
    }

    @Test
    fun horizontalConsumptionTicks() {
        post(Orientation.Horizontal, Offset(-3f, 0f)) shouldBe Offset.Zero
        scrolled.replayCache.size shouldBe 1
    }

    @Test
    fun yMoveIgnoredByHorizontalTicker() {
        post(Orientation.Horizontal, Offset(0f, 40f)) shouldBe Offset.Zero
        scrolled.replayCache.size shouldBe 0
    }

    @Test
    fun styleKeepsItsFields() {
        val style = ScrollbarStyle(
            reverseDirection = true,
            atEnd = false,
            thickness = 4f,
            color = Color.Red,
            alpha = { 0.5f },
        )
        style.reverseDirection shouldBe true
        style.atEnd shouldBe false
        style.thickness shouldBe 4f
        style.alpha() shouldBe 0.5f
    }
}
