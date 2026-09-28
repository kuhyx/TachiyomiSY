package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.loader.boostPage
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** The EH bar's retry and boost when the chapter, manga or current page is missing, and in the webtoon viewer. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderExhEdgesTest {
    private var harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity
    private val http = mockk<HttpPageLoader>(relaxed = true)
    private val pages get() = activity.viewModel.state.value.viewerChapters!!.currChapter.pages!!

    private fun launch(mode: ReadingMode = ReadingMode.LEFT_TO_RIGHT) {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = mode.flagValue.toLong())
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoaderQueueKt")
        every { http.boostPage(any()) } just runs
        activity = harness.launch().get()
    }

    @After
    fun tearDown() = harness.stop()

    private fun toastAfter(block: () -> Unit): String? {
        ShadowToast.reset()
        block()
        return ShadowToast.getTextOfLatestToast()
    }

    @Test
    fun retryWithoutPagesOrManga() {
        launch()
        val loaded = activity.viewModel.state.value.viewerChapters!!
        activity.viewModel.updateState { it.copy(viewerChapters = ViewerChapters(readerChapter(id = 9L), null, null)) }
        toastAfter { activity.exhRetryAll() } shouldBe "Retrying 0 failed pages…"
        activity.viewModel.updateState { it.copy(viewerChapters = loaded, manga = null) }
        val other = mockk<PageLoader>(relaxed = true)
        pages[1].chapter.pageLoader = other
        pages[1].status = Page.State.Error(IllegalStateException())
        toastAfter { activity.exhRetryAll() } shouldBe "Retrying 1 failed page…"
        verify { other.retryPage(pages[1]) }
    }

    @Test
    fun retryCurrentOtherLoader() {
        launch()
        val viewer = activity.viewModel.state.value.viewer as PagerViewer
        viewer.currentPage = pages[0]
        pages[0].chapter.pageLoader = null
        pages[0].status = Page.State.Error(IllegalStateException())
        toastAfter { activity.exhRetryAll() } shouldBe "Retrying 1 failed page…"
        pages[0].status shouldBe Page.State.Queue
    }

    @Test
    fun webtoonPageIsBoosted() {
        launch(ReadingMode.WEBTOON)
        val viewer = activity.viewModel.state.value.viewer as WebtoonViewer
        viewer.currentPage = pages[1]
        pages[1].chapter.pageLoader = http
        pages[1].status = Page.State.Queue
        toastAfter { activity.exhBoostPage() } shouldBe "Boosted current page!"
        verify { http.boostPage(pages[1]) }
    }

    @Test
    fun transitionIsNoBoostablePage() {
        launch(ReadingMode.WEBTOON)
        val viewer = activity.viewModel.state.value.viewer as WebtoonViewer
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.currentPage = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        toastAfter { activity.exhBoostPage() } shouldBe "This page cannot be boosted (invalid page)!"
    }

    @Test
    fun unknownPageIndexIsNoPage() {
        launch()
        val viewer = activity.viewModel.state.value.viewer as PagerViewer
        viewer.currentPage = ReaderPage(7).also { it.chapter = pages[0].chapter }
        toastAfter { activity.exhBoostPage() } shouldBe "This page cannot be boosted (invalid page)!"
        viewer.currentPage = pages[0]
        activity.viewModel.updateState { it.copy(viewerChapters = null) }
        toastAfter { activity.exhBoostPage() } shouldBe "This page cannot be boosted (invalid page)!"
    }
}
