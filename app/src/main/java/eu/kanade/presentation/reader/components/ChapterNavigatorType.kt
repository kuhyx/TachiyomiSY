package eu.kanade.presentation.reader.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal enum class ChapterNavigatorType {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    ;

    fun isHorizontal() = this in setOf(HORIZONTAL_LTR, HORIZONTAL_RTL)
}
