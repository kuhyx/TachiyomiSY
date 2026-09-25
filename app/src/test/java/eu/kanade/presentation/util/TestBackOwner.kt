package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * A stand-alone Back dispatcher: `BackHandler` registers with `LocalNavigationEventDispatcherOwner`,
 * so a test provides this owner through [ProvideBack] and presses Back with [pressBack].
 */
internal class TestBackOwner : NavigationEventDispatcherOwner {
    private val input = DirectNavigationEventInput()

    override val navigationEventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()

    init {
        navigationEventDispatcher.addInput(input)
    }

    fun pressBack() {
        input.backCompleted()
    }
}

@Composable
internal fun ProvideBack(owner: TestBackOwner, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
}
