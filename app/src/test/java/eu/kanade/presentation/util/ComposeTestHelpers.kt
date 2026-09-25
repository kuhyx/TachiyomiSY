package eu.kanade.presentation.util

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performSemanticsAction

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

/** Clicks through the semantics action, for nodes another node overlaps or that sit off-screen. */
internal fun SemanticsNodeInteraction.invokeClick(): SemanticsNodeInteraction =
    performSemanticsAction(SemanticsActions.OnClick)

/**
 * Recomposes with the same argument instances (bumping [tick]), then with each [swaps] applied in
 * turn, so every lambda the Compose compiler memoizes takes both its unchanged and changed arm.
 * The content must read [tick] next to the call under test, directly and through a host that
 * takes the arguments as parameters.
 */
internal fun ComposeContentTestRule.recomposeAll(tick: MutableIntState, vararg swaps: () -> Unit) {
    runOnIdle { tick.intValue++ }
    waitForIdle()
    swaps.forEach { swap ->
        runOnIdle(swap)
        waitForIdle()
        runOnIdle { tick.intValue++ }
        waitForIdle()
    }
}
