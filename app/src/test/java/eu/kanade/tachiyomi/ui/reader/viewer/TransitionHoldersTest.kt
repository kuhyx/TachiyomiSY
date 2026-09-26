package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.TestPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerTransitionHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonTransitionHolder
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class TransitionHoldersTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 2)
    private lateinit var activity: ReaderActivity
    private val from = readerChapter(id = 8L)
    private val to = readerChapter(id = 9L)

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
        activity = harness.launch().get()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun state(state: ReaderChapter.State) {
        to.state = state
        harness.settle()
    }

    @Test
    fun pagerHolderFollowsState() {
        val viewer = activity.viewModel.state.value.viewer as PagerViewer
        PagerTransitionHolder(activity, viewer, ChapterTransition.Prev(from, null)).item shouldBe
            ChapterTransition.Prev(from, null)
        val holder = PagerTransitionHolder(activity, viewer, ChapterTransition.Next(from, to))
        val pages = holder.getChildAt(1) as LinearLayout
        state(ReaderChapter.State.Loading)
        pages.childCount shouldBe 2
        state(ReaderChapter.State.Error(IllegalStateException()))
        pages.getChildAt(1).performClick()
        state(ReaderChapter.State.Error(IllegalStateException("x")))
        state(ReaderChapter.State.Loaded(emptyList()))
        pages.childCount shouldBe 0
        val root = activity.window.decorView as ViewGroup
        root.addView(holder)
        root.removeView(holder)
    }

    @Test
    fun webtoonHolderFollowsState() {
        val holder = WebtoonTransitionHolder(LinearLayout(activity), WebtoonViewer(activity))
        holder.bind(ChapterTransition.Prev(from, null))
        holder.bind(ChapterTransition.Next(from, to))
        val pages = holder.layout.getChildAt(1) as LinearLayout
        state(ReaderChapter.State.Loading)
        pages.isVisible shouldBe true
        state(ReaderChapter.State.Error(IllegalStateException()))
        pages.getChildAt(1).performClick()
        state(ReaderChapter.State.Error(IllegalStateException("x")))
        state(ReaderChapter.State.Wait)
        pages.isVisible shouldBe false
        holder.recycle()
    }

    @Test
    fun transitionViewFlags() {
        val view = ReaderTransitionView(activity)
        val downloads = harness.vm.downloadManager
        view.bind(ChapterTransition.Next(from, to), downloads, null)
        val manga = harness.vm.manga
        view.bind(ChapterTransition.Next(from, null), downloads, manga)
        view.bind(ChapterTransition.Next(from, to), downloads, manga)
        from.pageLoader = TestPageLoader()
        view.bind(ChapterTransition.Next(from, to), downloads, manga.copy(source = LocalSource.ID))
        from.pageLoader = TestPageLoader().apply { isLocal = false }
        view.bind(ChapterTransition.Next(from, to), downloads, manga)
    }
}
