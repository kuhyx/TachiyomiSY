package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderButton
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Splits before any chapter or at the first item, spreads long-tapped or split, and the overlay listener. */
@RunWith(RobolectricTestRunner::class)
internal class PagerAdapterEdgesTest {
    private var harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity
    private val PagerViewer.pages get() = adapter.currentChapter!!.pages!!

    @After
    fun tearDown() = harness.stop()

    private fun viewer(mode: ReadingMode = ReadingMode.LEFT_TO_RIGHT): PagerViewer {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = mode.flagValue.toLong())
        harness.start()
        activity = harness.launch().get()
        return activity.viewModel.state.value.viewer as PagerViewer
    }

    @Test
    fun splitBeforeChaptersIsKept() {
        val viewer = viewer()
        val fresh = PagerViewerAdapter(viewer)
        val page = viewer.pages[1]
        fresh.onPageSplit(page, InsertPage(page))
        fresh.preprocessed.keys shouldBe setOf(1)
    }

    @Test
    fun rightToLeftSplitAtStart() {
        val viewer = viewer(ReadingMode.RIGHT_TO_LEFT)
        val page = viewer.pages[0]
        viewer.adapter.joinedItems.add(0, page to null)
        viewer.adapter.onPageSplit(page, InsertPage(page))
        viewer.adapter.joinedItems.first().first.shouldBeInstanceOf<InsertPage>()
    }

    @Test
    fun spreadSplitsBack() {
        val viewer = viewer()
        val (first, second) = viewer.pages.take(2)
        viewer.adapter.joinedItems.add(viewer.pager.currentItem, first to second)
        viewer.adapter.splitDoublePages(second)
        viewer.adapter.joinedItems.clear()
        viewer.adapter.splitDoublePages(first)
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        viewer.adapter.joinedItems.isEmpty() shouldBe false
    }

    @Test
    fun longTapOnSpreadOffersBoth() {
        val viewer = viewer()
        val (first, second) = viewer.pages.take(2)
        viewer.adapter.joinedItems[viewer.pager.currentItem] = first to second
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        viewer.pager.longTapListener!!(event) shouldBe true
        val dialog = activity.viewModel.state.value.dialog.shouldBeInstanceOf<ReaderViewModel.Dialog.PageActions>()
        dialog.extraPage shouldBe second
        viewer.adapter.joinedItems.clear()
        viewer.pager.longTapListener!!(event) shouldBe false
    }

    @Test
    fun overlayListenerReadsBothFlags() {
        val viewer = viewer()
        viewer.config.navigationOverlayOnStart = true
        viewer.config.navigationModeChangedListener!!()
        viewer.config.navigationOverlayOnStart = false
        viewer.config.forceNavigationOverlay = true
        viewer.config.navigationModeChangedListener!!()
        viewer.config.forceNavigationOverlay = false
        viewer.config.navigationModeChangedListener!!()
        viewer.config.navigationOverlayOnStart shouldBe false
    }

    @Test
    fun buttonWithoutViewer() {
        viewer()
        val button = ReaderButton(activity)
        button.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        button.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 1f, 1f, 0))
        button.viewer shouldBe null
    }
}
