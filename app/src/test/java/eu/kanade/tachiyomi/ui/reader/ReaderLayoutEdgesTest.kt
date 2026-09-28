package eu.kanade.tachiyomi.ui.reader

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** Reloading into double pages, shifting on chapter load and swapping viewers, with the rarer inputs. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderLayoutEdgesTest {
    private var harness = ReaderActivityHarness(pageCount = 4)

    private val ReaderActivity.pager get() = viewModel.state.value.viewer as PagerViewer

    @After
    fun tearDown() = harness.stop()

    private fun launch(flags: Long = 0L, genres: List<String>? = null): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 4, viewerFlags = flags)
        harness.start()
        if (genres != null) {
            coEvery { harness.vm.getManga.await(10L) } returns harness.vm.manga.copy(ogGenre = genres)
        }
        return harness.launch().get()
    }

    @Test
    fun webtoonCannotReload() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong())
        activity.reloadChapters(doublePages = true, force = true)
        activity.viewModel.state.value.doublePages shouldBe false
    }

    @Test
    fun reloadCountsSpreadPages() {
        val activity = launch()
        val pages = activity.viewModel.state.value.currentChapter!!.pages!!
        pages[0].fullPage = true
        pages[1].isolatedPage = true
        activity.viewModel.updateState { it.copy(currentPage = 2) }
        activity.reloadChapters(doublePages = true, force = true)
        activity.pager.config.shiftDoublePage shouldBe false
        activity.viewModel.updateState { it.copy(currentPage = 1) }
        activity.reloadChapters(doublePages = true, force = true)
        activity.pager.config.shiftDoublePage shouldBe false
        activity.viewModel.updateState { it.copy(currentPage = 3) }
        pages[0].fullPage = false
        activity.reloadChapters(doublePages = true, force = true)
        activity.viewModel.state.value.doublePages shouldBe true
    }

    @Test
    fun reloadWithoutChapters() {
        val activity = launch()
        activity.viewModel.updateState { it.copy(viewerChapters = null, currentPage = 1) }
        activity.reloadChapters(doublePages = true, force = true)
        activity.pager.config.shiftDoublePage shouldBe true
        val unloaded = ViewerChapters(readerChapter(id = 8L), null, null)
        activity.viewModel.updateState { it.copy(viewerChapters = unloaded, currentPage = 2) }
        activity.reloadChapters(doublePages = true, force = true)
        activity.pager.config.shiftDoublePage shouldBe false
    }

    @Test
    fun shiftNeedsTheSameChapter() {
        val activity = launch()
        val chapters = activity.viewModel.state.value.viewerChapters!!
        activity.viewModel.updateState { it.copy(indexChapterToShift = 99L, indexPageToShift = 1) }
        activity.setChapters(chapters)
        activity.viewModel.state.value.indexChapterToShift shouldBe null
        activity.viewModel.updateState { it.copy(indexChapterToShift = 2L, indexPageToShift = null) }
        activity.setChapters(chapters)
        activity.viewModel.state.value.indexChapterToShift shouldBe 2L
    }

    @Test
    fun lastShiftOnUnloadedChapter() {
        val activity = launch()
        every { harness.source.getChapterUrl(any()) } throws IllegalStateException("no url")
        val unloaded = ViewerChapters(readerChapter(id = 8L), null, null)
        unloaded.currChapter.requestedPage = 1
        activity.viewModel.updateState { it.copy(lastShiftDoubleState = false) }
        activity.setChapters(unloaded)
        activity.pager.config.shiftDoublePage shouldBe true
        val spread = activity.viewModel.state.value.viewerChapters!!
        spread.currChapter.pages!![0].isolatedPage = true
        spread.currChapter.requestedPage = 2
        activity.setChapters(spread)
        activity.pager.config.shiftDoublePage shouldBe true
    }

    @Test
    fun automaticLayoutKeepsLastShift() {
        val activity = launch()
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        harness.vm.readerPreferences.showReadingMode.set(false)
        activity.viewModel.updateState { it.copy(lastShiftDoubleState = true) }
        ShadowToast.reset()
        activity.updateViewer()
        activity.pager.config.shiftDoublePage shouldBe true
        ShadowToast.shownToastCount() shouldBe 0
    }

    @Test
    fun autoWebtoonToastIsReplaced() {
        val activity = launch(genres = listOf("Webtoon"))
        ShadowToast.reset()
        activity.updateViewer()
        activity.updateViewer()
        ShadowToast.getTextOfLatestToast() shouldBe "Reading webtoon style"
    }

    @Test
    fun unhandledMotionReachesViews() {
        val activity = launch()
        val viewer = mockk<Viewer>(relaxed = true)
        every { viewer.handleGenericMotionEvent(any()) } returns false
        activity.viewModel.onViewerLoaded(viewer)
        val sink = View(activity).apply { setOnGenericMotionListener { _, _ -> true } }
        activity.addContentView(sink, ViewGroup.LayoutParams(MATCH, MATCH))
        harness.settle()
        val motion = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_SCROLL, 1f, 1f, 0)
        motion.source = InputDevice.SOURCE_MOUSE
        activity.dispatchGenericMotionEvent(motion) shouldBe true
    }
}

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
