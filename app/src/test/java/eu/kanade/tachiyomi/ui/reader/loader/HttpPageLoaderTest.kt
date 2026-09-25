package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.aggressivePageLoading
import eu.kanade.tachiyomi.ui.reader.setting.readerInstantRetry
import exh.source.EH_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test

internal class HttpPageLoaderTest {

    private val harness = HttpLoaderHarness()

    @Test
    fun pagesFromCacheReindexed() {
        runBlocking {
            every { harness.chapterCache.getPageListFromCache(any()) } returns listOf(Page(5, "/a", "https://a"))
            val loader = harness.loader()
            loader.isLocal shouldBe false
            val pages = loader.getPages()
            pages.single().index shouldBe 0
            pages.single().imageUrl shouldBe "https://a"
            loader.queue.shouldBeEmpty()
            loader.isLocal = true
            loader.isLocal shouldBe true
            loader.recycle()
        }
    }

    @Test
    fun cacheMissGoesToNetwork() {
        runBlocking {
            harness.readerPreferences.aggressivePageLoading.set(true)
            every { harness.chapterCache.getPageListFromCache(any()) } throws IllegalStateException("miss")
            coEvery { harness.source.getPageList(any()) } returns listOf(Page(0, "/a"), Page(1, "/b"))
            val loader = harness.loader()
            loader.getPages().map { it.url } shouldBe listOf("/a", "/b")
            loader.queue.map { it.priority } shouldBe listOf(0, 0)
            loader.recycle()
        }
    }

    @Test
    fun cancelledCacheReadRethrows() {
        every { harness.chapterCache.getPageListFromCache(any()) } throws CancellationException("stop")
        val loader = harness.loader()
        shouldThrow<CancellationException> { runBlocking { loader.getPages() } }
        loader.recycle()
    }

    @Test
    fun loadPageQueuesAndUnqueues() {
        runBlocking {
            val loader = harness.loader()
            val pages = loadedPages(harness.chapter, count = 3)
            every { harness.chapterCache.isImageInCache(any()) } returns false
            pages[0].status = Page.State.Ready
            pages[2].status = Page.State.Ready
            val job = async { loader.loadPage(pages[0]) }
            withTimeout(5_000) { while (loader.queue.size < 2) delay(5) }
            loader.queue.map { it.page.index } shouldBe listOf(0, 1)
            pages[1].status = Page.State.LoadPage
            job.cancelAndJoin()
            loader.queue.map { it.page.index } shouldBe listOf(1)
            loader.recycle()
        }
    }

    @Test
    fun loadPageRequeuesErrors() {
        runBlocking {
            val loader = harness.loader()
            val pages = loadedPages(harness.chapter, count = 1)
            pages[0].status = Page.State.Error(IllegalStateException("x"))
            val job = async { loader.loadPage(pages[0]) }
            withTimeout(5_000) { while (loader.queue.isEmpty()) delay(5) }
            pages[0].status shouldBe Page.State.Queue
            job.cancelAndJoin()
            loader.queue.shouldBeEmpty()
            loader.recycle()
        }
    }

    @Test
    fun loadPageKeepsReadyPages() {
        runBlocking {
            val loader = harness.loader()
            val cached = loadedPages(harness.chapter, count = 2)
            every { harness.chapterCache.isImageInCache("https://img/0") } returns true
            cached[0].status = Page.State.Ready
            val noUrl = ReaderPage(1).also {
                it.chapter = harness.chapter
                it.status = Page.State.Ready
            }
            for (page in listOf(cached[0], noUrl)) {
                val job = async { loader.loadPage(page) }
                delay(50)
                page.status shouldBe Page.State.Ready
                job.cancelAndJoin()
            }
            loader.recycle()
        }
    }

    @Test
    fun retryWithQueue() {
        harness.readerPreferences.readerInstantRetry.set(false)
        val loader = harness.loader()
        val page = ReaderPage(0, imageUrl = "https://a")
        page.status = Page.State.Error(IllegalStateException("x"))
        loader.retryPage(page)
        page.status shouldBe Page.State.Queue
        page.imageUrl shouldBe "https://a"
        loader.queue.single().priority shouldBe PriorityPage.RETRY
        val ready = ReaderPage(1).also { it.status = Page.State.Ready }
        loader.retryPage(ready)
        ready.status shouldBe Page.State.Ready
        loader.recycle()
    }

    @Test
    fun instantRetryOnEhDropsUrl() {
        val eh = HttpLoaderHarness(sourceId = EH_SOURCE_ID)
        val loader = eh.loader()
        val page = ReaderPage(0, imageUrl = "https://a").also { it.status = Page.State.Ready }
        loader.retryPage(page)
        page.imageUrl.shouldBeNull()
        loader.queue.shouldBeEmpty()
        loader.recycle()
    }

    @Test
    fun recycleCachesPageList() {
        val loader = harness.loader()
        loader.recycle()
        loader.isRecycled shouldBe true
        verify(exactly = 0) { harness.chapterCache.putPageListToCache(any(), any()) }
        loadedPages(harness.chapter, count = 2)
        harness.loader().recycle()
        verify(timeout = 5_000) { harness.chapterCache.putPageListToCache(any(), match { it.size == 2 }) }
    }

    @Test
    fun recycleSwallowsCacheErrors() {
        loadedPages(harness.chapter, count = 1)
        every { harness.chapterCache.putPageListToCache(any(), any()) } throws IllegalStateException("disk")
        harness.loader().recycle()
        verify(timeout = 5_000) { harness.chapterCache.putPageListToCache(any(), any()) }
        every { harness.chapterCache.putPageListToCache(any(), any()) } throws CancellationException("stop")
        harness.loader().recycle()
        verify(timeout = 5_000, exactly = 2) { harness.chapterCache.putPageListToCache(any(), any()) }
    }
}
