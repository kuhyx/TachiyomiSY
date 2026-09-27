package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The frame around the webtoon list: its settings, and pinches and flings handed to the list. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonFrameTest {
    private fun frame(withList: Boolean = true): WebtoonFrame {
        val frame = WebtoonFrame(ApplicationProvider.getApplicationContext())
        if (withList) {
            val list = recycler()
            (list.parent as ViewGroup).removeView(list)
            frame.addView(list)
            list.layout(0, 0, WIDTH, HEIGHT)
        }
        return frame
    }

    private fun scale(factor: Float): ScaleGestureDetector = mockk { every { scaleFactor } returns factor }

    @Test
    fun settingsReachTheList() {
        val frame = frame()
        frame.doubleTapZoom = false
        frame.zoomOutDisabled = true
        frame.recycler!!.doubleTapZoom shouldBe false
        frame.recycler!!.zoomOutDisabled shouldBe true
        val empty = frame(withList = false)
        empty.doubleTapZoom = true
        empty.zoomOutDisabled = false
        empty.recycler.shouldBeNull()
    }

    @Test
    fun pinchIsDelegated() {
        val frame = frame()
        val listener = frame.ScaleListener()
        listener.onScaleBegin(scale(1f)) shouldBe true
        listener.onScale(scale(2f)) shouldBe true
        frame.recycler!!.currentScale shouldBe 2f
        listener.onScaleEnd(scale(1f))
        val empty = frame(withList = false).ScaleListener()
        empty.onScaleBegin(scale(1f)) shouldBe true
        empty.onScale(scale(2f)) shouldBe true
        empty.onScaleEnd(scale(1f))
    }

    @Test
    fun flingIsDelegated() {
        val frame = frame()
        val listener = frame.FlingListener()
        val event = touch(MotionEvent.ACTION_UP, 1f to 1f)
        listener.onDown(event) shouldBe true
        listener.onFling(null, event, 10f, 10f) shouldBe false
        frame.recycler!!.currentScale = 2f
        listener.onFling(event, event, 10f, 10f) shouldBe true
        frame(withList = false).FlingListener().onFling(null, event, 1f, 1f) shouldBe false
    }

    @Test
    fun touchesAreClampedIntoTheList() {
        val frame = frame()
        val event = touch(MotionEvent.ACTION_DOWN, 5_000f to 5_000f)
        frame.dispatchTouchEvent(event)
        (event.x <= WIDTH) shouldBe true
        frame.recycler!!.layout(0, 0, 0, 0)
        frame.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN, 5f to 5f))
        frame(withList = false).dispatchTouchEvent(touch(MotionEvent.ACTION_UP, 5f to 5f))
    }
}
