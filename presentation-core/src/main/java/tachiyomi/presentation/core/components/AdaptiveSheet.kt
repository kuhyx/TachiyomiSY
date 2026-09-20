package tachiyomi.presentation.core.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal val SheetMaxWidth: Dp = 460.dp
private val DialogVerticalPadding: Dp = 16.dp

/**
 * A modal sheet: a centred dialog that fades in on tablets, a bottom sheet that slides up and
 * can be swiped away on phones. Tapping outside, or Back, requests dismissal.
 */
@Composable
public fun AdaptiveSheet(
    isTabletUi: Boolean,
    enableSwipeDismiss: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isTabletUi) {
        DialogSheet(onDismissRequest = onDismissRequest, modifier = modifier, content = content)
    } else {
        BottomSheet(
            enableSwipeDismiss = enableSwipeDismiss,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
private fun DialogSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var targetAlpha by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = SheetAnimationSpec,
        label = "alpha",
    )
    val internalOnDismissRequest: () -> Unit = {
        scope.launch {
            targetAlpha = 0f
            onDismissRequest()
        }
    }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = internalOnDismissRequest,
            )
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .requiredWidthIn(max = SheetMaxWidth)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {},
                )
                .systemBarsPadding()
                .padding(vertical = DialogVerticalPadding)
                .then(modifier),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = {
                BackHandler(
                    enabled = remember { derivedStateOf { alpha > 0f } }.value,
                    onBack = internalOnDismissRequest,
                )
                content()
            },
        )

        LaunchedEffect(Unit) {
            targetAlpha = 1f
        }
    }
}
