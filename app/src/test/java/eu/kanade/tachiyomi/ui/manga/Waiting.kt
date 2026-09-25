package eu.kanade.tachiyomi.ui.manga

import android.os.Looper
import org.robolectric.Shadows.shadowOf

private const val WAIT_MS = 20_000L

/**
 * Waits until [condition] holds, polling. On the main thread it also runs what the code under test
 * posted to the main looper meanwhile, since `Dispatchers.Main` is that looper under Robolectric.
 */
internal fun eventually(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + WAIT_MS
    val onMain = Looper.myLooper() == Looper.getMainLooper()
    while (true) {
        if (onMain) shadowOf(Looper.getMainLooper()).idle()
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "condition not met in time" }
        Thread.sleep(10)
    }
}
