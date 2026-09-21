package eu.kanade.presentation.webview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kevinnzou.web.LoadingState

// Progress bar pinned to the bottom of a web view's app bar: indeterminate while initialising,
// determinate (optionally animated between values) while loading, gone once finished.
@Composable
internal fun BoxScope.WebViewLoadingIndicator(loadingState: LoadingState, animated: Boolean = false) {
    val modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
    when (loadingState) {
        is LoadingState.Initializing -> {
            LinearProgressIndicator(modifier = modifier)
        }
        is LoadingState.Loading -> {
            val progress = if (animated) {
                val animatedProgress by animateFloatAsState(
                    loadingState.progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "webview_loading",
                )
                animatedProgress
            } else {
                loadingState.progress
            }
            LinearProgressIndicator(progress = { progress }, modifier = modifier)
        }
        else -> {}
    }
}
