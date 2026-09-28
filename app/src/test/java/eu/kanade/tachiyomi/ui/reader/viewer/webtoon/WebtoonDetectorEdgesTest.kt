package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** The zoom detector's rarer gestures: other pointers leaving, quick-scale ends, drags at the list's end. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonDetectorEdgesTest {
    private val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)

    @Test
    fun secondPointerLeaving() {
        val view = recycler()
        view.detector.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 10f to 10f))
        view.detector.onTouchEvent(touch(pointerUp, 10f to 10f, 50f to 50f))
        view.detector.isDoubleTapping shouldBe false
    }

    @Test
    fun quickScaleEndDoesNotZoom() {
        val view = recycler()
        view.detector.isDoubleTapping = true
        view.detector.isQuickScaling = true
        view.detector.onTouchEvent(touch(MotionEvent.ACTION_UP, 10f to 10f))
        view.currentScale shouldBe 1f
    }

    @Test
    fun doubleTapDragStillMoves() {
        val view = recycler()
        view.currentScale = 2f
        view.atLastPosition = true
        view.detector.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 100f to 100f))
        view.detector.isDoubleTapping = true
        view.detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 102f to 200f))
        view.detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 103f to 260f))
        (view.y > 0f) shouldBe true
    }

    @Test
    fun flingAtTheEndMovesDown() {
        val view = recycler()
        view.currentScale = 2f
        view.atLastPosition = true
        view.zoomFling(0, 100) shouldBe true
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        view.atLastPosition shouldBe true
    }
}
