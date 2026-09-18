package tachiyomi.core.common.util.lang

import android.os.Looper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The main-dispatcher launchers need a main looper, which only Robolectric provides. */
@OptIn(DelicateCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CoroutinesExtensionsMainTest {
    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun appScopeLaunchUiUsesMain() {
        var hasRun = false
        val job = launchUI { hasRun = true }
        hasRun shouldBe false
        idleMain()
        job.isCompleted shouldBe true
        hasRun shouldBe true
    }

    @Test
    fun launchNowRunsUndispatched() {
        var hasRun = false
        val job = launchNow { hasRun = true }
        hasRun shouldBe true
        job.isCompleted shouldBe true
    }

    @Test
    fun scopeLaunchUiRunsOnMainLooper() {
        val scope = CoroutineScope(Job())
        var hasRun = false
        val job = scope.launchUI { hasRun = true }
        hasRun shouldBe false
        idleMain()
        job.isCompleted shouldBe true
        hasRun shouldBe true
        scope.cancel()
    }

    @Test
    fun withUiContextReturnsValue() {
        var result = 0
        launchNow { result = withUIContext { 1 + 1 } }
        idleMain()
        result shouldBe 2
    }
}
