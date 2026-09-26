package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.robolectric.shadows.ShadowLooper

private const val POLL_MILLIS = 20L

/**
 * [ComposeContentTestRule.waitUntil] that also drains the main looper on every poll: work posted to
 * `Dispatchers.Main` from a background coroutine (a toast after an IO call, say) only runs when the
 * looper is idled, which Compose's own idling does not do under Robolectric.
 */
internal fun ComposeContentTestRule.awaitMain(timeoutMillis: Long = 10_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (true) {
        ShadowLooper.idleMainLooper()
        waitForIdle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Condition still not satisfied after $timeoutMillis ms" }
        Thread.sleep(POLL_MILLIS)
    }
}
