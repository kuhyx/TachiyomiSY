package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private const val SHOWN_ANCHOR = 0f
private const val HIDDEN_ANCHOR = 500f

internal class SheetNestedScrollTest {
    private val flings = mutableListOf<Float>()

    private val state = AnchoredDraggableState(
        initialValue = 0,
        anchors = DraggableAnchors {
            0 at SHOWN_ANCHOR
            1 at HIDDEN_ANCHOR
        },
    )

    private val connection = state.sheetNestedScrollConnection { flings += it }

    private fun dragSheetDownBy(delta: Float) {
        Snapshot.withMutableSnapshot { state.dispatchRawDelta(delta) }
    }

    @Test
    fun preScrollUpwardTakesSheetFirst() {
        dragSheetDownBy(100f)
        val consumed = connection.onPreScroll(Offset(0f, -30f), NestedScrollSource.UserInput)
        consumed shouldBe Offset(0f, -30f)
        state.offset shouldBe 70f
    }

    @Test
    fun preScrollIgnoresSideEffects() {
        dragSheetDownBy(100f)
        val consumed = connection.onPreScroll(Offset(0f, -30f), NestedScrollSource.SideEffect)
        consumed shouldBe Offset.Zero
        state.offset shouldBe 100f
    }

    @Test
    fun preScrollLeavesDownwardDrags() {
        val consumed = connection.onPreScroll(Offset(0f, 30f), NestedScrollSource.UserInput)
        consumed shouldBe Offset.Zero
        state.offset shouldBe SHOWN_ANCHOR
    }

    @Test
    fun postScrollTakesLeftoverDrag() {
        val consumed = connection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 40f),
            source = NestedScrollSource.UserInput,
        )
        consumed shouldBe Offset(0f, 40f)
        state.offset shouldBe 40f
    }

    @Test
    fun postScrollIgnoresSideEffects() {
        val consumed = connection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 40f),
            source = NestedScrollSource.SideEffect,
        )
        consumed shouldBe Offset.Zero
        state.offset shouldBe SHOWN_ANCHOR
    }

    @Test
    fun preFlingUpwardOffTopFlings() = runTest {
        dragSheetDownBy(100f)
        connection.onPreFling(Velocity(0f, -200f)) shouldBe Velocity(0f, -200f)
        flings shouldBe listOf(-200f)
    }

    @Test
    fun preFlingUpwardAtTopIgnored() = runTest {
        connection.onPreFling(Velocity(0f, -200f)) shouldBe Velocity.Zero
        flings.isEmpty() shouldBe true
    }

    @Test
    fun preFlingDownwardIsIgnored() = runTest {
        dragSheetDownBy(100f)
        connection.onPreFling(Velocity(0f, 200f)) shouldBe Velocity.Zero
        flings.isEmpty() shouldBe true
    }

    @Test
    fun postFlingConsumesUnsettled() = runTest {
        dragSheetDownBy(400f)
        connection.onPostFling(consumed = Velocity.Zero, available = Velocity(0f, 50f)) shouldBe Velocity(0f, 50f)
        flings shouldBe listOf(50f)
    }

    @Test
    fun postFlingPassesOnWhenSettled() = runTest {
        connection.onPostFling(consumed = Velocity.Zero, available = Velocity(0f, 50f)) shouldBe Velocity.Zero
        flings shouldBe listOf(50f)
    }
}
