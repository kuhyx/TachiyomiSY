package tachiyomi.presentation.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val HOST = "host"
private const val TOLERANCE = 0.01f

/** The thumb's building blocks, composed on their own. */
@RunWith(RobolectricTestRunner::class)
internal class FastScrollerThumbTest {
    @get:Rule
    val compose = createComposeRule()

    private var geometry: ThumbGeometry? = null
    private var alpha: Animatable<Float, AnimationVector1D>? = null
    private var ticks: MutableSharedFlow<Unit>? = null
    private var isAllowed = true
    private val deltas = mutableListOf<Float>()
    private val source = MutableInteractionSource()
    private var tick by mutableIntStateOf(0)
    private var thumbAlpha by mutableFloatStateOf(1f)

    private fun setThumb(dragged: Boolean, scrolling: Boolean) {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = Modifier.size(200.dp).testTag(HOST)) {
                        ScrollerThumb(
                            thumbOffsetY = 20f,
                            onDrag = { deltas += it },
                            dragInteractionSource = source,
                            isThumbDragged = dragged,
                            isScrollInProgress = scrolling,
                            alpha = thumbAlpha,
                            thumbColor = Color.Red,
                            horizontalPadding = 4.dp,
                            endContentPadding = 2.dp,
                        )
                    }
                }
            }
        }
    }

    private fun dragThumb() {
        compose.onNodeWithTag(HOST).performTouchInput {
            down(Offset(10f, 40f))
            moveBy(Offset(0f, 60f))
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun geometryFromPaddings() {
        compose.setContent {
            geometry = rememberThumbGeometry(
                contentHeight = 600,
                topContentPadding = 10.dp,
                bottomContentPadding = 20.dp,
                afterContentPadding = 30,
            )
        }
        val density = compose.density.density
        val measured = checkNotNull(geometry)
        measured.thumbTopPadding shouldBe (10 * density plusOrMinus TOLERANCE)
        measured.thumbBottomPadding shouldBe (20 * density plusOrMinus TOLERANCE)
        measured.heightPx shouldBe (600 - 30 * density - 30 plusOrMinus TOLERANCE)
        measured.trackHeightPx shouldBe (measured.heightPx - 48 * density plusOrMinus TOLERANCE)
    }

    @Test
    fun alphaShowsOnATickThenFades() {
        compose.setContent {
            val flow = rememberScrolledTicks()
            ticks = flow
            alpha = rememberThumbAlpha(flow) { isAllowed }
        }
        val animatable = checkNotNull(alpha)
        animatable.value shouldBe 0f
        compose.runOnIdle { checkNotNull(ticks).tryEmit(Unit) shouldBe true }
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        animatable.value shouldBe 1f
        compose.mainClock.advanceTimeBy(THUMB_GONE_MS)
        animatable.value shouldBe 0f
    }

    @Test
    fun alphaStaysHiddenWhenNotAllowed() {
        isAllowed = false
        compose.setContent {
            val flow = rememberScrolledTicks()
            ticks = flow
            alpha = rememberThumbAlpha(flow) { isAllowed }
        }
        compose.runOnIdle { checkNotNull(ticks).tryEmit(Unit) shouldBe true }
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        checkNotNull(alpha).value shouldBe 0f
    }

    @Test
    fun visibleIdleThumbDrags() {
        setThumb(dragged = false, scrolling = false)
        dragThumb()
        deltas.shouldNotBeEmpty()
        deltas.sum() shouldBeGreaterThan 0f
    }

    @Test
    fun hiddenThumbIgnoresDrags() {
        thumbAlpha = 0f
        setThumb(dragged = false, scrolling = false)
        dragThumb()
        deltas.shouldBeEmpty()
    }

    @Test
    fun scrollingListBlocksDrags() {
        setThumb(dragged = false, scrolling = true)
        dragThumb()
        deltas.shouldBeEmpty()
    }

    @Test
    fun draggedThumbKeepsDragging() {
        setThumb(dragged = true, scrolling = false)
        dragThumb()
        deltas.shouldNotBeEmpty()
    }

    @Test
    fun recompositionKeepsTheThumb() {
        setThumb(dragged = false, scrolling = false)
        compose.runOnIdle { tick += 1 }
        compose.waitForIdle()
        compose.runOnIdle { thumbAlpha = 0.5f }
        compose.waitForIdle()
        dragThumb()
        deltas.shouldNotBeEmpty()
    }

    @Test
    fun thumbConstants() {
        ThumbLength shouldBe 48.dp
        ThumbThickness shouldBe 12.dp
        ThumbVisibilityDuration.inWholeMilliseconds shouldBe 2_000L
        ThumbScrollSampling.inWholeMilliseconds shouldBe 100L
        ThumbFadeOutAnimationSpec.durationMillis shouldBe 250
    }
}
