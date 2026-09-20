package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.api.get

// Called from the presenter whenever a new [viewerChapters] have been set. It delegates the
// method to the current viewer, but also set the subtitle on the toolbar, and
// hides or disables the reader prev/next buttons if there's a prev or next chapter
@SuppressLint("RestrictedApi")
internal fun ReaderActivity.setChapters(viewerChapters: ViewerChapters) {
    binding.readerContainer.removeView(loadingIndicator)
    // SY -->
    val state = viewModel.state.value
    if (state.indexChapterToShift != null && state.indexPageToShift != null) {
        viewerChapters.currChapter.pages?.find {
            it.index == state.indexPageToShift && it.chapter.chapter.id == state.indexChapterToShift
        }?.let {
            (viewModel.state.value.viewer as? PagerViewer)?.updateShifting(it)
        }
        viewModel.setIndexChapterToShift(null)
        viewModel.setIndexPageToShift(null)
    } else if (state.lastShiftDoubleState != null) {
        val currentChapter = viewerChapters.currChapter
        (viewModel.state.value.viewer as? PagerViewer)?.config?.shiftDoublePage = (
            currentChapter.requestedPage +
                (
                    currentChapter.pages?.take(currentChapter.requestedPage)
                        ?.count { it.fullPage || it.isolatedPage }
                        ?: 0
                    )
            ) % 2 != 0
    }
    // SY <--

    viewModel.state.value.viewer?.setChapters(viewerChapters)

    lifecycleScope.launchIO {
        viewModel.getChapterUrl()?.let { url ->
            assistUrl = url
        }
    }
}

// Called from the presenter if the initial load couldn't load the pages of the chapter. In
// this case the activity is closed and a toast is shown to the user.
internal fun ReaderActivity.setInitialChapterError(error: Throwable) {
    logcat(LogPriority.ERROR, error)
    finish()
    toast(error.message)
}

// Called from the presenter whenever it's loading the next or previous chapter. It shows or
// dismisses a non-cancellable dialog to prevent user interaction according to the value of
// [show]. This is only used when the next/previous buttons on the toolbar are clicked; the
// other cases are handled with chapter transitions on the viewers and chapter preloading.
internal fun ReaderActivity.setProgressDialog(show: Boolean) {
    if (show) {
        viewModel.showLoadingDialog()
    } else {
        viewModel.closeDialog()
    }
}

// Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
// page is not found.
internal fun ReaderActivity.moveToPageIndex(index: Int) {
    val viewer = viewModel.state.value.viewer ?: return
    val currentChapter = viewModel.state.value.currentChapter ?: return
    val page = currentChapter.pages?.getOrNull(index) ?: return
    viewer.moveToPage(page)
}

// Tells the presenter to load the next chapter and mark it as active. The progress dialog
// should be automatically shown.
internal fun ReaderActivity.loadNextChapter() {
    lifecycleScope.launch {
        viewModel.loadNextChapter()
        moveToPageIndex(0)
    }
}

// Tells the presenter to load the previous chapter and mark it as active. The progress dialog
// should be automatically shown.
internal fun ReaderActivity.loadPreviousChapter() {
    lifecycleScope.launch {
        viewModel.loadPreviousChapter()
        moveToPageIndex(0)
    }
}

/**
 * Called from the viewer whenever a [page] is marked as active. It updates the values of the
 * bottom menu and delegates the change to the presenter.
 */
@SuppressLint("SetTextI18n")
internal fun ReaderActivity.onPageSelected(page: ReaderPage, hasExtraPage: Boolean = false) {
    // SY -->
    val currentPageText = if (hasExtraPage) {
        val invertDoublePage = (viewModel.state.value.viewer as? PagerViewer)?.config?.invertDoublePages ?: false
        if ((resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) xor
            invertDoublePage
        ) {
            "${page.number}-${page.number + 1}"
        } else {
            "${page.number + 1}-${page.number}"
        }
    } else {
        "${page.number}"
    }
    viewModel.onPageSelected(page, currentPageText, hasExtraPage)
    // SY <--
}

/**
 * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
 * actions to perform is shown.
 */
internal fun ReaderActivity.onPageLongTap(page: ReaderPage, extraPage: ReaderPage? = null) {
    // SY -->
    viewModel.openPageDialog(page, extraPage)
    // SY <--
}

/**
 * Called from the viewer when the given [chapter] should be preloaded. It should be called when
 * the viewer is reaching the beginning or end of a chapter or the transition page is active.
 */
internal fun ReaderActivity.requestPreloadChapter(chapter: ReaderChapter) {
    lifecycleScope.launchIO { viewModel.preload(chapter) }
}
