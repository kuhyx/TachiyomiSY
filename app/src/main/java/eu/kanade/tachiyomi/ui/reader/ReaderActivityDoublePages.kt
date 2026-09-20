package eu.kanade.tachiyomi.ui.reader

import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import uy.kohesive.injekt.api.get

internal fun ReaderActivity.reloadChapters(doublePages: Boolean, force: Boolean = false) {
    val viewer = viewModel.state.value.viewer as? PagerViewer ?: return
    viewer.updateShifting()
    if (!force && viewer.config.autoDoublePages) {
        setDoublePageMode(viewer)
    } else {
        viewer.config.doublePages = doublePages
        viewModel.setDoublePages(viewer.config.doublePages)
    }
    val currentChapter = viewModel.state.value.currentChapter
    if (doublePages) {
        // If we're moving from singe to double, we want the current page to be the first page
        val currentPage = viewModel.state.value.currentPage
        viewer.config.shiftDoublePage = (
            currentPage + (currentChapter?.pages?.take(currentPage)?.count { it.fullPage || it.isolatedPage } ?: 0)
            ) % 2 != 0
    }
    viewModel.state.value.viewerChapters?.let {
        viewer.setChaptersDoubleShift(it)
    }
}

internal fun ReaderActivity.setDoublePageMode(viewer: PagerViewer) {
    val currentOrientation = resources.configuration.orientation
    viewer.config.doublePages = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
    viewModel.setDoublePages(viewer.config.doublePages)
}

internal fun ReaderActivity.shiftDoublePages() {
    val viewer = viewModel.state.value.viewer as? PagerViewer ?: return
    viewer.config.let { config ->
        config.shiftDoublePage = !config.shiftDoublePage
        viewModel.state.value.viewerChapters?.let {
            viewer.updateShifting()
            viewer.setChaptersDoubleShift(it)
            invalidateOptionsMenu()
        }
    }
}
