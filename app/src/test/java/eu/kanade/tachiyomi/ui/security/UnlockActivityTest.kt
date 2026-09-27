package eu.kanade.tachiyomi.ui.security

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.startAuthentication
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/** The blank lock activity finishes on a successful unlock and closes the task on an error. */
@RunWith(RobolectricTestRunner::class)
internal class UnlockActivityTest {
    private val activityKoin = ActivityKoin()
    private val callback = slot<AuthenticatorUtil.AuthenticationCallback>()
    private val title = slot<String>()
    private val launched = mutableListOf<ActivityController<UnlockActivity>>()

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(activityKoin.module()) }
        mockkObject(AuthenticatorUtil)
        every {
            with(AuthenticatorUtil) {
                any<FragmentActivity>().startAuthentication(capture(title), any(), any(), capture(callback))
            }
        } just runs
    }

    @After
    fun tearDown() {
        launched.forEach { it.pause().stop().destroy() }
        unmockkAll()
        stopKoin()
    }

    private fun launch(): UnlockActivity {
        val controller = Robolectric.buildActivity(UnlockActivity::class.java).setup()
        launched += controller
        return controller.get()
    }

    @Test
    fun successUnlocks() {
        val activity = launch()
        title.captured.startsWith("Unlock ") shouldBe true
        callback.captured.onAuthenticationSucceeded(activity, mockk<BiometricPrompt.AuthenticationResult>())
        activity.isFinishing shouldBe true
    }

    @Test
    fun errorClosesTheTask() {
        val activity = launch()
        callback.captured.onAuthenticationError(activity, BiometricPrompt.ERROR_CANCELED, "cancelled")
        activity.isFinishing shouldBe true
    }
}
