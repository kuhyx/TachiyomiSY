package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Inserting the second half of a split page next to its original, per reading direction, and undoing it. */
@RunWith(RobolectricTestRunner::class)
internal class PagerAdapterSplitTest {
    private var harness = ReaderActivityHarness(pageCount = 3)

    @After
    fun tearDown() = harness.stop()

    private fun adapter(mode: ReadingMode? = null): PagerViewerAdapter {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = mode?.flagValue?.toLong() ?: 0L)
        harness.start()
        val activity: ReaderActivity = harness.launch().get()
        return (activity.viewModel.state.value.viewer as PagerViewer).adapter
    }

    // A page of the chapter being read (the list also holds the neighbouring chapters' pages).
    private fun PagerViewerAdapter.page(index: Int): ReaderPage = currentChapter!!.pages!![index]

    private fun PagerViewerAdapter.inserts() = joinedItems.count { it.first is InsertPage }

    @Test
    fun onlyPagesSplit() {
        val adapter = adapter()
        adapter.onPageSplit("not a page", InsertPage(adapter.page(0)))
        adapter.inserts() shouldBe 0
    }

    @Test
    fun otherChaptersArePutAside() {
        val adapter = adapter()
        val stranger = ReaderPage(1).also { it.chapter = readerChapter(id = 9L) }
        adapter.onPageSplit(stranger, InsertPage(stranger))
        adapter.preprocessed.keys shouldBe setOf(1)
    }

    @Test
    fun rightToLeftInsertsBefore() {
        val adapter = adapter(ReadingMode.RIGHT_TO_LEFT)
        val page = adapter.page(1)
        adapter.onPageSplit(page, InsertPage(page))
        adapter.inserts() shouldBe 1
        adapter.onPageSplit(page, InsertPage(page))
        adapter.inserts() shouldBe 1
        adapter.cleanupPageSplit()
        adapter.inserts() shouldBe 0
    }

    @Test
    fun leftToRightInsertsAfter() {
        val adapter = adapter(ReadingMode.LEFT_TO_RIGHT)
        val page = adapter.page(1)
        adapter.onPageSplit(page, InsertPage(page))
        adapter.inserts() shouldBe 1
        adapter.onPageSplit(page, InsertPage(page))
        adapter.inserts() shouldBe 1
    }

    @Test
    fun verticalInsertsAfter() {
        val adapter = adapter(ReadingMode.VERTICAL)
        val page = adapter.page(0)
        adapter.onPageSplit(page, InsertPage(page))
        adapter.inserts() shouldBe 1
    }

    @Test
    fun doublePagesSplitBack() {
        val adapter = adapter()
        adapter.splitDoublePages(adapter.page(0))
        adapter.splitDoublePages(adapter.page(2))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        // Splitting a page that lies before the one on screen.
        adapter.viewer.pager.currentItem = adapter.joinedItems.indexOfFirst { it.first == adapter.page(2) }
        adapter.splitDoublePages(adapter.page(0))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        adapter.viewer.pager.currentItem = adapter.joinedItems.size + 5
        adapter.splitDoublePages(adapter.page(1))
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        adapter.inserts() shouldBe 0
    }
}
