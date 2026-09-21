package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.components.ChapterNavigation
import eu.kanade.presentation.reader.components.ChapterNavigator
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import tachiyomi.presentation.core.components.material.padding

private const val SLIDE_MS = 200
private const val FADE_MS = 150
private val readerBarsSlideAnimationSpec = tween<IntOffset>(SLIDE_MS)
private val readerBarsFadeAnimationSpec = tween<Float>(FADE_MS)

@Composable
internal fun ReaderAppBars(
    visible: Boolean,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    // bookmarked: Boolean,
    // onToggleBookmarked: () -> Unit,

    chapterNavigatorType: ChapterNavigatorType,
    navigation: ChapterNavigation,
    currentPage: Int,
    totalPages: Int,

    settings: ReaderSettingButtons,
    onClickSettings: () -> Unit,
    // SY -->
    isExhToolsVisible: Boolean,
    onSetExhUtilsVisibility: (Boolean) -> Unit,
    autoScroll: AutoScrollControls,
    exhPageActions: ExhPageActions,
    currentPageText: String,
    syBottomBar: SyBottomBarState,
    syBottomBarActions: SyBottomBarActions,
    // SY <--
) {
    val backgroundColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(3.dp)
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    val navigator: @Composable () -> Unit = {
        ChapterNavigator(
            type = chapterNavigatorType,
            navigation = navigation,
            currentPage = currentPage,
            // SY -->
            currentPageText = currentPageText,
            // SY <--
            totalPages = totalPages,
        )
    }

    Column(modifier = Modifier.fillMaxHeight()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(readerBarsSlideAnimationSpec) { -it } + fadeIn(readerBarsFadeAnimationSpec),
            exit = slideOutVertically(readerBarsSlideAnimationSpec) { -it } + fadeOut(readerBarsFadeAnimationSpec),
        ) {
            // SY -->
            Column {
                // SY <--
                ReaderTopBar(
                    modifier = Modifier
                        .background(backgroundColor)
                        .clickable(onClick = onClickTopAppBar),
                    mangaTitle = mangaTitle,
                    chapterTitle = chapterTitle,
                    navigateUp = navigateUp,
                    /* SY -->
                    bookmarked = bookmarked,
                    onToggleBookmarked = onToggleBookmarked,
                    onOpenInWebView = onOpenInWebView,
                    onOpenInBrowser = onOpenInBrowser,
                    onShare = onShare,
                    SY <-- */
                )
                // SY -->
                ExhUtils(
                    isVisible = isExhToolsVisible,
                    onSetExhUtilsVisibility = onSetExhUtilsVisibility,
                    backgroundColor = backgroundColor,
                    autoScroll = autoScroll,
                    pageActions = exhPageActions,
                )
            }
            // SY <--
        }

        if (!chapterNavigatorType.isHorizontal()) {
            VerticalNavigatorRail(
                visible = visible,
                sliderOnLeft = chapterNavigatorType == ChapterNavigatorType.VERTICAL_LEFT,
                content = navigator,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        BottomBars(
            visible = visible,
            backgroundColor = backgroundColor,
            settings = settings,
            onClickSettings = onClickSettings,
            syBottomBar = syBottomBar,
            syBottomBarActions = syBottomBarActions,
            content = navigator.takeIf { chapterNavigatorType.isHorizontal() },
        )
    }
}

// Horizontal page slider (when the navigator is horizontal) above the bottom action bar, sliding up.
@Composable
private fun BottomBars(
    visible: Boolean,
    backgroundColor: Color,
    settings: ReaderSettingButtons,
    onClickSettings: () -> Unit,
    syBottomBar: SyBottomBarState,
    syBottomBarActions: SyBottomBarActions,
    content: (@Composable () -> Unit)?,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(readerBarsSlideAnimationSpec) { it } + fadeIn(readerBarsFadeAnimationSpec),
        exit = slideOutVertically(readerBarsSlideAnimationSpec) { it } + fadeOut(readerBarsFadeAnimationSpec),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            content?.invoke()
            ReaderBottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(horizontal = MaterialTheme.padding.small)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                settings = settings,
                onClickSettings = onClickSettings,
                // SY -->
                sy = syBottomBar,
                syActions = syBottomBarActions,
                // SY <--
            )
        }
    }
}

// The vertical page slider docked to one side, sliding in from that side.
@Composable
private fun ColumnScope.VerticalNavigatorRail(
    visible: Boolean,
    sliderOnLeft: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (sliderOnLeft) LayoutDirection.Ltr else LayoutDirection.Rtl,
    ) {
        Row(modifier = Modifier.weight(1f)) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(readerBarsSlideAnimationSpec) { if (sliderOnLeft) -it else it } +
                    fadeIn(readerBarsFadeAnimationSpec),
                exit = slideOutHorizontally(readerBarsSlideAnimationSpec) { if (sliderOnLeft) -it else it } +
                    fadeOut(readerBarsFadeAnimationSpec),
            ) {
                Row {
                    Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        content()
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
