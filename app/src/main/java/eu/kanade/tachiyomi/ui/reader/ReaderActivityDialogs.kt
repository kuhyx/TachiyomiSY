package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.reader.ChapterListDialog
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

// The dialog the reader view model asked for, if any.
@Composable
internal fun ReaderActivity.ReaderDialogs(
    state: ReaderViewModel.State,
    settingsScreenModel: ReaderSettingsScreenModel,
) {
    val onDismissRequest = viewModel::closeDialog
    when (val dialog = state.dialog) {
        is ReaderViewModel.Dialog.Loading -> {
            LoadingDialog()
        }
        is ReaderViewModel.Dialog.Settings -> {
            ReaderSettingsDialog(
                onDismissRequest = onDismissRequest,
                onShowMenus = { setMenuVisibility(true) },
                onHideMenus = { setMenuVisibility(false) },
                screenModel = settingsScreenModel,
            )
        }
        is ReaderViewModel.Dialog.ReadingModeSelect -> {
            ReadingModeSelectDialog(
                onDismissRequest = onDismissRequest,
                screenModel = settingsScreenModel,
                // The reader already shows the mode as an overlay when that preference is on.
                onChange = { showModeToast(it, unless = readerPreferences.showReadingMode.get()) },
            )
        }
        is ReaderViewModel.Dialog.OrientationModeSelect -> {
            OrientationSelectDialog(
                onDismissRequest = onDismissRequest,
                screenModel = settingsScreenModel,
                onChange = { showModeToast(it, unless = false) },
            )
        }
        is ReaderViewModel.Dialog.PageActions -> {
            ReaderPageActionsDialog(
                onDismissRequest = onDismissRequest,
                onSetAsCover = viewModel.images::setAsCover,
                onShare = viewModel.images::shareImage,
                onSave = viewModel.images::saveImage,
                onShareCombined = viewModel.images::shareImages,
                onSaveCombined = viewModel.images::saveImages,
                hasExtraPage = dialog.extraPage != null,
            )
        }
        is ReaderViewModel.Dialog.ChapterList -> {
            ReaderChapterListDialog(state, settingsScreenModel, onDismissRequest)
        }
        // SY -->
        ReaderViewModel.Dialog.AutoScrollHelp -> {
            HelpDialog(SYMR.strings.eh_autoscroll_help, SYMR.strings.eh_autoscroll_help_message, onDismissRequest)
        }
        ReaderViewModel.Dialog.BoostPageHelp -> {
            HelpDialog(SYMR.strings.eh_boost_page_help, SYMR.strings.eh_boost_page_help_message, onDismissRequest)
        }
        ReaderViewModel.Dialog.RetryAllHelp -> {
            HelpDialog(SYMR.strings.eh_retry_all_help, SYMR.strings.eh_retry_all_help_message, onDismissRequest)
        }
        // SY <--
        null -> {}
    }
}

// Replaces any earlier mode toast so quick successive changes don't queue up.
private fun ReaderActivity.showModeToast(stringRes: StringResource, unless: Boolean) {
    menuToggleToast?.cancel()
    if (!unless) {
        menuToggleToast = toast(stringRes)
    }
}

@Composable
private fun LoadingDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(stringResource(MR.strings.loading))
            }
        },
    )
}

@Composable
private fun ReaderActivity.ReaderChapterListDialog(
    state: ReaderViewModel.State,
    settingsScreenModel: ReaderSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    var chapters by remember { mutableStateOf(viewModel.getChapters()) }
    ChapterListDialog(
        onDismissRequest = onDismissRequest,
        screenModel = settingsScreenModel,
        chapters = chapters,
        onClickChapter = {
            viewModel.loadNewChapterFromDialog(it)
            onDismissRequest()
        },
        onBookmark = { chapter ->
            viewModel.toggleBookmark(chapter.id, !chapter.bookmark)
            chapters = chapters.map {
                if (it.chapter.id == chapter.id) {
                    it.copy(chapter = chapter.copy(bookmark = !chapter.bookmark))
                } else {
                    it
                }
            }
        },
        state.dateRelativeTime,
    )
}

// SY -->
@Composable
private fun HelpDialog(title: StringResource, text: StringResource, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        title = { Text(text = stringResource(title)) },
        text = { Text(text = stringResource(text)) },
    )
}
// SY <--
