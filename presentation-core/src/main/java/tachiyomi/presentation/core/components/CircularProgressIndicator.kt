package tachiyomi.presentation.core.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview

private const val FULL_TURN_DEGREES = 360f
private const val ROTATION_MILLIS = 2000
private const val PREVIEW_STEP = 0.2f

/**
 * A combined [CircularProgressIndicator] that always rotates.
 *
 * By always rotating we give the feedback to the user that the application isn't 'stuck'.
 */
@Composable
public fun RotatingProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = progress() == 0f,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "progressState",
        modifier = modifier,
    ) { indeterminate ->
        if (indeterminate) {
            // Indeterminate
            CircularProgressIndicator()
        } else {
            // Determinate
            val infiniteTransition = rememberInfiniteTransition(label = "infiniteRotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = FULL_TURN_DEGREES,
                animationSpec = infiniteRepeatable(
                    animation = tween(ROTATION_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotation",
            )
            val animatedProgress by animateFloatAsState(
                targetValue = progress(),
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress",
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

/** [RotatingProgressIndicator] with a button cycling through progress values, for the IDE preview. */
@Preview
@Composable
internal fun RotatingProgressPreview() {
    var progress by remember { mutableFloatStateOf(0f) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        progress = if (progress + PREVIEW_STEP < 1f) progress + PREVIEW_STEP else 0f
                    },
                ) {
                    Text("change")
                }
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                RotatingProgressIndicator(progress = { progress })
            }
        }
    }
}
