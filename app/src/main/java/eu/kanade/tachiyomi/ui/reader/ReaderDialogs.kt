package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.api.get

internal fun ReaderViewModel.openChapterListDialog() {
    mutableState.update { it.copy(dialog = Dialog.ChapterList) }
}

internal fun ReaderViewModel.openAutoScrollHelpDialog() {
    mutableState.update { it.copy(dialog = Dialog.AutoScrollHelp) }
}

internal fun ReaderViewModel.openBoostPageHelp() {
    mutableState.update { it.copy(dialog = Dialog.BoostPageHelp) }
}

internal fun ReaderViewModel.openRetryAllHelp() {
    mutableState.update { it.copy(dialog = Dialog.RetryAllHelp) }
}

internal fun ReaderViewModel.showLoadingDialog() {
    mutableState.update { it.copy(dialog = Dialog.Loading) }
}

internal fun ReaderViewModel.openReadingModeSelectDialog() {
    mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
}

internal fun ReaderViewModel.openOrientationSelectDialog() {
    mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
}

internal fun ReaderViewModel.openPageDialog(page: ReaderPage/* SY --> */, extraPage: ReaderPage? = null/* SY <-- */) {
    mutableState.update { it.copy(dialog = Dialog.PageActions(page, extraPage)) }
}

internal fun ReaderViewModel.openSettingsDialog() {
    mutableState.update { it.copy(dialog = Dialog.Settings) }
}

internal fun ReaderViewModel.closeDialog() {
    mutableState.update { it.copy(dialog = null) }
}
