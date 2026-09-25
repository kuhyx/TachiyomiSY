package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class HttpPageLoaderQueueTest {

    @TempDir
    lateinit var dir: File

    @Test
    fun preloadSkipsLoadedAndLast() {
        val harness = HttpLoaderHarness()
        val loader = harness.loader()
        loader.preloadNextPages(ReaderPage(0).also { it.chapter = readerChapter() }, amount = 2).shouldBeEmpty()
        val pages = loadedPages(harness.chapter, count = 4)
        pages[2].status = Page.State.Ready
        loader.preloadNextPages(pages[3], amount = 2).shouldBeEmpty()
        loader.preloadNextPages(pages[0], amount = 5).map { it.page.index } shouldBe listOf(1, 3)
        loader.preloadNextPages(pages[0], amount = 1).map { it.priority } shouldBe listOf(PriorityPage.ADJACENT)
        loader.queue.size shouldBe 3
        loader.recycle()
    }

    @Test
    fun resolvesUrlAndDownloads() = runBlocking {
        val harness = HttpLoaderHarness()
        harness.cacheServes(File(dir, "img"), byteArrayOf(9))
        coEvery { harness.source.getImageUrl(any()) } returns "https://img/9"
        coEvery { harness.source.getImage(any(), any()) } returns imageResponse()
        every { harness.chapterCache.isImageInCache("https://img/9") } returns false
        val loader = harness.loader()
        val page = ReaderPage(0, imageUrl = "")
        loader.internalLoadPage(page, force = false)
        page.imageUrl shouldBe "https://img/9"
        page.status shouldBe Page.State.Ready
        page.stream!!().read() shouldBe 9
        verify { harness.chapterCache.putImageToCache("https://img/9", any()) }
        loader.recycle()
    }

    @Test
    fun cachedImageSkipsDownload() = runBlocking {
        val harness = HttpLoaderHarness()
        harness.cacheServes(File(dir, "img"), byteArrayOf(4))
        every { harness.chapterCache.isImageInCache("https://a") } returns true
        val loader = harness.loader()
        val page = ReaderPage(0, imageUrl = "https://a")
        loader.internalLoadPage(page, force = false)
        page.status shouldBe Page.State.Ready
        coVerify(exactly = 0) { harness.source.getImage(any(), any()) }
        coEvery { harness.source.getImage(any(), any()) } returns imageResponse()
        loader.internalLoadPage(page, force = true)
        coVerify(exactly = 1) { harness.source.getImage(any(), any()) }
        loader.recycle()
    }

    @Test
    fun failuresMarkThePage() = runBlocking {
        val harness = HttpLoaderHarness()
        coEvery { harness.source.getImageUrl(any()) } throws IllegalStateException("gone")
        val loader = harness.loader()
        val page = ReaderPage(0)
        loader.internalLoadPage(page, force = false)
        page.status.shouldBeInstanceOf<Page.State.Error>()
        coEvery { harness.source.getImageUrl(any()) } throws CancellationException("stop")
        shouldThrow<CancellationException> { loader.internalLoadPage(page, force = false) }
        (page.status as Page.State.Error).error.shouldBeInstanceOf<CancellationException>()
        loader.recycle()
    }

    @Test
    fun workersDrainTheQueue() {
        val harness = HttpLoaderHarness(threads = 1)
        harness.cacheServes(File(dir, "img"), byteArrayOf(1))
        every { harness.chapterCache.isImageInCache(any()) } returns true
        coEvery { harness.source.getImage(any(), any()) } returns imageResponse()
        val loader = harness.loader()
        val skipped = ReaderPage(0, imageUrl = "https://skip").also { it.status = Page.State.Ready }
        val retried = ReaderPage(1, imageUrl = "https://retry")
        val plain = ReaderPage(2, imageUrl = "https://plain")
        loader.queue.offer(PriorityPage(skipped, PriorityPage.RETRY))
        loader.queue.offer(PriorityPage(retried, PriorityPage.RETRY))
        loader.queue.offer(PriorityPage(plain, PriorityPage.DEFAULT))
        awaitStatus(plain) { it == Page.State.Ready }
        awaitStatus(retried) { it == Page.State.Ready }
        coVerify(exactly = 1) { harness.source.getImage(retried, any()) }
        coVerify(exactly = 0) { harness.source.getImage(plain, any()) }
        loader.recycle()
    }

    @Test
    fun boostLoadsQueuedPages() {
        val harness = HttpLoaderHarness()
        harness.cacheServes(File(dir, "img"), byteArrayOf(1))
        every { harness.chapterCache.isImageInCache(any()) } returns true
        val loader = harness.loader()
        val pages = loadedPages(harness.chapter, count = 1)
        loader.retryPage(pages[0])
        runBlocking {
            withTimeout(5_000) {
                while (loader.queue.isEmpty()) delay(5)
            }
        }
        loader.queue.single().page shouldBe pages[0]
        loader.recycle()
    }
}
