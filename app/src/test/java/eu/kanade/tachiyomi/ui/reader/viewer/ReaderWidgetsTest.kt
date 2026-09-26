package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.viewer.pager.Pager
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class ReaderWidgetsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun event(action: Int, time: Long = 0L, x: Float = 10f): MotionEvent =
        MotionEvent.obtain(time, time, action, x, 10f, 0)

    @Test
    fun buttonTogglesPagerGestures() {
        val button = ReaderButton(ContextThemeWrapper(context, R.style.Theme_Tachiyomi))
        button.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN))
        val pager = mockk<Pager>(relaxed = true)
        val viewer = mockk<PagerViewer>()
        every { viewer.pager } returns pager
        button.viewer = viewer
        button.dispatchTouchEvent(event(MotionEvent.ACTION_DOWN))
        verify(exactly = 0) { pager.setGestureDetectorEnabled(true) }
        button.dispatchTouchEvent(event(MotionEvent.ACTION_UP))
        verify { pager.setGestureDetectorEnabled(true) }
    }

    @Test
    fun longTapFiresOnlyWhenHeld() {
        val taps = mutableListOf<MotionEvent>()
        val detector = GestureDetectorWithLongTap(
            context,
            object : GestureDetectorWithLongTap.Listener() {
                override fun onLongTapConfirmed(ev: MotionEvent) {
                    taps += ev
                }
            },
        )
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_000L))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        taps.size shouldBe 1
        detector.onTouchEvent(event(MotionEvent.ACTION_UP, time = 10_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 10_050L))
        detector.onTouchEvent(event(MotionEvent.ACTION_DOWN, time = 20_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_MOVE, time = 20_000L, x = 11f))
        detector.onTouchEvent(event(MotionEvent.ACTION_MOVE, time = 20_000L, x = 500f))
        detector.onTouchEvent(event(MotionEvent.ACTION_CANCEL, time = 20_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_POINTER_DOWN, time = 20_000L))
        detector.onTouchEvent(event(MotionEvent.ACTION_OUTSIDE, time = 20_000L))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        taps.size shouldBe 1
    }

    @Test
    fun plainListenerIgnoresLongTaps() {
        GestureDetectorWithLongTap.Listener().onLongTapConfirmed(event(MotionEvent.ACTION_DOWN))
    }
}
