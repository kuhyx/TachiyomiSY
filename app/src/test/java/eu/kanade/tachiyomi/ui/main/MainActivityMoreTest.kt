package eu.kanade.tachiyomi.ui.main

import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The debug overlay turned off, the E-Hentai upload warning left off, and idle tasks off the main thread. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MainActivityMoreTest {
    private val rig = MainActivityRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun debugOverlayCanBeOff() {
        val previous = DebugToggles.ENABLE_DEBUG_OVERLAY.enabled
        DebugToggles.ENABLE_DEBUG_OVERLAY.enabled = false
        try {
            val activity = rig.launch().get()
            rig.until { activity.navigator != null }
            rig.frames()
            activity.isFinishing shouldBe false
        } finally {
            DebugToggles.ENABLE_DEBUG_OVERLAY.enabled = previous
        }
    }

    @Test
    fun noWarningNoDialog() {
        rig.harness.exhPreferences.enableExhentai.set(true)
        rig.harness.exhPreferences.exhShowSettingsUploadWarning.set(false)
        idleTasksLeaveDialogOff()
    }

    @Test
    fun noExhentaiNoDialog() {
        rig.harness.exhPreferences.enableExhentai.set(false)
        idleTasksLeaveDialogOff()
    }

    private fun idleTasksLeaveDialogOff() {
        val controller = rig.launch { activity ->
            MainActivity::class.java.getDeclaredField("firstPaint").also { it.isAccessible = true }.set(activity, true)
        }
        rig.until { controller.get().navigator != null }
        verify(timeout = 5_000) { EHentaiUpdateWorker.scheduleBackground(controller.get(), any(), any()) }
        controller.get().runExhConfigureDialog shouldBe false
    }

    @Test
    fun idleTasksNeedMainThread() {
        val activity = rig.launch().get()
        val failure = runBlocking {
            withContext(Dispatchers.Default) {
                shouldThrow<IllegalStateException> { activity.initWhenIdle {} }
            }
        }
        failure.message shouldBe "Can only be called on main thread!"
    }
}
