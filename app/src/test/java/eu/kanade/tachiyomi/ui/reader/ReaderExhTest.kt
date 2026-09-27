package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.loader.boostPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** Boosting the current page and retrying every failed one, from the reader's EH bar. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderExhTest {
    private val harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity
    private val http = mockk<HttpPageLoader>(relaxed = true)
    private val viewer get() = activity.viewModel.state.value.viewer as PagerViewer
    private val pages get() = activity.viewModel.state.value.viewerChapters!!.currChapter.pages!!

    @Before
    fun setUp() {
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoaderQueueKt")
        every { http.boostPage(any()) } just runs
        activity = harness.launch().get()
    }

    @After
    fun tearDown() = harness.stop()

    private fun current(): ReaderPage = pages[0].also { viewer.currentPage = it }

    private fun boosted(): String {
        activity.exhBoostPage()
        return ShadowToast.getTextOfLatestToast()
    }

    @Test
    fun boostNeedsAPage() {
        viewer.currentPage = null
        boosted() shouldBe "This page cannot be boosted (invalid page)!"
        activity.viewModel.updateState { it.copy(viewer = null) }
        ShadowToast.reset()
        activity.exhBoostPage()
        ShadowToast.getTextOfLatestToast().shouldBeNull()
    }

    @Test
    fun boostExplainsThePageState() {
        val page = current()
        page.status = Page.State.Error(IllegalStateException())
        boosted() shouldBe "Page failed to load, press the retry button instead!"
        page.status = Page.State.LoadPage
        boosted() shouldBe "This page is already downloading!"
        page.status = Page.State.DownloadImage
        boosted() shouldBe "This page is already downloading!"
        page.status = Page.State.Ready
        boosted() shouldBe "This page has already been downloaded!"
    }

    @Test
    fun queuedPageIsBoosted() {
        val page = current()
        page.status = Page.State.Queue
        boosted() shouldBe "This page cannot be boosted (invalid page loader)!"
        page.chapter.pageLoader = http
        boosted() shouldBe "Boosted current page!"
        verify { http.boostPage(page) }
    }

    @Test
    fun retryAllRequeuesFailedPages() {
        val other = mockk<PageLoader>(relaxed = true)
        val first = current()
        first.chapter.pageLoader = http
        pages.forEach { it.status = Page.State.Error(IllegalStateException()) }
        pages[2].status = Page.State.Ready
        activity.exhRetryAll()
        ShadowToast.getTextOfLatestToast() shouldBe "Retrying 2 failed pages…"
        verify { http.boostPage(first) }
        verify { http.retryPage(pages[1]) }
        first.chapter.pageLoader = other
        first.status = Page.State.Error(IllegalStateException())
        activity.exhRetryAll()
        verify { other.retryPage(first) }
    }

    @Test
    fun ehPagesGetNewUrls() {
        val eh = mockk<CatalogueSource>(relaxed = true) { every { id } returns EH_SOURCE_ID }
        every { harness.vm.sourceManager.get(any()) } returns eh
        val page = current().also { it.imageUrl = "https://old" }
        page.status = Page.State.Error(IllegalStateException())
        activity.exhRetryAll()
        page.imageUrl.shouldBeNull()
        every { harness.vm.sourceManager.get(any()) } returns null
        page.imageUrl = "https://kept"
        page.status = Page.State.Error(IllegalStateException())
        activity.exhRetryAll()
        page.imageUrl shouldBe "https://kept"
        activity.viewModel.updateState { it.copy(viewerChapters = null) }
        activity.exhRetryAll()
        ShadowToast.getTextOfLatestToast() shouldBe "Retrying 0 failed pages…"
    }
}
