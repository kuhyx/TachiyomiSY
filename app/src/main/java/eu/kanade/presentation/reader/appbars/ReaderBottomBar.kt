package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

// Bottom-bar buttons that toggle a reader setting, with the value they currently display.
internal data class ReaderSettingButtons(
    val readingMode: ReadingMode,
    val onClickReadingMode: () -> Unit,
    val orientation: ReaderOrientation,
    val onClickOrientation: () -> Unit,
    val cropEnabled: Boolean,
    val onClickCropBorder: () -> Unit,
)

// SY --> Which of the fork's optional bottom buttons are shown, and the state they depend on.
internal data class SyBottomBarState(
    val enabledButtons: Set<String>,
    val currentReadingMode: ReadingMode,
    val dualPageSplitEnabled: Boolean,
    val doublePages: Boolean,
)

internal data class SyBottomBarActions(
    val onClickChapterList: () -> Unit,
    val onClickWebView: (() -> Unit)?,
    val onClickBrowser: (() -> Unit)?,
    val onClickShare: (() -> Unit)?,
    val onClickPageLayout: () -> Unit,
    val onClickShiftPage: () -> Unit,
)
// SY <--

@Composable
internal fun ReaderBottomBar(
    settings: ReaderSettingButtons,
    onClickSettings: () -> Unit,
    // SY -->
    sy: SyBottomBarState,
    syActions: SyBottomBarActions,
    // SY <--
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .pointerInput(Unit) {},
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // SY -->
        ChapterLinkButtons(enabledButtons = sy.enabledButtons, actions = syActions)
        // SY <--
        SettingButtons(settings = settings, sy = sy)
        // SY -->
        PageLayoutButtons(sy = sy, actions = syActions)
        // SY <--

        IconButton(onClick = onClickSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}

// SY --> Chapter list, web view, browser and share; each only when enabled in settings (and available).
@Composable
private fun ChapterLinkButtons(enabledButtons: Set<String>, actions: SyBottomBarActions) {
    if (ReaderBottomButton.ViewChapters.isIn(enabledButtons)) {
        IconButton(onClick = actions.onClickChapterList) {
            Icon(
                imageVector = Icons.Outlined.FormatListNumbered,
                contentDescription = stringResource(MR.strings.chapters),
            )
        }
    }

    if (ReaderBottomButton.WebView.isIn(enabledButtons) && actions.onClickWebView != null) {
        IconButton(onClick = actions.onClickWebView) {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = stringResource(MR.strings.action_open_in_web_view),
            )
        }
    }

    if (ReaderBottomButton.Browser.isIn(enabledButtons) && actions.onClickBrowser != null) {
        IconButton(onClick = actions.onClickBrowser) {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = stringResource(MR.strings.action_open_in_browser),
            )
        }
    }

    if (ReaderBottomButton.Share.isIn(enabledButtons) && actions.onClickShare != null) {
        IconButton(onClick = actions.onClickShare) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(MR.strings.action_share),
            )
        }
    }
}
// SY <--

@Composable
private fun SettingButtons(settings: ReaderSettingButtons, sy: SyBottomBarState) {
    val enabledButtons = sy.enabledButtons
    if (ReaderBottomButton.ReadingMode.isIn(enabledButtons)) {
        IconButton(onClick = settings.onClickReadingMode) {
            Icon(
                painter = painterResource(settings.readingMode.iconRes),
                contentDescription = stringResource(MR.strings.viewer),
            )
        }
    }

    if (ReaderBottomButton.Rotation.isIn(enabledButtons)) {
        IconButton(onClick = settings.onClickOrientation) {
            Icon(
                imageVector = settings.orientation.icon,
                contentDescription = stringResource(MR.strings.pref_rotation_type),
            )
        }
    }

    val cropBorders = when (sy.currentReadingMode) {
        ReadingMode.WEBTOON -> ReaderBottomButton.CropBordersWebtoon
        ReadingMode.CONTINUOUS_VERTICAL -> ReaderBottomButton.CropBordersContinuesVertical
        else -> ReaderBottomButton.CropBordersPager
    }
    if (cropBorders.isIn(enabledButtons)) {
        IconButton(onClick = settings.onClickCropBorder) {
            Icon(
                painter = painterResource(
                    if (settings.cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp,
                ),
                contentDescription = stringResource(MR.strings.pref_crop_borders),
            )
        }
    }
}

// SY --> Page layout picker (pager modes without dual-page split) and the double-page shift.
@Composable
private fun PageLayoutButtons(sy: SyBottomBarState, actions: SyBottomBarActions) {
    if (
        !sy.dualPageSplitEnabled &&
        ReaderBottomButton.PageLayout.isIn(sy.enabledButtons) &&
        ReadingMode.isPagerType(sy.currentReadingMode.flagValue)
    ) {
        IconButton(onClick = actions.onClickPageLayout) {
            Icon(
                painter = painterResource(R.drawable.ic_book_open_variant_24dp),
                contentDescription = stringResource(SYMR.strings.page_layout),
            )
        }
    }

    if (sy.doublePages) {
        IconButton(onClick = actions.onClickShiftPage) {
            Icon(
                painter = painterResource(R.drawable.ic_page_next_outline_24dp),
                contentDescription = stringResource(SYMR.strings.shift_double_pages),
            )
        }
    }
}
// SY <--
