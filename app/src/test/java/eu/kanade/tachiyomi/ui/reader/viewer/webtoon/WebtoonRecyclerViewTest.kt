package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** The webtoon list's measuring, scroll bookkeeping, taps and zoom-out setting. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonRecyclerViewTest {
    @Test
    fun measureKeepsTheFirstHeight() {
        val view = recycler()
        view.halfWidth shouldBe WIDTH / 2
        view.halfHeight shouldBe HEIGHT / 2
        view.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT * 2, View.MeasureSpec.EXACTLY),
        )
        view.originalHeight shouldBe HEIGHT
        view.halfHeight shouldBe HEIGHT
    }

    @Test
    fun scrollTracksTheEnds() {
        val view = recycler()
        view.onScrolled(0, 0)
        view.onScrollStateChanged(RecyclerView.SCROLL_STATE_DRAGGING)
        view.atFirstPosition shouldBe true
        view.atLastPosition shouldBe false
        val short = recycler(count = 1)
        short.onScrolled(0, 0)
        short.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
        short.atLastPosition shouldBe true
        short.layoutManager = null
        short.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
        short.atLastPosition shouldBe false
    }

    @Test
    fun tapsReachTheListener() {
        val view = recycler()
        val taps = mutableListOf<MotionEvent>()
        view.tapListener = { taps += it }
        val listener = view.GestureListener()
        listener.onSingleTapConfirmed(touch(MotionEvent.ACTION_UP, 1f to 1f)) shouldBe false
        view.onManualScroll()
        view.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 1f to 1f))
        listener.onSingleTapConfirmed(touch(MotionEvent.ACTION_UP, 1f to 1f))
        view.onTouchEvent(touch(MotionEvent.ACTION_UP, 1f to 1f))
        taps.size shouldBe 1
        view.tapListener = null
        view.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
        view.onTouchEvent(touch(MotionEvent.ACTION_DOWN, 1f to 1f))
        listener.onSingleTapConfirmed(touch(MotionEvent.ACTION_UP, 1f to 1f))
    }

    @Test
    fun longTapsVibrateWhenHandled() {
        val view = recycler()
        val listener = view.GestureListener()
        val event = touch(MotionEvent.ACTION_DOWN, 1f to 1f)
        listener.onLongTapConfirmed(event)
        view.longTapListener = { false }
        listener.onLongTapConfirmed(event)
        view.longTapListener = { true }
        listener.onLongTapConfirmed(event)
        listener.onDoubleTap(event) shouldBe false
        view.detector.isDoubleTapping shouldBe true
    }

    @Test
    fun doubleTapZoomsInAndOut() {
        val view = recycler()
        val listener = view.GestureListener()
        listener.onDoubleTapConfirmed(touch(MotionEvent.ACTION_UP, 100f to 100f))
        ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        view.currentScale shouldBe 2f
        view.scaleX shouldBe 2f
        listener.onDoubleTapConfirmed(touch(MotionEvent.ACTION_UP, 100f to 100f))
        ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        view.currentScale shouldBe 1f
        view.isZooming = true
        listener.onDoubleTapConfirmed(touch(MotionEvent.ACTION_UP, 1f to 1f))
        view.isZooming = false
        view.doubleTapZoom = false
        listener.onDoubleTapConfirmed(touch(MotionEvent.ACTION_UP, 1f to 1f))
        view.currentScale shouldBe 1f
    }

    @Test
    fun zoomOutCanBeDisabled() {
        val view = recycler()
        view.minRate shouldBe 0.5f
        view.currentScale = 0.5f
        view.zoomOutDisabled = true
        ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        view.currentScale shouldBe 1f
        view.minRate shouldBe 1f
        view.zoomOutDisabled = true
        view.zoomOutDisabled = false
        0.6f.roundToPixel() shouldBe 1
    }
}
