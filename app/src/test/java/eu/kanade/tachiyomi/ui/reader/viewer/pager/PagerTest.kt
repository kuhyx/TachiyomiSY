package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.ui.reader.viewer.GestureDetectorWithLongTap
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The reader's view pager: its tap listeners, the switchable gesture detector and crash guards. */
@RunWith(RobolectricTestRunner::class)
internal class PagerTest {
    private val pager = Pager(ApplicationProvider.getApplicationContext())

    private fun event(action: Int) = MotionEvent.obtain(0L, 0L, action, 1f, 1f, 0)

    // The pager keeps its gesture listener private; tests drive it directly.
    private fun listener(): GestureDetectorWithLongTap.Listener {
        val field = Pager::class.java.getDeclaredField("gestureListener")
        field.isAccessible = true
        return field.get(pager) as GestureDetectorWithLongTap.Listener
    }

    @Test
    fun tapsReachTheListener() {
        val taps = mutableListOf<MotionEvent>()
        listener().onSingleTapConfirmed(event(MotionEvent.ACTION_UP)) shouldBe true
        pager.tapListener = { taps += it }
        listener().onSingleTapConfirmed(event(MotionEvent.ACTION_UP)) shouldBe true
        taps.size shouldBe 1
    }

    @Test
    fun longTapsVibrateWhenHandled() {
        var asked = 0
        listener().onLongTapConfirmed(event(MotionEvent.ACTION_DOWN))
        pager.longTapListener = {
            asked++
            asked > 1
        }
        listener().onLongTapConfirmed(event(MotionEvent.ACTION_DOWN))
        listener().onLongTapConfirmed(event(MotionEvent.ACTION_DOWN))
        asked shouldBe 2
    }

    @Test
    fun touchesSurviveWithoutAdapter() {
        pager.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN))
        pager.setGestureDetectorEnabled(false)
        pager.dispatchTouchEvent(event(MotionEvent.ACTION_UP))
        pager.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN)) shouldBe false
        pager.onTouchEvent(event(MotionEvent.ACTION_UP))
        pager.onTouchEvent(event(MotionEvent.ACTION_MOVE))
        pager.executeKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)) shouldBe false
    }

    @Test
    fun restoringKeepsThePosition() {
        val state = pager.onSaveInstanceState()
        pager.onRestoreInstanceState(state)
        pager.isRestoring shouldBe false
        pager.currentItem shouldBe 0
    }
}
