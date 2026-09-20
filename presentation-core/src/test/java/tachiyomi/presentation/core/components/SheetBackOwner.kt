package tachiyomi.presentation.core.components

import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

/**
 * A stand-alone Back dispatcher for sheet tests. `BackHandler` registers with
 * `LocalNavigationEventDispatcherOwner` first (the activity's, through the view tree), so tests
 * must provide this owner; pressing Back with no enabled handler is then a no-op instead of
 * finishing the activity under the test.
 */
internal class SheetBackOwner : NavigationEventDispatcherOwner {
    private val input = DirectNavigationEventInput()

    override val navigationEventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()

    init {
        navigationEventDispatcher.addInput(input)
    }

    fun pressBack() {
        input.backCompleted()
    }
}
