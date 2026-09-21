package eu.kanade.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.manga.components.pagePreviewItems
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold

// The phone layout: one scrolling list with the info column above the chapters.
@Composable
internal fun MangaScreenSmallImpl(layout: MangaScreenLayout) {
    val state = layout.state
    val actions = layout.actions
    val chapterListState = rememberLazyListState()
    val chapters = layout.chapters
    val isAnySelected = layout.isAnySelected
    // SY -->
    val metadataDescription = metadataDescription(state.source)
    var maxWidth by remember { mutableStateOf(Dp.Hairline) }
    // SY <--

    BackHandler(enabled = isAnySelected) {
        actions.selection.onAllChapterSelected(false)
    }

    Scaffold(
        topBar = { CollapsingToolbar(layout, chapterListState) },
        bottomBar = { SharedMangaBottomActionMenu(chapters = chapters, actions = actions, fillFraction = 1f) },
        snackbarHost = { SnackbarHost(hostState = layout.snackbarHostState) },
        floatingActionButton = {
            ContinueReadingFab(state, chapters, isAnySelected, chapterListState, actions.info.onContinueReading)
        },
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()
        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = actions.toolbar.onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = chapterListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = chapterListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    infoItems(layout, topPadding, metadataDescription, maxWidth) { maxWidth = it }
                    chapterHeaderItem(chapters, isAnySelected, actions.toolbar.onFilterButtonClicked)
                    sharedChapterItems(
                        state = state,
                        chapters = layout.listItems,
                        isAnyChapterSelected = chapters.fastAny { it.selected },
                        chapterSwipeStartAction = layout.chapterSwipeStartAction,
                        chapterSwipeEndAction = layout.chapterSwipeEndAction,
                        actions = actions.chapters,
                    )
                }
            }
        }
    }
}

// The toolbar fades its title and background in once the info box scrolls away.
@Composable
private fun CollapsingToolbar(layout: MangaScreenLayout, chapterListState: LazyListState) {
    val isFirstItemVisible by remember {
        derivedStateOf { chapterListState.firstVisibleItemIndex == 0 }
    }
    val isFirstItemScrolled by remember {
        derivedStateOf { chapterListState.firstVisibleItemScrollOffset > 0 }
    }
    val titleAlpha by animateFloatAsState(
        if (!isFirstItemVisible) 1f else 0f,
        label = "Top Bar Title",
    )
    val backgroundAlpha by animateFloatAsState(
        if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
        label = "Top Bar Background",
    )
    MangaScreenToolbar(
        state = layout.state,
        chapters = layout.chapters,
        actions = layout.actions,
        titleAlphaProvider = { titleAlpha },
        backgroundAlphaProvider = { backgroundAlpha },
    )
}

// The info column as list items, so it scrolls with the chapters and the SY page previews can size to the width.
private fun LazyListScope.infoItems(
    layout: MangaScreenLayout,
    topPadding: Dp,
    metadataDescription: MetadataDescriptionComposable?,
    maxWidth: Dp,
    setMaxWidth: (Dp) -> Unit,
) {
    val state = layout.state
    item(key = MangaScreenItem.INFO_BOX, contentType = MangaScreenItem.INFO_BOX) {
        InfoBoxSection(layout, isTabletUi = false, appBarPadding = topPadding)
    }
    item(key = MangaScreenItem.ACTION_ROW, contentType = MangaScreenItem.ACTION_ROW) {
        ActionRowSection(layout)
    }
    // SY -->
    if (metadataDescription != null) {
        item(key = MangaScreenItem.METADATA_INFO, contentType = MangaScreenItem.METADATA_INFO) {
            MetadataSection(layout, metadataDescription)
        }
    }
    // SY <--
    item(key = MangaScreenItem.DESCRIPTION_WITH_TAG, contentType = MangaScreenItem.DESCRIPTION_WITH_TAG) {
        DescriptionSection(layout, defaultExpandState = state.isFromSource)
    }
    // SY -->
    if (!state.showRecommendationsInOverflow || state.showMergeWithAnother) {
        item(key = MangaScreenItem.INFO_BUTTONS, contentType = MangaScreenItem.INFO_BUTTONS) {
            InfoButtonsSection(layout)
        }
    }
    val previews = layout.actions.sy.previews
    if (state.pagePreviewsState !is PagePreviewState.Unused && previews.previewsRowCount > 0) {
        pagePreviewItems(
            pagePreviewState = state.pagePreviewsState,
            onOpenPage = previews.onOpenPagePreview,
            onMorePreviewsClicked = previews.onMorePreviewsClicked,
            maxWidth = maxWidth,
            setMaxWidth = setMaxWidth,
            rowCount = previews.previewsRowCount,
        )
    }
    // SY <--
}
