package tachiyomi.presentation.core.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext

private const val FRAME_NANOS = 16_000_000L

/** A frame clock that answers every frame at once, 16 ms apart, and counts them. */
internal class ImmediateFrameClock : MonotonicFrameClock {
    var frames: Int = 0
        private set
    private var nanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frames += 1
        nanos += FRAME_NANOS
        return onFrame(nanos)
    }
}

/** Animates a fresh [Animatable] from 0 dp to 4 dp and returns how many frames it took (0 = snapped). */
internal suspend fun animateFrames(from: Interaction?, to: Interaction?): Int {
    val clock = ImmediateFrameClock()
    val animatable = Animatable(0.dp, Dp.VectorConverter)
    withContext(clock) { animatable.animateElevation(target = 4.dp, from = from, to = to) }
    animatable.value shouldBe 4.dp
    return clock.frames
}
