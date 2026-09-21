package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.animation.LinearInterpolator
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.onPageSelected
import eu.kanade.tachiyomi.ui.reader.toggleMenu
import uy.kohesive.injekt.api.get
import kotlin.time.Duration

internal fun WebtoonViewer.onScrolled(pos: Int? = null) {
    val position = pos ?: layoutManager.findLastEndVisibleItemPosition()
    val item = adapter.items.getOrNull(position)
    val allowPreload = checkAllowPreload(item as? ReaderPage)
    if (item != null && currentPage != item) {
        currentPage = item
        when (item) {
            is ReaderPage -> onPageSelected(item, allowPreload)
            is ChapterTransition -> onTransitionSelected(item)
        }
    }
}

// Scrolls up by [scrollDistance].
internal fun WebtoonViewer.scrollUp() {
    if (config.usePageTransitions) {
        recycler.smoothScrollBy(0, -scrollDistance)
    } else {
        recycler.scrollBy(0, -scrollDistance)
    }
}

/**
 * Scrolls one screen over a period of time.
 */
internal fun WebtoonViewer.linearScroll(duration: Duration) {
    recycler.smoothScrollBy(
        0,
        activity.resources.displayMetrics.heightPixels,
        LinearInterpolator(),
        duration.inWholeMilliseconds.toInt(),
    )
}

/**
 * Scrolls down by [scrollDistance].
 */
/* [EXH] private */
internal fun WebtoonViewer.scrollDown() {
    // SY --> Tapping by page (non-continuous mode) jumps to the next page when there is one.
    val tapTarget = (currentPage as? ReaderPage)?.takeIf { !isContinuous && tapByPage }
    val position = tapTarget?.let { adapter.items.indexOf(it) }
    val nextItem = position?.let { adapter.items.getOrNull(it + 1) }
    if (position != null && nextItem is ReaderPage) {
        if (config.usePageTransitions) {
            recycler.smoothScrollToPosition(position + 1)
        } else {
            recycler.scrollToPosition(position + 1)
        }
        return
    }
    // SY <--
    scrollDownBy()
}

internal fun WebtoonViewer.scrollDownBy() {
    // SY <--
    if (config.usePageTransitions) {
        recycler.smoothScrollBy(0, scrollDistance)
    } else {
        recycler.scrollBy(0, scrollDistance)
    }
}

// What a key does, or null for a key the viewer does not handle.
internal fun WebtoonViewer.keyAction(keyCode: Int): (() -> Unit)? = when (keyCode) {
    KeyEvent.KEYCODE_VOLUME_DOWN -> if (config.volumeKeysInverted) ::scrollUp else ::scrollDown
    KeyEvent.KEYCODE_VOLUME_UP -> if (config.volumeKeysInverted) ::scrollDown else ::scrollUp
    KeyEvent.KEYCODE_MENU -> {
        { activity.toggleMenu() }
    }
    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> ::scrollUp
    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> ::scrollDown
    else -> null
}
