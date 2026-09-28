package eu.kanade.tachiyomi.ui.base

import android.os.Looper
import io.mockk.clearAllMocks
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Destroys the activity and then runs what that destruction queued on the main looper.
 *
 * On destroy, each Compose window cancels its Recomposer, but the Recomposer only leaves Compose's
 * JVM-wide registries (`Recomposer._runningRecomposers`, the snapshot apply observers) once its runner
 * resumes on the main thread. Robolectric drops the looper's queue at the end of a test, so without this
 * idle every destroyed activity stayed reachable from those statics for the rest of the test JVM (heap
 * dump, 2026-09-28: ~280 destroyed ReaderActivity instances, the live heap climbing to the 1 GiB cap).
 */
internal fun ActivityController<*>.release() {
    if (!get().isDestroyed) pause().stop().destroy()
    shadowOf(Looper.getMainLooper()).idle()
}

/**
 * Forgets every call mockk recorded. A relaxed mock records its arguments, suspend continuations included,
 * and a continuation holding an activity whose view model holds that mock is a cycle through mockk's global
 * handler map, which never lets either go. Call at teardown, before `unmockkAll()`.
 */
internal fun forgetRecordedCalls() {
    clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
}
