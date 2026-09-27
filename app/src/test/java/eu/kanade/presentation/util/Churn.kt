package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.test.junit4.ComposeContentTestRule

// A static local nothing reads: a new value invalidates its whole subtree, so nothing under it can skip.
private val LocalChurn = staticCompositionLocalOf { 0 }

/**
 * Re-runs hosted composables with unchanged arguments. Compose skips a call whose arguments did not
 * change, which leaves the compiler's "argument unchanged" and "value remembered" arms unreached; a
 * changed static local above the call turns skipping off for the whole subtree.
 */
internal class Churn(private val compose: ComposeContentTestRule) {
    private var tick by mutableIntStateOf(0)

    @Composable
    fun Host(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalChurn provides tick, content = content)
    }

    /** Recomposes everything under [Host] once, every argument unchanged. */
    fun rerun() {
        tick++
        compose.waitForIdle()
    }
}
