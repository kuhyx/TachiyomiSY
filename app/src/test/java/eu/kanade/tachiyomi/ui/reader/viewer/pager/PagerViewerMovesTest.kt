package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.canPanLeft
import eu.kanade.tachiyomi.ui.reader.viewer.canPanRight
import eu.kanade.tachiyomi.ui.reader.viewer.panLeft
import eu.kanade.tachiyomi.ui.reader.viewer.panRight
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PANNING = "eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImagePanningKt"

/** Page turns, pans instead of turns, chapters handed over on idle, and the double-page entry points. */
@RunWith(RobolectricTestRunner::class)
internal class PagerViewerMovesTest {
    private val harness = ReaderActivityHarness(
        pageCount = 3,
        viewerFlags = ReadingMode.LEFT_TO_RIGHT.flagValue.toLong(),
    )
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() {
        unmockkStatic(PANNING)
        harness.stop()
    }

    @Test
    fun nextAndPreviousTurn() {
        val start = viewer.pager.currentItem
        viewer.moveToNext()
        viewer.pager.currentItem shouldBe start + 1
        viewer.moveToPrevious()
        viewer.pager.currentItem shouldBe start
    }

    @Test
    fun zoomedPagesPanFirst() {
        mockkStatic(PANNING)
        every { any<ReaderPageImageView>().canPanRight() } returns true
        every { any<ReaderPageImageView>().canPanLeft() } returns true
        every { any<ReaderPageImageView>().panRight() } just runs
        every { any<ReaderPageImageView>().panLeft() } just runs
        val start = viewer.pager.currentItem
        viewer.moveRight()
        viewer.moveLeft()
        viewer.pager.currentItem shouldBe start
        verify { any<ReaderPageImageView>().panRight() }
        verify { any<ReaderPageImageView>().panLeft() }
    }

    @Test
    fun leftTurnsWhenPanningIsOff() {
        harness.vm.readerPreferences.navigateToPan.set(false)
        harness.settle()
        viewer.moveToNext()
        val start = viewer.pager.currentItem
        viewer.moveLeft()
        viewer.pager.currentItem shouldBe start - 1
    }

    // Before its first layout, a viewer handed a chapter whose pages are not loaded yet stays hidden.
    @Test
    fun unloadedChapterHidesPager() {
        val fresh = L2RPagerViewer(activity)
        fresh.config.alwaysShowChapterTransition = false
        fresh.setChapters(ViewerChapters(readerChapter(id = 70L), null, null))
        fresh.pager.isVisible shouldBe false
        fresh.destroy()
    }

    @Test
    fun onePageChapterPreloadsNext() {
        val single = readerChapter(id = 60L).also { loadedPages(it, 1) }
        val next = readerChapter(id = 61L)
        viewer.isIdle = false
        viewer.setChapters(ViewerChapters(single, null, next))
        viewer.isIdle = true
        viewer.adapter.nextTransition?.to shouldBe next
        viewer.isIdle = false
        viewer.setChapters(ViewerChapters(readerChapter(id = 62L), null, null))
        viewer.isIdle = true
        viewer.adapter.nextTransition?.to shouldBe null
    }

    @Test
    fun splitsGoThroughTheAdapter() {
        val page = viewer.adapter.currentChapter!!.pages!![1]
        viewer.onPageSplit(page, InsertPage(page))
        harness.settle()
        viewer.adapter.joinedItems.count { it.first is InsertPage } shouldBe 1
        viewer.config.doublePages = true
        viewer.splitDoublePages(page)
        harness.settle()
        viewer.adapter.joinedItems.count { it.first is InsertPage } shouldBe 0
    }

    @Test
    fun shiftingPicksAPage() {
        val page = viewer.adapter.currentChapter!!.pages!![2]
        viewer.updateShifting(page)
        viewer.getShiftedPage() shouldBe page
        viewer.updateShifting()
        viewer.getShiftedPage() shouldBe viewer.adapter.joinedItems[viewer.pager.currentItem].first as? ReaderPage
    }

    @Test
    fun insertsAndStrangersSkipped() {
        val page = viewer.adapter.currentChapter!!.pages!!.last()
        viewer.onReaderPageSelected(InsertPage(page), allowPreload = true, forward = true, hasExtraPage = false)
        val start = viewer.pager.currentItem
        viewer.moveToPage(ReaderPage(99).also { it.chapter = page.chapter })
        viewer.pager.currentItem shouldBe start
    }
}
