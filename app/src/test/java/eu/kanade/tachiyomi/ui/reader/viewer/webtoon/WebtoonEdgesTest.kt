package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The webtoon viewer's rarer inputs: transitions under a long tap, chapters without pages, paging taps. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonEdgesTest {
    private val harness = ReaderActivityHarness(pageCount = 4, viewerFlags = ReadingMode.WEBTOON.flagValue.toLong())
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: WebtoonViewer

    private fun launch() {
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as WebtoonViewer
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun transitionIsNoLongTapPage() {
        launch()
        viewer.recycler.scrollToPosition(0)
        harness.settle()
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
        viewer.recycler.longTapListener!!(event) shouldBe false
    }

    @Test
    fun overlayListenerReadsBothFlags() {
        launch()
        viewer.config.navigationOverlayOnStart = true
        viewer.config.navigationModeChangedListener!!()
        viewer.config.navigationOverlayOnStart = false
        viewer.config.forceNavigationOverlay = true
        viewer.config.navigationModeChangedListener!!()
        viewer.config.forceNavigationOverlay shouldBe true
    }

    @Test
    fun freshViewerWaitsForPages() {
        launch()
        val fresh = WebtoonViewer(activity)
        fresh.config.alwaysShowChapterTransition = false
        fresh.currentPage = ChapterTransition.Prev(readerChapter(id = 30L), null)
        fresh.setChapters(ViewerChapters(readerChapter(id = 31L), null, null))
        fresh.adapter.items.size shouldBe 2
        fresh.onPageSelected(ReaderPage(0).also { it.chapter = readerChapter(id = 32L) }, allowPreload = true)
        fresh.destroy()
    }

    @Test
    fun pageLeavesTransitionsOptional() {
        launch()
        val fresh = WebtoonViewer(activity)
        fresh.config.alwaysShowChapterTransition = false
        fresh.currentPage = ReaderPage(0)
        fresh.setChapters(ViewerChapters(readerChapter(id = 34L), null, null))
        fresh.adapter.items.size shouldBe 2
        fresh.destroy()
    }

    @Test
    fun loneChapterGetsBothEnds() {
        launch()
        viewer.adapter.setChapters(ViewerChapters(readerChapter(id = 33L), null, null), forceTransition = false)
        viewer.adapter.items.size shouldBe 2
    }

    @Test
    fun tapByPageOnlyWhenPaged() {
        launch()
        val page = viewer.adapter.items.filterIsInstance<ReaderPage>().first()
        val continuous = WebtoonViewer(activity, isContinuous = true, tapByPage = true)
        continuous.currentPage = page
        continuous.scrollDown()
        val paged = WebtoonViewer(activity, isContinuous = false, tapByPage = false)
        paged.currentPage = page
        paged.scrollDown()
        paged.currentPage shouldBe page
        continuous.destroy()
        paged.destroy()
    }

    @Test
    fun flatListTouchIsLeftAlone() {
        val frame = WebtoonFrame(ApplicationProvider.getApplicationContext())
        val list = recycler()
        (list.parent as ViewGroup).removeView(list)
        frame.addView(list)
        list.layout(0, 0, WIDTH, 0)
        val event = touch(MotionEvent.ACTION_DOWN, 5f to 50f)
        frame.dispatchTouchEvent(event)
        event.y shouldBe 50f
    }
}
