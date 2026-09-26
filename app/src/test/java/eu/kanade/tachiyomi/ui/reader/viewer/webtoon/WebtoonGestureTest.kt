package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebtoonGestureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val recycler = WebtoonRecyclerView(context).apply {
        layoutParams = ViewGroup.LayoutParams(400, 800)
        layoutManager = LinearLayoutManager(context)
    }
    private val listener = recycler.GestureListener()

    @Test
    fun tapsReachListener() {
        val taps = mutableListOf<MotionEvent>()
        listener.onSingleTapConfirmed(motion(MotionEvent.ACTION_UP)) shouldBe false
        recycler.tapListener = { taps += it }
        listener.onSingleTapConfirmed(motion(MotionEvent.ACTION_UP))
        recycler.onManualScroll()
        recycler.onTouchEvent(motion(MotionEvent.ACTION_DOWN))
        listener.onSingleTapConfirmed(motion(MotionEvent.ACTION_UP))
        taps.size shouldBe 1
        recycler.onTouchEvent(motion(MotionEvent.ACTION_UP))
        recycler.onTouchEvent(motion(MotionEvent.ACTION_MOVE))
    }

    @Test
    fun longTapsNeedListener() {
        listener.onLongTapConfirmed(motion(MotionEvent.ACTION_DOWN))
        recycler.longTapListener = { false }
        listener.onLongTapConfirmed(motion(MotionEvent.ACTION_DOWN))
        var handled = 0
        recycler.longTapListener = {
            handled++
            true
        }
        listener.onLongTapConfirmed(motion(MotionEvent.ACTION_DOWN))
        handled shouldBe 1
    }

    @Test
    fun doubleTapZoomsInAndOut() {
        listener.onDoubleTap(motion(MotionEvent.ACTION_DOWN)) shouldBe false
        recycler.detector.isDoubleTapping shouldBe true
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_UP))
        finishAnimations()
        recycler.currentScale shouldBe 2f
        listener.onDoubleTapConfirmed(motion(MotionEvent.ACTION_UP))
        finishAnimations()
        recycler.currentScale shouldBe 1f
        recycler.doubleTapZoom = false
        listener.onDoubleTapConfirmed(motion(MotionEvent.ACTION_UP))
        recycler.doubleTapZoom = true
        recycler.isZooming = true
        listener.onDoubleTapConfirmed(motion(MotionEvent.ACTION_UP))
    }

    @Test
    fun quickScaleSkipsDoubleTap() {
        recycler.detector.isDoubleTapping = true
        recycler.onScaleBegin()
        recycler.detector.isQuickScaling shouldBe true
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE)) shouldBe true
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_UP))
        recycler.detector.isDoubleTapping shouldBe false
        recycler.onScaleBegin()
        recycler.detector.isQuickScaling shouldBe false
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_CANCEL))
    }

    @Test
    fun zoomedDragMovesPastSlop() {
        recycler.currentScale = 2f
        recycler.halfWidth = 200
        recycler.halfHeight = 400
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 100f, 100f))
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE, 101f, 101f))
        recycler.x shouldBe 0f
        recycler.atFirstPosition = true
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE, 160f, 180f))
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE, 170f, 190f))
        (recycler.x > 0f) shouldBe true
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_CANCEL))
        recycler.atFirstPosition = false
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 100f, 100f))
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE, 100f, 200f))
    }

    @Test
    fun otherPointersAreTracked() {
        val down = motion(MotionEvent.ACTION_DOWN)
        recycler.detector.onTouchEvent(down)
        val properties = arrayOf(
            MotionEvent.PointerProperties().apply { id = 0 },
            MotionEvent.PointerProperties().apply { id = 5 },
        )
        val coords = arrayOf(MotionEvent.PointerCoords(), MotionEvent.PointerCoords())
        val pointerDown = MotionEvent.obtain(
            0L,
            0L,
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            2,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
        recycler.detector.onTouchEvent(pointerDown)
        recycler.detector.onTouchEvent(motion(MotionEvent.ACTION_MOVE)) shouldBe false
    }
}
