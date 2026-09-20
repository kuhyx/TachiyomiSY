package eu.kanade.tachiyomi.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import uy.kohesive.injekt.api.get

// Sets the visibility of the menu according to [visible].
internal fun ReaderActivity.setMenuVisibility(visible: Boolean) {
    viewModel.showMenus(visible)
    if (visible) {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    } else if (readerPreferences.fullscreen.get()) {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Called from the viewer to toggle the visibility of the menu. It's implemented on the
 * viewer because each one implements its own touch and key events.
 */
internal fun ReaderActivity.toggleMenu() {
    setMenuVisibility(!viewModel.state.value.menuVisible)
}

/**
 * Called from the viewer to show the menu.
 */
internal fun ReaderActivity.showMenu() {
    if (!viewModel.state.value.menuVisible) {
        setMenuVisibility(true)
    }
}

/**
 * Called from the viewer to hide the menu.
 */
internal fun ReaderActivity.hideMenu() {
    if (viewModel.state.value.menuVisible) {
        setMenuVisibility(false)
    }
}
