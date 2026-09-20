package eu.kanade.tachiyomi.ui.reader

import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.api.get

internal fun ReaderViewModel.showMenus(visible: Boolean) {
    mutableState.update { it.copy(menuVisible = visible) }
}

// SY -->
internal fun ReaderViewModel.showEhUtils(visible: Boolean) {
    mutableState.update { it.copy(ehUtilsVisible = visible) }
}

internal fun ReaderViewModel.setIndexChapterToShift(index: Long?) {
    mutableState.update { it.copy(indexChapterToShift = index) }
}

internal fun ReaderViewModel.setIndexPageToShift(index: Int?) {
    mutableState.update { it.copy(indexPageToShift = index) }
}

internal fun ReaderViewModel.setDoublePages(doublePages: Boolean) {
    mutableState.update { it.copy(doublePages = doublePages) }
}

internal fun ReaderViewModel.toggleAutoScroll(enabled: Boolean) {
    mutableState.update { it.copy(autoScroll = enabled) }
}

internal fun ReaderViewModel.setAutoScrollFrequency(frequency: String) {
    mutableState.update { it.copy(ehAutoscrollFreq = frequency) }
}

internal fun ReaderViewModel.setBrightnessOverlayValue(value: Int) {
    mutableState.update { it.copy(brightnessOverlayValue = value) }
}
