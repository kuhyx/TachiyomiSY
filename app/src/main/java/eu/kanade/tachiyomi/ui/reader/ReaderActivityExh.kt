package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.system.toast
import exh.source.isEhBasedSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val AUTOSCROLL_IDLE_POLL_MS = 100L

/*
 * The EH reader utilities (SY): auto-scroll, retry-all and page boosting, driven from the
 * reader's EH utils bar.
 */

internal fun ReaderActivity.enableExhAutoScroll() {
    readerPreferences.autoscrollInterval.changes()
        .combine(viewModel.state.map { it.autoScroll }.distinctUntilChanged()) { interval, enabled ->
            interval.toDouble() to enabled
        }
        .mapLatest { (intervalFloat, enabled) ->
            if (enabled) {
                repeatOnLifecycle(Lifecycle.State.STARTED) { autoScrollLoop(intervalFloat.seconds) }
            }
        }
        .launchIn(lifecycleScope)
}

// Advances every [interval] while the menu is hidden; with the menu up it only polls for it to close.
private suspend fun ReaderActivity.autoScrollLoop(interval: Duration) {
    while (true) {
        if (viewModel.state.value.menuVisible) {
            delay(AUTOSCROLL_IDLE_POLL_MS)
        } else {
            autoScrollStep(interval)
            delay(interval)
        }
    }
}

private fun ReaderActivity.autoScrollStep(interval: Duration) {
    when (val v = viewModel.state.value.viewer) {
        is PagerViewer -> v.moveToNext()
        is WebtoonViewer -> if (readerPreferences.smoothAutoScroll.get()) v.linearScroll(interval) else v.scrollDown()
        else -> {}
    }
}

internal fun ReaderActivity.exhRetryAll() {
    var retried = 0

    viewModel.state.value.viewerChapters
        ?.currChapter
        ?.pages
        ?.filter { it.status is Page.State.Error }
        ?.forEach { page ->
            page.status = Page.State.Queue

            // If we are using EHentai/ExHentai, get a new image URL
            viewModel.manga?.let { m ->
                val src = sourceManager.get(m.source)
                if (src?.isEhBasedSource() == true) {
                    page.imageUrl = null
                }
            }

            val loader = page.chapter.pageLoader
            if (page.index == exhCurrentpage()?.index && loader is HttpPageLoader) {
                loader.boostPage(page)
            } else {
                loader?.retryPage(page)
            }

            retried++
        }

    toast(pluralStringResource(SYMR.plurals.eh_retry_toast, retried, retried))
}

internal fun ReaderActivity.exhBoostPage() {
    viewModel.state.value.viewer ?: return
    val curPage = exhCurrentpage() ?: run {
        toast(SYMR.strings.eh_boost_page_invalid)
        return
    }

    if (curPage.status is Page.State.Error) {
        toast(SYMR.strings.eh_boost_page_errored)
    } else if (curPage.status == Page.State.LoadPage || curPage.status == Page.State.DownloadImage) {
        toast(SYMR.strings.eh_boost_page_downloading)
    } else if (curPage.status == Page.State.Ready) {
        toast(SYMR.strings.eh_boost_page_downloaded)
    } else {
        val loader = viewModel.state.value.viewerChapters?.currChapter?.pageLoader as? HttpPageLoader
        if (loader != null) {
            loader.boostPage(curPage)
            toast(SYMR.strings.eh_boost_boosted)
        } else {
            toast(SYMR.strings.eh_boost_invalid_loader)
        }
    }
}

private fun ReaderActivity.exhCurrentpage(): ReaderPage? {
    val viewer = viewModel.state.value.viewer
    val currentPage =
        (((viewer as? PagerViewer)?.currentPage ?: (viewer as? WebtoonViewer)?.currentPage) as? ReaderPage)?.index
    return currentPage?.let { viewModel.state.value.viewerChapters?.currChapter?.pages?.getOrNull(it) }
}
