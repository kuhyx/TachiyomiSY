package eu.kanade.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.manga.components.PagePreviews
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold

// The tablet layout: the info column on the left, the chapter list on the right.
@Composable
internal fun MangaScreenLargeImpl(layout: MangaScreenLayout) {
    val state = layout.state
    val actions = layout.actions
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val chapters = layout.chapters
    val isAnySelected = layout.isAnySelected
    // SY -->
    val metadataDescription = metadataDescription(state.source)
    // SY <--
    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableIntStateOf(0) }
    val chapterListState = rememberLazyListState()

    BackHandler(enabled = isAnySelected) {
        actions.selection.onAllChapterSelected(false)
    }

    Scaffold(
        topBar = {
            MangaScreenToolbar(
                state = state,
                chapters = chapters,
                actions = actions,
                titleAlphaProvider = { 1f },
                backgroundAlphaProvider = { 1f },
                modifier = Modifier.onSizeChanged { topBarHeight = it.height },
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                SharedMangaBottomActionMenu(chapters = chapters, actions = actions, fillFraction = 0.5f)
            }
        },
        snackbarHost = { SnackbarHost(hostState = layout.snackbarHostState) },
        floatingActionButton = {
            ContinueReadingFab(state, chapters, isAnySelected, chapterListState, actions.info.onContinueReading)
        },
    ) { contentPadding ->
        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = actions.toolbar.onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(
                start = insetPadding.calculateStartPadding(layoutDirection),
                top = with(density) { topBarHeight.toDp() },
                end = insetPadding.calculateEndPadding(layoutDirection),
            ),
        ) {
            TwoPanelBox(
                modifier = Modifier.padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
                startContent = { InfoPanel(layout, contentPadding, metadataDescription) },
                endContent = { ChapterPanel(layout, contentPadding, chapterListState) },
            )
        }
    }
}

@Composable
private fun InfoPanel(
    layout: MangaScreenLayout,
    contentPadding: PaddingValues,
    metadataDescription: MetadataDescriptionComposable?,
) {
    val state = layout.state
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        MangaInfoColumn(
            layout = layout,
            appBarPadding = contentPadding.calculateTopPadding(),
            metadataDescription = metadataDescription,
        )
        // SY -->
        val previews = layout.actions.sy.previews
        if (state.pagePreviewsState !is PagePreviewState.Unused && previews.previewsRowCount > 0) {
            PagePreviews(
                pagePreviewState = state.pagePreviewsState,
                onOpenPage = previews.onOpenPagePreview,
                onMorePreviewsClicked = previews.onMorePreviewsClicked,
                rowCount = previews.previewsRowCount,
            )
        }
        // SY <--
    }
}

@Composable
private fun ChapterPanel(
    layout: MangaScreenLayout,
    contentPadding: PaddingValues,
    chapterListState: LazyListState,
) {
    val chapters = layout.chapters
    val isAnySelected = layout.isAnySelected
    VerticalFastScroller(
        listState = chapterListState,
        topContentPadding = contentPadding.calculateTopPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            state = chapterListState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            chapterHeaderItem(chapters, isAnySelected, layout.actions.toolbar.onFilterButtonClicked)
            sharedChapterItems(
                state = layout.state,
                chapters = layout.listItems,
                isAnyChapterSelected = chapters.fastAny { it.selected },
                chapterSwipeStartAction = layout.chapterSwipeStartAction,
                chapterSwipeEndAction = layout.chapterSwipeEndAction,
                actions = layout.actions.chapters,
            )
        }
    }
}
