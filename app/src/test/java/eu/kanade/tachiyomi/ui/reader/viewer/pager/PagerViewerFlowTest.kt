package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.MotionEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.viewpager.widget.ViewPager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.navigationModePager
import eu.kanade.tachiyomi.ui.reader.setting.readWithLongTap
import eu.kanade.tachiyomi.ui.reader.showMenu
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PagerViewerFlowTest {

    // Compose runs on a test clock: an animating page spinner never lets an auto-advancing clock idle.
    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun tap(xFraction: Float, yFraction: Float) {
        val x = viewer.pager.width * xFraction
        val y = viewer.pager.height * yFraction
        viewer.pager.tapListener!!.invoke(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, x, y, 0))
    }

    @Test
    fun tapZonesDrive() {
        val start = viewer.pager.currentItem
        tap(0.9f, 0.5f)
        tap(0.1f, 0.5f)
        viewer.pager.currentItem shouldBe start
        tap(0.5f, 0.5f)
        activity.viewModel.state.value.menuVisible shouldBe true
        harness.vm.readerPreferences.navigationModePager.set(1)
        harness.settle()
        tap(0.5f, 0.9f)
        tap(0.5f, 0.1f)
        viewer.pager.currentItem shouldBe start
    }

    @Test
    fun longTapOpensPageActions() {
        val listener = viewer.pager.longTapListener!!
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        listener(event) shouldBe true
        activity.viewModel.state.value.dialog.shouldBeInstanceOf<ReaderViewModel.Dialog.PageActions>()
        harness.vm.readerPreferences.readWithLongTap.set(false)
        harness.settle()
        listener(event) shouldBe false
        activity.showMenu()
        listener(event) shouldBe true
        viewer.pager.setCurrentItem(viewer.adapter.count - 1, false)
        listener(event) shouldBe false
    }

    @Test
    fun chaptersWaitForIdle() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.pagerListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING)
        viewer.isIdle shouldBe false
        viewer.setChapters(chapters)
        viewer.pagerListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)
        viewer.isIdle shouldBe true
        val single = ViewerChapters(chapters.currChapter, null, chapters.nextChapter)
        viewer.isIdle = false
        viewer.setChapters(single)
        viewer.isIdle = true
        viewer.isIdle = true
    }

    @Test
    fun pageSelectionHidesMenu() {
        activity.showMenu()
        viewer.pagerListener.onPageSelected(0)
        activity.viewModel.state.value.menuVisible shouldBe false
        activity.isScrollingThroughPages = true
        activity.showMenu()
        viewer.pagerListener.onPageSelected(1)
        activity.viewModel.state.value.menuVisible shouldBe true
        viewer.pager.isRestoring = true
        viewer.pagerListener.onPageSelected(2)
    }

    @Test
    fun transitionsRequestPreload() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.onTransitionSelected(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
        viewer.onTransitionSelected(ChapterTransition.Next(chapters.currChapter, null))
        activity.viewModel.state.value.menuVisible shouldBe true
        viewer.onTransitionSelected(ChapterTransition.Prev(chapters.currChapter, null))
    }

    @Test
    fun configListenersReact() {
        harness.vm.readerPreferences.dualPageSplitPaged.set(true)
        harness.settle()
        harness.vm.readerPreferences.dualPageSplitPaged.set(false)
        harness.settle()
        harness.vm.readerPreferences.cropBorders.set(true)
        harness.settle()
        harness.vm.readerPreferences.readerTheme.set(0)
        harness.settle()
        viewer.config.dualPageSplit shouldBe false
    }
}
