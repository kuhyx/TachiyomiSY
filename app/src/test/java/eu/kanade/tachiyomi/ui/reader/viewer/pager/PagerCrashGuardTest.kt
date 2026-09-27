package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager.widget.PagerAdapter
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowVelocityTracker

/** What the next velocity sample throws, one per touch, as the base pager's touch handling does on bad streams. */
internal object TouchThrows {
    val queue: ArrayDeque<RuntimeException> = ArrayDeque()
}

/** Throws [TouchThrows] from the sample every base-pager touch handler records first; records normally otherwise. */
@Implements(VelocityTracker::class)
internal class ThrowingVelocityShadow : ShadowVelocityTracker() {
    @Implementation
    override fun addMovement(event: MotionEvent) {
        TouchThrows.queue.removeFirstOrNull()?.let { throw it }
        super.addMovement(event)
    }
}

/** The reader pager swallows what the base pager throws on inconsistent touch streams. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ThrowingVelocityShadow::class])
internal class PagerCrashGuardTest {
    private val pager = Pager(ApplicationProvider.getApplicationContext()).apply {
        adapter = object : PagerAdapter() {
            override fun getCount(): Int = 2

            override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

            override fun instantiateItem(container: ViewGroup, position: Int): Any =
                View(container.context).also(container::addView)
        }
    }

    private fun event(action: Int) = MotionEvent.obtain(0L, 0L, action, 1f, 1f, 0)

    @After
    fun tearDown() = TouchThrows.queue.clear()

    @Test
    fun interceptSwallowsBadPointers() {
        TouchThrows.queue += IllegalArgumentException("pointerIndex out of range")
        pager.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN)) shouldBe false
        TouchThrows.queue.isEmpty() shouldBe true
    }

    @Test
    fun touchSwallowsEachKnownCrash() {
        TouchThrows.queue += NullPointerException()
        TouchThrows.queue += IndexOutOfBoundsException()
        TouchThrows.queue += IllegalArgumentException()
        repeat(3) { pager.onTouchEvent(event(MotionEvent.ACTION_DOWN)) shouldBe false }
        TouchThrows.queue.isEmpty() shouldBe true
    }
}
