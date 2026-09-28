package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Page turns at the ends and without panning, transitions kept or not, and pages found through their pair. */
@RunWith(RobolectricTestRunner::class)
internal class PagerViewerEdgesTest {
    private val harness = ReaderActivityHarness(
        pageCount = 3,
        viewerFlags = ReadingMode.LEFT_TO_RIGHT.flagValue.toLong(),
    )
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer
    private val pages get() = viewer.adapter.currentChapter!!.pages!!

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun turnsWithoutPanning() {
        harness.vm.readerPreferences.navigateToPan.set(false)
        harness.settle()
        val start = viewer.pager.currentItem
        viewer.moveRight()
        viewer.pager.currentItem shouldBe start + 1
        harness.vm.readerPreferences.navigateToPan.set(true)
        harness.settle()
        viewer.moveLeft()
        viewer.pager.currentItem shouldBe start
    }

    @Test
    fun endsStopTheTurns() {
        viewer.pager.currentItem = viewer.adapter.count - 1
        viewer.moveRight()
        viewer.pager.currentItem shouldBe viewer.adapter.count - 1
        viewer.pager.currentItem = 0
        viewer.moveLeft()
        viewer.pager.currentItem shouldBe 0
    }

    @Test
    fun transitionPagesDoNotPan() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.currentPage = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        val start = viewer.pager.currentItem
        viewer.moveRight()
        viewer.moveLeft()
        viewer.pager.currentItem shouldBe start
    }

    @Test
    fun transitionsKeptWhereShown() {
        viewer.config.alwaysShowChapterTransition = false
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.setChaptersInternal(chapters)
        val transition = viewer.adapter.joinedItems.indexOfFirst { it.first is ChapterTransition }
        viewer.pager.currentItem = transition
        viewer.setChaptersInternal(chapters)
        viewer.adapter.joinedItems.any { it.first is ChapterTransition } shouldBe true
    }

    @Test
    fun secondOfPairIsFound() {
        val first = pages[0]
        val second = pages[1]
        viewer.adapter.joinedItems.add(0, first to second)
        viewer.adapter.notifyDataSetChanged()
        viewer.moveToReaderPage(second)
        viewer.pager.currentItem shouldBe 0
        viewer.getPageHolder(second)
        viewer.moveToReaderPage(ReaderPage(9).also { it.chapter = readerChapter(id = 9L) })
        viewer.pager.currentItem shouldBe 0
    }

    @Test
    fun outOfRangeChangeIsIgnored() {
        val before = viewer.currentPage
        viewer.onPageChange(viewer.adapter.joinedItems.size + 3)
        viewer.onPageChange(viewer.pager.currentItem)
        viewer.currentPage shouldBe before
    }

    @Test
    fun fromPrevTransitionGoesBack() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.currentPage = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        val position = viewer.adapter.joinedItems.indexOfFirst { it.first == pages[0] }
        viewer.onPageChange(position)
        viewer.currentPage shouldBe pages[0]
    }

    @Test
    fun preloadRulesByOrigin() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        val next = chapters.nextChapter!!
        viewer.currentPage = ChapterTransition.Next(chapters.currChapter, next)
        viewer.checkAllowPreload(ReaderPage(0).also { it.chapter = next }) shouldBe true
        viewer.currentPage = pages[0]
        viewer.checkAllowPreload(ReaderPage(0).also { it.chapter = readerChapter(id = 40L) }) shouldBe false
        viewer.checkAllowPreload(ReaderPage(0).also { it.chapter = next }) shouldBe true
    }

    @Test
    fun unloadedPageSelectionIsSkipped() {
        val stray = ReaderPage(0).also { it.chapter = readerChapter(id = 41L) }
        viewer.onReaderPageSelected(stray, allowPreload = true, forward = true, hasExtraPage = false)
        viewer.updateShifting()
        viewer.pager.currentItem = 0
        viewer.updateShifting()
        viewer.getShiftedPage() shouldBe null
    }
}
