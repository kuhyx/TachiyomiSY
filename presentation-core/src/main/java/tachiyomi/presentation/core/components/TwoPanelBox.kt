package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val StartPanelMaxWidth: Dp = 450.dp

/**
 * Two side-by-side panels: the start one takes half the width up to 450 dp, the end one the rest.
 *
 * `contentWindowInsets`: insets the panels' outer edges keep clear of; none when `null`.
 */
@Composable
public fun TwoPanelBox(
    startContent: @Composable BoxScope.() -> Unit,
    endContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val direction = LocalLayoutDirection.current
    val padding = (contentWindowInsets ?: WindowInsets(0)).asPaddingValues()
    val startPadding = padding.calculateStartPadding(direction)
    val endPadding = padding.calculateEndPadding(direction)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = maxWidth - startPadding - endPadding
        val firstWidth = (width / 2).coerceAtMost(StartPanelMaxWidth)
        val secondWidth = width - firstWidth
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(firstWidth + startPadding),
            content = startContent,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(secondWidth + endPadding),
            content = endContent,
        )
    }
}
