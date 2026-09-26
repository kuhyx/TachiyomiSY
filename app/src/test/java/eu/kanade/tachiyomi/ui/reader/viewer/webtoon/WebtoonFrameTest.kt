package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebtoonFrameTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val frame = WebtoonFrame(context)

    private fun withRecycler(): WebtoonRecyclerView {
        val recycler = WebtoonRecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }
        frame.addView(recycler, ViewGroup.LayoutParams(400, 800))
        frame.layout(0, 0, 400, 800)
        recycler.layout(0, 0, 400, 800)
        return recycler
    }

    @Test
    fun settersWithoutRecycler() {
        frame.recycler shouldBe null
        frame.doubleTapZoom = false
        frame.zoomOutDisabled = true
        frame.doubleTapZoom shouldBe false
        frame.zoomOutDisabled shouldBe true
        frame.dispatchTouchEvent(motion(MotionEvent.ACTION_DOWN))
    }

    @Test
    fun settersReachRecycler() {
        val recycler = withRecycler()
        frame.doubleTapZoom = false
        frame.zoomOutDisabled = true
        recycler.doubleTapZoom shouldBe false
        recycler.zoomOutDisabled shouldBe true
    }

    @Test
    fun touchesAreClampedIntoRecycler() {
        withRecycler()
        val event = motion(MotionEvent.ACTION_DOWN, 900f, 900f)
        frame.dispatchTouchEvent(event)
        event.x shouldBe 399f
        val collapsed = WebtoonFrame(context)
        collapsed.addView(WebtoonRecyclerView(context))
        val outside = motion(MotionEvent.ACTION_DOWN, 900f, 900f)
        collapsed.dispatchTouchEvent(outside)
        outside.x shouldBe 900f
    }

    @Test
    fun scaleEventsReachRecycler() {
        val scale = frame.ScaleListener()
        val detector = mockk<ScaleGestureDetector>()
        every { detector.scaleFactor } returns 2f
        scale.onScaleBegin(detector) shouldBe true
        scale.onScale(detector) shouldBe true
        scale.onScaleEnd(detector)
        val recycler = withRecycler()
        scale.onScaleBegin(detector)
        scale.onScale(detector)
        recycler.currentScale shouldBe 2f
        scale.onScaleEnd(detector)
    }

    @Test
    fun flingsReachRecycler() {
        val fling = frame.FlingListener()
        val event = motion(MotionEvent.ACTION_UP)
        fling.onDown(event) shouldBe true
        fling.onFling(null, event, 10f, 10f) shouldBe false
        val recycler = withRecycler()
        fling.onFling(event, event, 10f, 10f) shouldBe false
        recycler.currentScale = 2f
        fling.onFling(event, event, 10f, 10f) shouldBe true
        finishAnimations()
    }
}
