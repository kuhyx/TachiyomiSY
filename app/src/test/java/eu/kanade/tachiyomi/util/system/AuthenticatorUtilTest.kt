package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.AuthenticationCallback
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AuthenticatorUtilTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        AuthenticatorUtil.isAuthenticating = false
        unmockkAll()
    }

    @Test
    fun supportNeedsWeakBiometricsOrA() {
        mockkStatic(BiometricManager::class)
        val manager = mockk<BiometricManager>()
        every { BiometricManager.from(any()) } returns manager
        val authenticators = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
        every { manager.canAuthenticate(authenticators) } returns BiometricManager.BIOMETRIC_SUCCESS
        context.isAuthenticationSupported() shouldBe true
        every { manager.canAuthenticate(authenticators) } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        context.isAuthenticationSupported() shouldBe false
    }

    @Test
    fun theCallbackClearsThe() {
        val callback = object : AuthenticationCallback() {}
        AuthenticatorUtil.isAuthenticating = true
        callback.onAuthenticationError(null, 1, "error")
        AuthenticatorUtil.isAuthenticating shouldBe false
        AuthenticatorUtil.isAuthenticating = true
        callback.onAuthenticationSucceeded(null, mockk<BiometricPrompt.AuthenticationResult>())
        AuthenticatorUtil.isAuthenticating shouldBe false
    }

    @Test
    fun theFlagIsPublicState() {
        AuthenticatorUtil.isAuthenticating shouldBe false
        AuthenticatorUtil.isAuthenticating = true
        AuthenticatorUtil.isAuthenticating shouldBe true
    }

    @Test
    fun anUnsupportedDevice() = runTest {
        mockkStatic(BiometricManager::class)
        val manager = mockk<BiometricManager>()
        every { BiometricManager.from(any()) } returns manager
        every { manager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        val activity = mockk<FragmentActivity>(relaxed = true)
        with(AuthenticatorUtil) { activity.authenticate("Title") shouldBe true }
    }
}
