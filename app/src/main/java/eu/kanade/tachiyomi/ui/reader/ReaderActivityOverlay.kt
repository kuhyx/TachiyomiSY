package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderPageIndicator
import eu.kanade.presentation.reader.appbars.AutoScrollControls
import eu.kanade.presentation.reader.appbars.ExhPageActions
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.appbars.ReaderSettingButtons
import eu.kanade.presentation.reader.appbars.SyBottomBarActions
import eu.kanade.presentation.reader.appbars.SyBottomBarState
import eu.kanade.presentation.reader.components.ChapterNavigation
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.cropBordersContinuousVertical
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.readerBottomButtons
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setComposeContent
import exh.ui.ifSourcesLoaded
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get

/*
 * The Compose layer over the reader: page indicator, the top and bottom app bars with their
 * menus, and the dialogs. Extensions of [ReaderActivity] so the callbacks stay its own.
 */

internal fun ReaderActivity.setComposeOverlay(binding: ReaderActivityBinding) {
    binding.composeOverlay.setComposeContent {
        val state by viewModel.state.collectAsState()
        val showPageNumber by readerPreferences.showPageNumber.collectAsState()
        val settingsScreenModel = remember {
            ReaderSettingsScreenModel(
                readerState = viewModel.state,
                onChangeReadingMode = viewModel.viewerSettings::setMangaReadingMode,
                onChangeOrientation = viewModel.viewerSettings::setMangaOrientationType,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!state.menuVisible && showPageNumber) {
                ReaderPageIndicator(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }

            ContentOverlay(state = state)

            AppBars(state = state)
        }

        ReaderDialogs(state, settingsScreenModel)
    }
}

/**
 * Called when the activity is destroyed. Cleans up the viewer, configuration and any view.
 */

@Composable
internal fun ReaderActivity.ContentOverlay(state: ReaderViewModel.State) {
    val flashOnPageChange by readerPreferences.flashOnPageChange.collectAsState()

    val colorOverlayEnabled by readerPreferences.colorFilter.collectAsState()
    val colorOverlay by readerPreferences.colorFilterValue.collectAsState()
    val colorOverlayMode by readerPreferences.colorFilterMode.collectAsState()
    val colorOverlayBlendMode = remember(colorOverlayMode) {
        ReaderPreferences.ColorFilterMode.getOrNull(colorOverlayMode)?.second
    }

    ReaderContentOverlay(
        brightness = state.brightnessOverlayValue,
        color = colorOverlay.takeIf { colorOverlayEnabled },
        colorBlendMode = colorOverlayBlendMode,
    )

    if (flashOnPageChange) {
        DisplayRefreshHost(hostState = displayRefreshHost)
    }
}

// The reader-preference values the app bars display, read once per composition.
private data class AppBarPrefs(
    val cropEnabled: Boolean,
    val readerBottomButtons: Set<String>,
    val dualPageSplitPaged: Boolean,
    val chapterNavigatorType: ChapterNavigatorType,
)

@Composable
private fun ReaderActivity.rememberAppBarPrefs(state: ReaderViewModel.State): AppBarPrefs {
    val cropBorderPaged by readerPreferences.cropBorders.collectAsState()
    val cropBorderWebtoon by readerPreferences.cropBordersWebtoon.collectAsState()
    val isPagerType = ReadingMode.isPagerType(viewModel.viewerSettings.getMangaReadingMode())
    // SY -->
    val readingMode = viewModel.viewerSettings.getMangaReadingMode()
    val isWebtoon = ReadingMode.WEBTOON.flagValue == readingMode
    val cropBorderContinuousVertical by readerPreferences.cropBordersContinuousVertical.collectAsState()
    val cropEnabled = when {
        isPagerType -> cropBorderPaged
        isWebtoon -> cropBorderWebtoon
        else -> cropBorderContinuousVertical
    }
    val readerBottomButtons by remember {
        readerPreferences.readerBottomButtons.changes()
    }.collectAsState(emptySet())
    val dualPageSplitPaged by readerPreferences.dualPageSplitPaged.collectAsState()
    // SY <--
    val verticalNavigatorModes by readerPreferences.verticalNavigator.collectAsState()
    val verticalNavigator = verticalNavigatorModes.contains(
        ReadingMode.fromPreference(viewModel.viewerSettings.getMangaReadingMode()),
    )
    val verticalNavigatorOnLeft by readerPreferences.verticalNavigatorOnLeft.collectAsState()
    val chapterNavigatorType = when {
        !verticalNavigator && state.viewer is R2LPagerViewer -> ChapterNavigatorType.HORIZONTAL_RTL
        !verticalNavigator -> ChapterNavigatorType.HORIZONTAL_LTR
        verticalNavigatorOnLeft -> ChapterNavigatorType.VERTICAL_LEFT
        else -> ChapterNavigatorType.VERTICAL_RIGHT
    }
    return AppBarPrefs(cropEnabled, readerBottomButtons, dualPageSplitPaged, chapterNavigatorType)
}

// SY -->
private fun ReaderActivity.togglePageLayout() {
    if (readerPreferences.pageLayout.get() == PagerConfig.PageLayout.AUTOMATIC) {
        (viewModel.state.value.viewer as? PagerViewer)?.config?.let { config ->
            config.doublePages = !config.doublePages
            reloadChapters(config.doublePages, true)
        }
    } else {
        readerPreferences.pageLayout.set(1 - readerPreferences.pageLayout.get())
    }
}
// SY <--

@Composable
internal fun ReaderActivity.AppBars(state: ReaderViewModel.State) {
    if (!ifSourcesLoaded()) {
        return
    }
    val isHttpSource = viewModel.getSource() is HttpSource
    val prefs = rememberAppBarPrefs(state)
    ReaderAppBars(
        visible = state.menuVisible,
        mangaTitle = state.manga?.title,
        chapterTitle = state.currentChapter?.chapter?.name,
        navigateUp = onBackPressedDispatcher::onBackPressed,
        onClickTopAppBar = ::openMangaScreen,
        // bookmarked = state.bookmarked,
        // onToggleBookmarked = viewModel::toggleChapterBookmark,
        chapterNavigatorType = prefs.chapterNavigatorType,
        navigation = chapterNavigation(state),
        currentPage = state.currentPage,
        totalPages = state.totalPages,
        settings = readerSettingButtons(cropEnabled = prefs.cropEnabled),
        onClickSettings = viewModel::openSettingsDialog,
        // SY -->
        isExhToolsVisible = state.ehUtilsVisible,
        onSetExhUtilsVisibility = viewModel::showEhUtils,
        autoScroll = AutoScrollControls(
            isAutoScroll = state.autoScroll,
            isAutoScrollEnabled = state.isAutoScrollEnabled,
            onToggleAutoscroll = viewModel::toggleAutoScroll,
            autoScrollFrequency = state.ehAutoscrollFreq,
            onSetAutoScrollFrequency = viewModel::setAutoScrollFrequency,
            onClickHelp = viewModel::openAutoScrollHelpDialog,
        ),
        exhPageActions = ExhPageActions(
            onClickRetryAll = ::exhRetryAll,
            onClickRetryAllHelp = viewModel::openRetryAllHelp,
            onClickBoostPage = ::exhBoostPage,
            onClickBoostPageHelp = viewModel::openBoostPageHelp,
        ),
        currentPageText = state.currentPageText,
        syBottomBar = SyBottomBarState(
            enabledButtons = prefs.readerBottomButtons,
            currentReadingMode = ReadingMode.fromPreference(
                viewModel.viewerSettings.getMangaReadingMode(resolveDefault = true),
            ),
            dualPageSplitEnabled = prefs.dualPageSplitPaged,
            doublePages = state.doublePages,
        ),
        syBottomBarActions = SyBottomBarActions(
            onClickChapterList = viewModel::openChapterListDialog,
            onClickWebView = ::openChapterInWebView.takeIf { isHttpSource },
            onClickBrowser = ::openChapterInBrowser.takeIf { isHttpSource },
            onClickShare = ::shareChapter.takeIf { isHttpSource },
            onClickPageLayout = ::togglePageLayout,
            onClickShiftPage = ::shiftDoublePages,
        ),
        // SY <--
    )
}

private fun ReaderActivity.chapterNavigation(state: ReaderViewModel.State) = ChapterNavigation(
    onNextChapter = ::loadNextChapter,
    enabledNext = state.viewerChapters?.nextChapter != null,
    onPreviousChapter = ::loadPreviousChapter,
    enabledPrevious = state.viewerChapters?.prevChapter != null,
    onPageIndexChange = {
        isScrollingThroughPages = true
        moveToPageIndex(it)
    },
    onPageIndexChangeFinished = { isScrollingThroughPages = false },
)

private fun ReaderActivity.readerSettingButtons(cropEnabled: Boolean) = ReaderSettingButtons(
    readingMode = ReadingMode.fromPreference(viewModel.viewerSettings.getMangaReadingMode(resolveDefault = false)),
    onClickReadingMode = viewModel::openReadingModeSelectDialog,
    orientation = ReaderOrientation.fromPreference(
        viewModel.viewerSettings.getMangaOrientation(resolveDefault = false),
    ),
    onClickOrientation = viewModel::openOrientationSelectDialog,
    cropEnabled = cropEnabled,
    onClickCropBorder = {
        val enabled = viewModel.viewerSettings.toggleCropBorders()
        menuToggleToast?.cancel()
        menuToggleToast = toast(if (enabled) MR.strings.on else MR.strings.off)
    },
)
