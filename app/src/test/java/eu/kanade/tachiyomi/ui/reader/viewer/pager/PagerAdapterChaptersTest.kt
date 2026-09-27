package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.viewpager.widget.PagerAdapter
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Which transitions and preprocessed pages [PagerViewerAdapter.setChapters] lays out. */
@RunWith(RobolectricTestRunner::class)
internal class PagerAdapterChaptersTest {
    private val harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var adapter: PagerViewerAdapter

    @Before
    fun setUp() {
        harness.start()
        val activity: ReaderActivity = harness.launch().get()
        adapter = (activity.viewModel.state.value.viewer as PagerViewer).adapter
    }

    @After
    fun tearDown() = harness.stop()

    private fun numbered(id: Long, pages: Int = 2): ReaderChapter = readerChapter(id = 100L + id).also {
        it.chapter.chapter_number = id.toFloat()
        if (pages > 0) loadedPages(it, pages)
    }

    private fun transitions() = adapter.joinedItems.count { it.first is ChapterTransition }

    @Test
    fun gapsForceTransitions() {
        adapter.setChapters(ViewerChapters(numbered(5), numbered(2), numbered(9)), forceTransition = false)
        transitions() shouldBe 2
    }

    @Test
    fun loadedNeighboursJoinUp() {
        adapter.setChapters(ViewerChapters(numbered(5), numbered(4), numbered(6)), forceTransition = false)
        transitions() shouldBe 0
        adapter.setChapters(ViewerChapters(numbered(5), numbered(4), numbered(6)), forceTransition = true)
        transitions() shouldBe 2
    }

    @Test
    fun unloadedKeepTransitions() {
        adapter.setChapters(ViewerChapters(numbered(5), numbered(4, 0), numbered(6, 0)), forceTransition = false)
        transitions() shouldBe 2
    }

    @Test
    fun unloadedChapterHasNoPages() {
        adapter.setChapters(ViewerChapters(numbered(5, 0), null, null), forceTransition = false)
        adapter.joinedItems.size shouldBe 2
    }

    @Test
    fun preprocessedPagesReturn() {
        val chapter = numbered(5, 3)
        val pages = chapter.pages!!
        adapter.preprocessed[0] = InsertPage(pages[0])
        adapter.preprocessed[2] = InsertPage(pages[2])
        adapter.setChapters(ViewerChapters(chapter, null, null), forceTransition = false)
        adapter.joinedItems.count { it.first is InsertPage } shouldBe 2
        adapter.preprocessed.isEmpty() shouldBe true
    }

    @Test
    fun foreignViewsHaveNoPosition() {
        adapter.getItemPosition("not a holder") shouldBe PagerAdapter.POSITION_NONE
        val view = adapter.instantiateItem(adapter.viewer.pager, 0)
        adapter.getItemPosition(view) shouldBe 0
        adapter.setChapters(ViewerChapters(numbered(5), null, null), forceTransition = false)
        adapter.getItemPosition(view) shouldBe PagerAdapter.POSITION_NONE
    }

    @Test
    fun freshAdapterKeepsPosition() {
        val fresh = PagerViewerAdapter(adapter.viewer)
        fresh.setJoinedItems()
        fresh.count shouldBe 0
        fresh.setJoinedItems(useSecondPage = true)
        fresh.count shouldBe 0
    }
}
