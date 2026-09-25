package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.aggressivePageLoading
import eu.kanade.tachiyomi.ui.reader.setting.preloadSize
import eu.kanade.tachiyomi.ui.reader.setting.readerInstantRetry
import eu.kanade.tachiyomi.ui.reader.setting.readerThreads
import exh.source.isEhBasedSource
import exh.util.DataSaver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue

/**
 * Loader used to load chapters from an online source.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
    internal val source: HttpSource,
    internal val chapterCache: ChapterCache = Injekt.get(),
    // SY -->
    private val readerPreferences: ReaderPreferences = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    // SY <--
) : PageLoader() {

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // A queue used to manage requests one by one while allowing priorities.
    internal val queue = PriorityBlockingQueue<PriorityPage>()

    private val preloadSize = /* SY --> */ readerPreferences.preloadSize.get() // SY <--

    // SY -->
    internal val dataSaver = DataSaver(source, sourcePreferences)
    // SY <--

    init {
        // EXH -->
        repeat(readerPreferences.readerThreads.get()) {
            // EXH <--
            scope.launchIO {
                // Never returns: the loop ends only when [recycle] cancels the scope.
                while (true) {
                    val next = runInterruptible { queue.take() }
                    if (next.page.status == Page.State.Queue) {
                        internalLoadPage(page = next.page, force = next.priority == PriorityPage.RETRY)
                    }
                }
            }
            // EXH -->
        }
        // EXH <--
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = try {
            chapterCache.getPageListFromCache(chapter.chapter.toDomainChapter()!!)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Rethrown (or wrapped) whatever the cause.
            source.getPageList(chapter.chapter)
        }
        // SY -->
        val rp = pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
        if (readerPreferences.aggressivePageLoading.get()) {
            // Freshly built pages are all still queued.
            rp.forEach { queue.offer(PriorityPage(it, 0)) }
        }
        return rp
        // SY <--
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) = withIOContext {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.Ready && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.Queue
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }

        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Page.State.Queue) {
            queuedPages += PriorityPage(page, PriorityPage.DEFAULT).also { queue.offer(it) }
        }
        queuedPages += preloadNextPages(page, preloadSize)

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Page.State.Queue) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }
        // EXH -->
        // Grab a new image URL on EXH sources
        if (source.isEhBasedSource()) {
            page.imageUrl = null
        }

        if (readerPreferences.readerInstantRetry.get()) {
            boostPage(page)
        } else {
            // EXH <--
            queue.offer(PriorityPage(page, PriorityPage.RETRY))
        }
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter.toDomainChapter()!!, pagesToSave)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (expected: Throwable) {
                    // Rethrown (or wrapped) whatever the cause.
                }
            }
        }
    }

    // EXH <--
}
