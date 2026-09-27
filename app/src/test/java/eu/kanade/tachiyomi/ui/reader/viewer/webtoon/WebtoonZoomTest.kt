package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Pinch zoom, flings and drags of the zoomed webtoon list. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonZoomTest {
    private fun settle() = ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)

    @Test
    fun positionsClampToTheZoom() {
        val view = recycler()
        view.currentScale = 0.5f
        view.getPositionX(50f) shouldBe 0f
        view.getPositionY(50f) shouldBe (HEIGHT / 2 - view.halfHeight).toFloat()
        view.currentScale = 2f
        view.getPositionX(1_000f) shouldBe WIDTH / 2f
        view.getPositionY(-1_000f) shouldBe -HEIGHT / 2f
    }

    @Test
    fun pinchScalesAndClamps() {
        val view = recycler()
        view.onScale(2f)
        view.currentScale shouldBe 2f
        view.onScale(10f)
        view.currentScale shouldBe 3f
        view.onScale(0.1f)
        view.currentScale shouldBe 0.5f
        view.layoutParams.height shouldBe HEIGHT * 2
        view.onScale(2f)
        view.x shouldBe 0f
        view.y shouldBe 0f
    }

    @Test
    fun scaleEndSnapsBack() {
        val view = recycler()
        view.onScaleBegin()
        view.detector.isQuickScaling shouldBe false
        view.detector.isDoubleTapping = true
        view.onScaleBegin()
        view.detector.isQuickScaling shouldBe true
        view.onScaleEnd()
        view.scaleX = 0.2f
        view.onScaleEnd()
        settle()
        view.currentScale shouldBe 0.5f
    }

    @Test
    fun flingMovesOnlyWhenZoomed() {
        val view = recycler()
        view.zoomFling(100, 100) shouldBe false
        view.currentScale = 2f
        view.zoomFling(100, 100) shouldBe true
        settle()
        view.atFirstPosition = true
        view.zoomFling(0, 100) shouldBe true
        settle()
        view.zoomFling(0, 0) shouldBe true
    }

    @Test
    fun dragScrollsZoomedContent() {
        val view = recycler()
        view.zoomScrollBy(0, 0)
        view.currentScale = 2f
        view.zoomScrollBy(10, -10)
        view.x shouldBe 10f
        view.y shouldBe -10f
    }

    @Test
    fun detectorDragsPastSlop() {
        val view = recycler()
        view.currentScale = 2f
        view.atFirstPosition = true
        val detector = view.detector
        detector.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 100f to 100f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 102f to 102f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 200f to 150f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 60f to 40f))
        (view.x > 0f) shouldBe true
        detector.onTouchEvent(touch(MotionEvent.ACTION_UP, 60f to 40f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_CANCEL, 60f to 40f))
    }

    @Test
    fun detectorQuickScaleAndPointers() {
        val view = recycler()
        val detector = view.detector
        detector.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 10f to 10f))
        val second = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        detector.onTouchEvent(touch(second, 10f to 10f, 50f to 50f))
        detector.isDoubleTapping = true
        detector.isQuickScaling = true
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 20f to 20f, 60f to 60f)) shouldBe true
        detector.isQuickScaling = false
        detector.onTouchEvent(touch(MotionEvent.ACTION_UP, 20f to 20f))
        settle()
        view.currentScale shouldBe 2f
    }

    @Test
    fun lostPointerIsIgnored() {
        val view = recycler()
        val detector = view.detector
        val second = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        detector.onTouchEvent(touch(second, 10f to 10f, 50f to 50f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 20f to 20f)) shouldBe false
        view.currentScale = 0.5f
        detector.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 10f to 10f))
        detector.onTouchEvent(touch(MotionEvent.ACTION_MOVE, 20f to 20f))
        view.x shouldBe 0f
    }
}
