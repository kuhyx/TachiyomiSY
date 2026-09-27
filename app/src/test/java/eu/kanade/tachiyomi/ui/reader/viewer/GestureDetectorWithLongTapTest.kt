package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Long taps fire after the timeout unless the finger moves, lifts or a second pointer lands. */
@RunWith(RobolectricTestRunner::class)
internal class GestureDetectorWithLongTapTest {
    private val longTaps = mutableListOf<MotionEvent>()
    private val detector = GestureDetectorWithLongTap(
        ApplicationProvider.getApplicationContext(),
        object : GestureDetectorWithLongTap.Listener() {
            override fun onLongTapConfirmed(ev: MotionEvent) {
                longTaps += ev
            }
        },
    )

    // Events far enough apart in time that a down is never taken for the second tap of a double tap.
    private fun event(action: Int, time: Long, x: Float = 1f) = MotionEvent.obtain(time, time, action, x, 1f, 0)

    private fun waitLongTap() =
        ShadowLooper.idleMainLooper(ViewConfiguration.getLongPressTimeout() + 10L, TimeUnit.MILLISECONDS)

    @Test
    fun heldDownConfirmsLongTap() {
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_MOVE, time = 10_010L))
        waitLongTap()
        longTaps.size shouldBe 1
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 20_000L))
        longTaps.size shouldBe 1
    }

    @Test
    fun movingCancelsLongTap() {
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_MOVE, time = 10_010L, x = 500f))
        waitLongTap()
        longTaps.size shouldBe 0
    }

    @Test
    fun liftAndSecondFingerCancel() {
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_UP, time = 10_010L))
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_020L))
        detector.onTouchEvent(event(MotionEvent.ACTION_CANCEL, time = 10_030L))
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 20_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_POINTER_DOWN, time = 20_010L))
        detector.onTouchEvent(event(MotionEvent.ACTION_OUTSIDE, time = 20_020L))
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
        longTaps.size shouldBe 0
    }

    @Test
    fun baseListenerIgnoresLongTaps() {
        GestureDetectorWithLongTap.Listener().onLongTapConfirmed(event(MotionEvent.ACTION_DOWN, time = 1L))
        longTaps.size shouldBe 0
    }
}
