package eu.kanade.presentation.util

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.ComposeContentTestRule
import androidx.compose.ui.test.performSemanticsAction
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

/** Taps outside the (single) open popup, which dismisses a menu the way a real outside tap does. */
internal fun ComposeContentTestRule.tapOutsidePopup() {
    val popupRoot = onNode(isPopup()).fetchSemanticsNode().root as ViewRootForTest
    val popupWindow = popupRoot.view.parent as View
    runOnUiThread {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_OUTSIDE, 0f, 0f, 0)
        popupWindow.onTouchEvent(event)
        event.recycle()
    }
    waitForIdle()
}

/** Drags the [index]th slider on screen to [value], as a user's drag ending there would. */
internal fun ComposeContentTestRule.setSlider(index: Int, value: Float) {
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[index]
        .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    waitForIdle()
}
