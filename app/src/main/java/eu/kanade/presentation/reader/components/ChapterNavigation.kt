package eu.kanade.presentation.reader.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// Chapter/page controls shared by the horizontal and vertical navigators.
internal data class ChapterNavigation(
    val onNextChapter: () -> Unit,
    val enabledNext: Boolean,
    val onPreviousChapter: () -> Unit,
    val enabledPrevious: Boolean,
    val onPageIndexChange: (Int) -> Unit,
    val onPageIndexChangeFinished: () -> Unit,
)
