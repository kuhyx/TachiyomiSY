package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.PointF
import android.view.ViewGroup.LayoutParams
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.onPageLongTap
import eu.kanade.tachiyomi.ui.reader.reloadChapters
import eu.kanade.tachiyomi.ui.reader.toggleMenu
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion

internal fun PagerViewer.setUpPager() {
    pager.isVisible = false // Don't layout the pager yet
    pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    pager.isFocusable = false
    pager.offscreenPageLimit = 1
    pager.id = R.id.reader_pager
    pager.adapter = adapter
    pager.addOnPageChangeListener(pagerListener)
}

internal fun PagerViewer.setUpTapListeners() {
    pager.tapListener = { event ->
        val viewPosition = IntArray(2)
        pager.getLocationOnScreen(viewPosition)
        val viewPositionRelativeToWindow = IntArray(2)
        pager.getLocationInWindow(viewPositionRelativeToWindow)
        val pos = PointF(
            (event.rawX - viewPosition[0] + viewPositionRelativeToWindow[0]) / pager.width,
            (event.rawY - viewPosition[1] + viewPositionRelativeToWindow[1]) / pager.height,
        )
        when (config.navigator.getAction(pos)) {
            NavigationRegion.MENU -> activity.toggleMenu()
            NavigationRegion.NEXT -> moveToNext()
            NavigationRegion.PREV -> moveToPrevious()
            NavigationRegion.RIGHT -> moveRight()
            NavigationRegion.LEFT -> moveLeft()
        }
    }
    pager.longTapListener = {
        val item = adapter.joinedItems.getOrNull(pager.currentItem)
        val firstPage = (item?.first as? ReaderPage)
            ?.takeIf { activity.viewModel.state.value.menuVisible || config.longTapEnabled }
        if (firstPage != null) activity.onPageLongTap(firstPage, item.second as? ReaderPage)
        firstPage != null
    }
}

internal fun PagerViewer.setUpConfigListeners() {
    config.dualPageSplitChangedListener = { enabled ->
        if (!enabled) {
            cleanupPageSplit()
        }
    }

    config.reloadChapterListener = {
        activity.reloadChapters(it)
    }

    config.imagePropertyChangedListener = {
        refreshAdapter()
    }

    config.navigationModeChangedListener = {
        val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
        activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
    }
}

// Resets the adapter in order to recreate all the views. Used when a image configuration is
// changed.
internal fun PagerViewer.refreshAdapter() {
    val currentItem = pager.currentItem
    adapter.refresh()
    pager.adapter = adapter
    pager.setCurrentItem(currentItem, false)
}
