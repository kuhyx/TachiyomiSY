package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.PointF
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.onPageLongTap
import eu.kanade.tachiyomi.ui.reader.requestPreloadChapter
import eu.kanade.tachiyomi.ui.reader.showMenu
import eu.kanade.tachiyomi.ui.reader.toggleMenu
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import uy.kohesive.injekt.api.get

private const val RECYCLER_VIEW_CACHE_SIZE = 4

internal fun WebtoonViewer.setUpRecycler() {
    recycler.setItemViewCacheSize(RECYCLER_VIEW_CACHE_SIZE)
    recycler.isVisible = false // Don't let the recycler layout yet
    recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    recycler.isFocusable = false
    recycler.itemAnimator = null
    recycler.layoutManager = layoutManager
    recycler.adapter = adapter
    recycler.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrolled()

                if ((dy > threshold || dy < -threshold) && activity.viewModel.state.value.menuVisible) {
                    activity.hideMenu()
                }

                if (dy < 0) {
                    val firstIndex = layoutManager.findFirstVisibleItemPosition()
                    val firstItem = adapter.items.getOrNull(firstIndex)
                    if (firstItem is ChapterTransition.Prev && firstItem.to != null) {
                        activity.requestPreloadChapter(firstItem.to)
                    }
                }

                val lastIndex = layoutManager.findLastEndVisibleItemPosition()
                val lastItem = adapter.items.getOrNull(lastIndex)
                if (lastItem is ChapterTransition.Next && lastItem.to == null) {
                    activity.showMenu()
                }
            }
        },
    )
}

internal fun WebtoonViewer.setUpTapListeners() {
    recycler.tapListener = { event ->
        val viewPosition = IntArray(2)
        recycler.getLocationOnScreen(viewPosition)
        val viewPositionRelativeToWindow = IntArray(2)
        recycler.getLocationInWindow(viewPositionRelativeToWindow)
        val pos = PointF(
            (event.rawX - viewPosition[0] + viewPositionRelativeToWindow[0]) / recycler.width,
            (event.rawY - viewPosition[1] + viewPositionRelativeToWindow[1]) / recycler.originalHeight,
        )
        when (config.navigator.getAction(pos)) {
            NavigationRegion.MENU -> activity.toggleMenu()
            NavigationRegion.NEXT, NavigationRegion.RIGHT -> scrollDown()
            NavigationRegion.PREV, NavigationRegion.LEFT -> scrollUp()
        }
    }
    recycler.longTapListener = { event ->
        val page = recycler.findChildViewUnder(event.x, event.y)
            ?.let { adapter.items.getOrNull(recycler.getChildAdapterPosition(it)) as? ReaderPage }
            ?.takeIf { activity.viewModel.state.value.menuVisible || config.longTapEnabled }
        if (page != null) activity.onPageLongTap(page)
        page != null
    }
}

internal fun WebtoonViewer.setUpConfigListeners() {
    config.imagePropertyChangedListener = {
        refreshAdapter()
    }

    config.themeChangedListener = {
        ActivityCompat.recreate(activity)
    }

    config.doubleTapZoomChangedListener = {
        frame.doubleTapZoom = it
    }

    config.zoomPropertyChangedListener = {
        frame.zoomOutDisabled = it
    }

    config.navigationModeChangedListener = {
        val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
        activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
    }
}

internal fun WebtoonViewer.setUpFrame() {
    frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    frame.addView(recycler)
}
