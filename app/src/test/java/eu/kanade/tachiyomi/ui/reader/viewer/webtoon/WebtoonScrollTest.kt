package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Scrolling by distance or by page, with and without animated transitions, and the scroll-driven page change. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonScrollTest {
    private val harness = ReaderActivityHarness(pageCount = 4, viewerFlags = ReadingMode.WEBTOON.flagValue.toLong())
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: WebtoonViewer

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as WebtoonViewer
    }

    @After
    fun tearDown() = harness.stop()

    private fun firstPage(viewer: WebtoonViewer): ReaderPage =
        viewer.adapter.items.filterIsInstance<ReaderPage>().first()

    @Test
    fun distanceScrollsEitherWay() {
        viewer.config.usePageTransitions = false
        viewer.scrollUp()
        viewer.scrollDownBy()
        viewer.config.usePageTransitions = true
        viewer.scrollUp()
        viewer.scrollDownBy()
        viewer.config.usePageTransitions shouldBe true
    }

    @Test
    fun tapByPageJumpsToNextPage() {
        val paged = WebtoonViewer(activity, isContinuous = false, tapByPage = true)
        paged.setChapters(activity.viewModel.state.value.viewerChapters!!)
        val page = firstPage(paged)
        paged.currentPage = page
        paged.config.usePageTransitions = false
        paged.scrollDown()
        paged.config.usePageTransitions = true
        paged.scrollDown()
        paged.currentPage = paged.adapter.items.filterIsInstance<ReaderPage>().last()
        paged.scrollDown()
        paged.currentPage shouldBe paged.adapter.items.filterIsInstance<ReaderPage>().last()
        paged.destroy()
    }

    @Test
    fun continuousScrollsByDistance() {
        viewer.currentPage = firstPage(viewer)
        viewer.scrollDown()
        val continuousByPage = WebtoonViewer(activity, isContinuous = true, tapByPage = true)
        continuousByPage.scrollDown()
        continuousByPage.destroy()
        viewer.currentPage.shouldBeInstanceOf<ReaderPage>()
    }

    @Test
    fun transitionScrollPreloads() {
        val index = viewer.adapter.items.indexOfFirst { it is ChapterTransition }
        viewer.onScrolled(index)
        viewer.currentPage.shouldBeInstanceOf<ChapterTransition>()
        viewer.onScrolled(index)
        viewer.onScrolled(viewer.adapter.items.size + 3)
        viewer.currentPage.shouldBeInstanceOf<ChapterTransition>()
    }
}
