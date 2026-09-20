package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.reader.ChapterListDialog
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReaderPageIndicator
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setComposeContent
import exh.ui.ifSourcesLoaded
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
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

        val onDismissRequest = viewModel::closeDialog
        when (state.dialog) {
            is ReaderViewModel.Dialog.Loading -> {
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
                    onChange = { stringRes ->
                        menuToggleToast?.cancel()
                        if (!readerPreferences.showReadingMode.get()) {
                            menuToggleToast = toast(stringRes)
                        }
                    },
                )
            }

            is ReaderViewModel.Dialog.OrientationModeSelect -> {
                OrientationSelectDialog(
                    onDismissRequest = onDismissRequest,
                    screenModel = settingsScreenModel,
                    onChange = { stringRes ->
                        menuToggleToast?.cancel()
                        menuToggleToast = toast(stringRes)
                    },
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
                    hasExtraPage = (state.dialog as? ReaderViewModel.Dialog.PageActions)?.extraPage != null,
                )
            }

            is ReaderViewModel.Dialog.ChapterList -> {
                var chapters by remember {
                    mutableStateOf(viewModel.getChapters())
                }
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
            ReaderViewModel.Dialog.AutoScrollHelp -> {
                AlertDialog(
                    onDismissRequest = onDismissRequest,
                    confirmButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    },
                    title = { Text(text = stringResource(SYMR.strings.eh_autoscroll_help)) },
                    text = { Text(text = stringResource(SYMR.strings.eh_autoscroll_help_message)) },
                )
            }

            ReaderViewModel.Dialog.BoostPageHelp -> {
                AlertDialog(
                    onDismissRequest = onDismissRequest,
                    confirmButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    },
                    title = { Text(text = stringResource(SYMR.strings.eh_boost_page_help)) },
                    text = { Text(text = stringResource(SYMR.strings.eh_boost_page_help_message)) },
                )
            }

            ReaderViewModel.Dialog.RetryAllHelp -> {
                AlertDialog(
                    onDismissRequest = onDismissRequest,
                    confirmButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    },
                    title = { Text(text = stringResource(SYMR.strings.eh_retry_all_help)) },
                    text = { Text(text = stringResource(SYMR.strings.eh_retry_all_help_message)) },
                )
            }
            // SY <--
            null -> {}
        }
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

@Composable
internal fun ReaderActivity.AppBars(state: ReaderViewModel.State) {
    if (!ifSourcesLoaded()) {
        return
    }

    val isHttpSource = viewModel.getSource() is HttpSource

    val cropBorderPaged by readerPreferences.cropBorders.collectAsState()
    val cropBorderWebtoon by readerPreferences.cropBordersWebtoon.collectAsState()
    val isPagerType = ReadingMode.isPagerType(viewModel.viewerSettings.getMangaReadingMode())

    // SY -->
    val readingMode = viewModel.viewerSettings.getMangaReadingMode()
    val isWebtoon = ReadingMode.WEBTOON.flagValue == readingMode
    val cropBorderContinuousVertical by readerPreferences.cropBordersContinuousVertical.collectAsState()
    val cropEnabled = if (isPagerType) {
        cropBorderPaged
    } else if (isWebtoon) {
        cropBorderWebtoon
    } else {
        cropBorderContinuousVertical
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

    ReaderAppBars(
        visible = state.menuVisible,

        mangaTitle = state.manga?.title,
        chapterTitle = state.currentChapter?.chapter?.name,
        navigateUp = onBackPressedDispatcher::onBackPressed,
        onClickTopAppBar = ::openMangaScreen,
        // bookmarked = state.bookmarked,
        // onToggleBookmarked = viewModel::toggleChapterBookmark,
        onOpenInWebView = ::openChapterInWebView.takeIf { isHttpSource },
        onOpenInBrowser = ::openChapterInBrowser.takeIf { isHttpSource },
        onShare = ::shareChapter.takeIf { isHttpSource },

        chapterNavigatorType = if (!verticalNavigator) {
            if (state.viewer is R2LPagerViewer) {
                ChapterNavigatorType.HORIZONTAL_RTL
            } else {
                ChapterNavigatorType.HORIZONTAL_LTR
            }
        } else {
            if (verticalNavigatorOnLeft) {
                ChapterNavigatorType.VERTICAL_LEFT
            } else {
                ChapterNavigatorType.VERTICAL_RIGHT
            }
        },
        onNextChapter = ::loadNextChapter,
        enabledNext = state.viewerChapters?.nextChapter != null,
        onPreviousChapter = ::loadPreviousChapter,
        enabledPrevious = state.viewerChapters?.prevChapter != null,
        currentPage = state.currentPage,
        totalPages = state.totalPages,
        onPageIndexChange = {
            isScrollingThroughPages = true
            moveToPageIndex(it)
        },
        onPageIndexChangeFinished = {
            isScrollingThroughPages = false
        },

        readingMode = ReadingMode.fromPreference(
            viewModel.viewerSettings.getMangaReadingMode(resolveDefault = false),
        ),
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
        onClickSettings = viewModel::openSettingsDialog,
        // SY -->
        isExhToolsVisible = state.ehUtilsVisible,
        onSetExhUtilsVisibility = viewModel::showEhUtils,
        isAutoScroll = state.autoScroll,
        isAutoScrollEnabled = state.isAutoScrollEnabled,
        onToggleAutoscroll = viewModel::toggleAutoScroll,
        autoScrollFrequency = state.ehAutoscrollFreq,
        onSetAutoScrollFrequency = viewModel::setAutoScrollFrequency,
        onClickAutoScrollHelp = viewModel::openAutoScrollHelpDialog,
        onClickRetryAll = ::exhRetryAll,
        onClickRetryAllHelp = viewModel::openRetryAllHelp,
        onClickBoostPage = ::exhBoostPage,
        onClickBoostPageHelp = viewModel::openBoostPageHelp,
        currentPageText = state.currentPageText,
        enabledButtons = readerBottomButtons,
        currentReadingMode = ReadingMode.fromPreference(
            viewModel.viewerSettings.getMangaReadingMode(resolveDefault = true),
        ),
        dualPageSplitEnabled = dualPageSplitPaged,
        doublePages = state.doublePages,
        onClickChapterList = viewModel::openChapterListDialog,
        onClickPageLayout = {
            if (readerPreferences.pageLayout.get() == PagerConfig.PageLayout.AUTOMATIC) {
                (viewModel.state.value.viewer as? PagerViewer)?.config?.let { config ->
                    config.doublePages = !config.doublePages
                    reloadChapters(config.doublePages, true)
                }
            } else {
                readerPreferences.pageLayout.set(1 - readerPreferences.pageLayout.get())
            }
        },
        onClickShiftPage = ::shiftDoublePages,
        // SY <--
    )
}
