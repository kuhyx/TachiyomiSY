package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.requestPreloadChapter
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerTransitionHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonTransitionHolder
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

private const val CHAPTERS = "eu.kanade.tachiyomi.ui.reader.ReaderActivityChaptersKt"

/** The transition pages of both viewers following the next chapter's loading state, with a retry on error. */
@RunWith(RobolectricTestRunner::class)
internal class TransitionHoldersTest {
    private val harness = ReaderActivityHarness(pageCount = 2)
    private lateinit var activity: ReaderActivity
    private val next = readerChapter(id = 3L)

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
        // A real preload would move the chapter's state from an IO thread while the test moves it too.
        mockkStatic(CHAPTERS)
        every { any<ReaderActivity>().requestPreloadChapter(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkStatic(CHAPTERS)
        harness.stop()
    }

    private fun texts(root: ViewGroup): List<String> = root.children.flatMap { child ->
        when (child) {
            is TextView -> sequenceOf(child.text.toString())
            is ViewGroup -> texts(child).asSequence()
            else -> emptySequence()
        }
    }.toList()

    private fun buttons(root: ViewGroup): List<Button> = root.children.flatMap { child ->
        when (child) {
            is Button -> sequenceOf(child)
            is ViewGroup -> buttons(child).asSequence()
            else -> emptySequence()
        }
    }.toList()

    private fun ReaderChapter.moveTo(state: ReaderChapter.State) {
        this.state = state
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun pagerFollowsChapter() {
        val viewer = mockk<PagerViewer>(relaxed = true)
        every { viewer.activity } returns activity
        val holder = PagerTransitionHolder(activity, viewer, ChapterTransition.Next(readerChapter(), next))
        holder.item shouldBe ChapterTransition.Next(readerChapter(), next)
        next.moveTo(ReaderChapter.State.Loading)
        texts(holder) shouldBe listOf("Loading pages…")
        next.moveTo(ReaderChapter.State.Error(IllegalStateException("net")))
        texts(holder).last() shouldBe "Retry"
        buttons(holder).single().performClick()
        verify { activity.requestPreloadChapter(next) }
        next.moveTo(ReaderChapter.State.Error(IllegalStateException()))
        next.moveTo(ReaderChapter.State.Wait)
        texts(holder) shouldBe emptyList()
        activity.setContentView(holder)
        (holder.parent as ViewGroup).removeView(holder)
        PagerTransitionHolder(activity, viewer, ChapterTransition.Prev(readerChapter(), null))
    }

    @Test
    fun webtoonFollowsChapter() {
        val viewer = mockk<WebtoonViewer>(relaxed = true)
        every { viewer.activity } returns activity
        val layout = LinearLayout(activity)
        val holder = WebtoonTransitionHolder(layout, viewer)
        holder.bind(ChapterTransition.Next(readerChapter(), next))
        next.moveTo(ReaderChapter.State.Loading)
        texts(layout) shouldBe listOf("Loading pages…")
        next.moveTo(ReaderChapter.State.Error(IllegalStateException("net")))
        buttons(layout).single().performClick()
        verify { activity.requestPreloadChapter(next) }
        next.moveTo(ReaderChapter.State.Error(IllegalStateException()))
        next.moveTo(ReaderChapter.State.Loaded(emptyList()))
        texts(layout) shouldBe emptyList()
        holder.recycle()
        holder.bind(ChapterTransition.Prev(readerChapter(), null))
    }

    @Test
    fun buttonPausesGestures() {
        val viewer = mockk<PagerViewer>(relaxed = true)
        val button = ReaderButton(activity)
        button.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        button.viewer = viewer
        button.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        button.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 1f, 1f, 0))
        verify { viewer.pager.setGestureDetectorEnabled(false) }
        verify { viewer.pager.setGestureDetectorEnabled(true) }
    }
}
