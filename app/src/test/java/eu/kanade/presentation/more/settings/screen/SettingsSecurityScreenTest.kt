package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.fragment.app.FragmentActivity
import eu.kanade.domain.installFakeAndroidKeyStore
import eu.kanade.tachiyomi.ui.category.biometric.BiometricTimesScreen
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsSecurityScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val passwordSet = MutableStateFlow(false)

    @Before
    fun setUp() {
        installFakeAndroidKeyStore()
        mockkObject(CbzCrypto)
        every { CbzCrypto.isPasswordSetState(any()) } returns passwordSet
        every { CbzCrypto.deleteKeyCbz() } just runs
        every { CbzCrypto.encryptCbz(any()) } answers { "enc:" + firstArg<String>() }
        koin.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun activity(): FragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

    @Test
    fun unsupportedAuthDisablesLock() {
        mockkObject(AuthenticatorUtil)
        every { with(AuthenticatorUtil) { any<Context>().isAuthenticationSupported() } } returns false
        harness.show(SettingsSecurityScreen, context = activity())
        harness.item("Require unlock").enabled shouldBe false
        harness.item("Lock when idle").enabled shouldBe false
        harness.switch("Require unlock", value = true) shouldBe true
        harness.list("Lock when idle", 5) shouldBe true
    }

    @Test
    fun supportedAuthFollowsSwitch() {
        mockkObject(AuthenticatorUtil)
        every { with(AuthenticatorUtil) { any<Context>().isAuthenticationSupported() } } returns true
        harness.show(SettingsSecurityScreen)
        harness.item("Require unlock").enabled shouldBe true
        harness.item("Lock when idle").enabled shouldBe false
        koin.security.useAuthenticator.set(true)
        compose.waitForIdle()
        harness.item("Lock when idle").enabled shouldBe true
        harness.item("Edit lock times").enabled shouldBe true
    }

    @Test
    fun cbzRowsFollowPassword() {
        harness.show(SettingsSecurityScreen)
        harness.item("Password protect downloads").enabled shouldBe false
        harness.item("Encryption type").enabled shouldBe false
        passwordSet.value = true
        koin.security.passwordProtectDownloads.set(true)
        compose.waitForIdle()
        harness.item("Password protect downloads").enabled shouldBe true
        harness.item("Encryption type").enabled shouldBe true
    }

    @Test
    fun deletePasswordClears() {
        koin.security.cbzPassword.set("old")
        harness.show(SettingsSecurityScreen)
        harness.click("Delete CBZ archive password")
        verify { CbzCrypto.deleteKeyCbz() }
        koin.security.cbzPassword.get() shouldBe ""
    }

    @Test
    fun lockTimesOpensScreen() {
        harness.show(SettingsSecurityScreen)
        harness.click("Edit lock times")
        verify { harness.navigator.push(any<BiometricTimesScreen>()) }
    }
}
