package eu.kanade.presentation.reader.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.isTabletUi

@Composable
internal fun ChapterNavigator(
    type: ChapterNavigatorType,
    navigation: ChapterNavigation,
    currentPage: Int,
    // SY -->
    currentPageText: String,
    // SY <--
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val state = key(totalPages) {
        rememberSliderState(
            value = currentPage.toFloat(),
            steps = totalPages - 2,
            trackRange = 1f..totalPages.toFloat(),
        )
    }
    state.value = currentPage.toFloat()

    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(currentPage) {
        if (sliderDragged) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val isTabletUi = isTabletUi()

    // Match with toolbar background color set in ReaderActivity
    val backgroundColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(3.dp)
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    val style = NavigatorStyle(
        state = state,
        interactionSource = interactionSource,
        mainAxisPadding = if (isTabletUi) 24.dp else 8.dp,
        backgroundColor = backgroundColor,
        buttonColor = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
        ),
    )

    if (type.isHorizontal()) {
        HorizontalChapterNavigator(
            isRtl = type == ChapterNavigatorType.HORIZONTAL_RTL,
            navigation = navigation,
            style = style,
            currentPageText = currentPageText,
            totalPages = totalPages,
            modifier = modifier,
        )
    } else {
        VerticalChapterNavigator(
            navigation = navigation,
            style = style,
            currentPageText = currentPageText,
            totalPages = totalPages,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
internal fun ChapterNavigatorPreview() {
    var currentPage by remember { mutableIntStateOf(1) }
    TachiyomiPreviewTheme {
        ChapterNavigator(
            type = ChapterNavigatorType.VERTICAL_RIGHT,
            navigation = ChapterNavigation(
                onNextChapter = {},
                enabledNext = true,
                onPreviousChapter = {},
                enabledPrevious = true,
                onPageIndexChange = { currentPage = it + 1 },
                onPageIndexChangeFinished = {},
            ),
            currentPage = currentPage,
            totalPages = 10,
            // SY -->
            currentPageText = "1",
            // SY <--
        )
    }
}
