package eu.kanade.tachiyomi.util.system

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.AuthenticationCallback
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.startAuthentication
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Driving the biometric prompt: the androidx entry point is stubbed and its callback invoked by hand. */
@RunWith(RobolectricTestRunner::class)
internal class AuthenticatorUtilPromptTest {

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val callback = slot<AuthPromptCallback>()

    @Before
    fun setUp() {
        mockkStatic("androidx.biometric.auth.Class2BiometricOrCredentialAuthExtensionsKt")
        mockkStatic(BiometricManager::class)
        val manager = mockk<BiometricManager>()
        every { BiometricManager.from(any()) } returns manager
        every { manager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_SUCCESS
        every {
            activity.startClass2BiometricOrCredentialAuthentication(
                title = any(),
                subtitle = any(),
                description = any(),
                confirmationRequired = any(),
                executor = any(),
                callback = capture(callback),
            )
        } returns mockk<AuthPrompt>()
    }

    @After
    fun tearDown() {
        AuthenticatorUtil.isAuthenticating = false
        unmockkAll()
    }

    @Test
    fun startingMarksTheAppAs() {
        with(AuthenticatorUtil) {
            activity.startAuthentication(title = "Title", callback = object : AuthenticationCallback() {})
        }
        AuthenticatorUtil.isAuthenticating shouldBe true
        callback.isCaptured shouldBe true
    }

    @Test
    fun aSucceedingPromptAuthenticates() = runTest {
        val authenticated = mutableListOf<Boolean>()
        val running = launch {
            authenticated += activity.authenticate("Title")
        }
        testScheduler.advanceUntilIdle()
        callback.captured.onAuthenticationSucceeded(activity, mockk<BiometricPrompt.AuthenticationResult>())
        testScheduler.advanceUntilIdle()
        running.join()
        authenticated shouldBe listOf(true)
        AuthenticatorUtil.isAuthenticating shouldBe false
    }

    @Test
    fun aFailingPromptDoesNot() = runTest {
        val authenticated = mutableListOf<Boolean>()
        val running = launch {
            authenticated += activity.authenticate(title = "Title", subtitle = "Subtitle")
        }
        testScheduler.advanceUntilIdle()
        callback.captured.onAuthenticationError(activity, 1, "denied")
        testScheduler.advanceUntilIdle()
        running.join()
        authenticated shouldBe listOf(false)
        AuthenticatorUtil.isAuthenticating shouldBe false
    }

    @Test
    fun anErrorWithoutAnActivityIs() = runTest {
        val authenticated = mutableListOf<Boolean>()
        val running = launch { authenticated += activity.authenticate("Title") }
        testScheduler.advanceUntilIdle()
        callback.captured.onAuthenticationError(null, 1, "denied")
        testScheduler.advanceUntilIdle()
        running.join()
        authenticated shouldBe listOf(false)
    }
}
