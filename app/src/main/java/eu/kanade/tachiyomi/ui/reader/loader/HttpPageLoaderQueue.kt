@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import exh.util.DataSaver.Companion.getImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.api.get
import kotlin.math.min

// Preloads the given [amount] of pages after the [currentPage] with a lower priority.
// @return a list of [PriorityPage] that were added to the [queue]
internal fun HttpPageLoader.preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
    val pages = currentPage.chapter.pages ?: return emptyList()
    val pageIndex = currentPage.index
    if (pageIndex == pages.lastIndex) return emptyList()

    return pages
        .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
        .mapNotNull {
            if (it.status == Page.State.Queue) {
                PriorityPage(it, PriorityPage.ADJACENT).apply { queue.offer(this) }
            } else {
                null
            }
        }
}

// Loads the page, retrieving the image URL and downloading the image if necessary.
// Downloaded images are stored in the chapter cache.
// @param page the page whose source image has to be downloaded.
internal suspend fun HttpPageLoader.internalLoadPage(page: ReaderPage, force: Boolean) {
    try {
        if (page.imageUrl.isNullOrEmpty()) {
            page.status = Page.State.LoadPage
            page.imageUrl = source.getImageUrl(page)
        }
        val imageUrl = page.imageUrl!!

        if (force || !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.DownloadImage
            val imageResponse = source.getImage(page, dataSaver = dataSaver)
            chapterCache.putImageToCache(imageUrl, imageResponse)
        }

        page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
        page.status = Page.State.Ready
    } catch (cancelled: CancellationException) {
        page.status = Page.State.Error(cancelled)
        throw cancelled
    } catch (expected: Throwable) {
        // The page shows the error, whatever the cause.
        page.status = Page.State.Error(expected)
    }
}

// EXH -->
internal fun HttpPageLoader.boostPage(page: ReaderPage) {
    if (page.status == Page.State.Queue) {
        scope.launchIO {
            loadPage(page)
        }
    }
}
