package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.KeyEvent
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.onPageSelected
import eu.kanade.tachiyomi.ui.reader.toggleMenu
import tachiyomi.core.common.util.system.logcat

internal fun PagerViewer.moveToReaderPage(page: ReaderPage) {
    val position = adapter.joinedItems.indexOfFirst { it.first == page || it.second == page }
    if (position != -1) {
        val currentPosition = pager.currentItem
        pager.setCurrentItem(position, true)
        // manually call onPageChange since ViewPager listener is not triggered in this case
        if (currentPosition == position) {
            onPageChange(position)
        } else {
            // Call this since with double shift onPageChange wont get called (it shouldn't)
            // Instead just update the page count in ui
            val joinedItem = adapter.joinedItems.firstOrNull { it.first == page || it.second == page }
            activity.onPageSelected(
                joinedItem?.first as? ReaderPage ?: page,
                joinedItem?.second != null,
            )
        }
    } else {
        logcat { "Page $page not found in adapter" }
    }
}

// What a key does, or null for a key the viewer does not handle.
internal fun PagerViewer.keyAction(event: KeyEvent): (() -> Unit)? {
    val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0
    return when (event.keyCode) {
        KeyEvent.KEYCODE_VOLUME_DOWN -> if (config.volumeKeysInverted) ::moveUp else ::moveDown
        KeyEvent.KEYCODE_VOLUME_UP -> if (config.volumeKeysInverted) ::moveDown else ::moveUp
        KeyEvent.KEYCODE_DPAD_RIGHT -> if (ctrlPressed) ::moveToNext else ::moveRight
        KeyEvent.KEYCODE_DPAD_LEFT -> if (ctrlPressed) ::moveToPrevious else ::moveLeft
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> ::moveDown
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> ::moveUp
        KeyEvent.KEYCODE_MENU -> {
            { activity.toggleMenu() }
        }
        else -> null
    }
}
