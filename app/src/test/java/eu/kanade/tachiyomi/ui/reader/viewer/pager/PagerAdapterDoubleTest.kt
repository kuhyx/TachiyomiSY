package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Re-laying out chapters as the double-page and shift settings change underneath the adapter. */
@RunWith(RobolectricTestRunner::class)
internal class PagerAdapterDoubleTest {
    private val harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var adapter: PagerViewerAdapter
    private val chapters by lazy {
        val chapter = readerChapter(id = 50L).also { loadedPages(it, 5) }
        ViewerChapters(chapter, null, null)
    }

    @Before
    fun setUp() {
        harness.start()
        val activity: ReaderActivity = harness.launch().get()
        adapter = (activity.viewModel.state.value.viewer as PagerViewer).adapter
    }

    @After
    fun tearDown() = harness.stop()

    private fun relayout() = adapter.setChapters(chapters, forceTransition = false)

    @Test
    fun shiftingOnAndOffRelays() {
        val config = adapter.viewer.config
        config.doublePages = true
        config.shiftDoublePage = true
        relayout()
        adapter.joinedItems.count { it.second is ReaderPage } shouldBe 2
        config.shiftDoublePage = false
        relayout()
        adapter.joinedItems.count { it.second is ReaderPage } shouldBe 2
        config.doublePages = false
        relayout()
        adapter.joinedItems.count { it.second != null } shouldBe 0
    }

    @Test
    fun enablingDoublesAlone() {
        adapter.viewer.config.doublePages = true
        relayout()
        adapter.joinedItems.count { it.second is ReaderPage } shouldBe 2
    }

    @Test
    fun spreadsBuildHolders() {
        adapter.viewer.config.doublePages = true
        relayout()
        val views = (0 until adapter.count).map { adapter.instantiateItem(adapter.viewer.pager, it) }
        views.first().shouldBeInstanceOf<PagerTransitionHolder>()
        views.filterIsInstance<PagerPageHolder>().size shouldBe 3
    }
}
