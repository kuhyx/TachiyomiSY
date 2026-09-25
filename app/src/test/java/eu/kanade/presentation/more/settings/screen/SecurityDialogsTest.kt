package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.hasAnyAncestor
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SecurityDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.isPasswordSetState(any()) } returns MutableStateFlow(true)
        every { CbzCrypto.deleteKeyCbz() } just runs
        every { CbzCrypto.encryptCbz(any()) } answers { "enc:" + firstArg<String>() }
        koin.start()
        koin.security.useAuthenticator.set(true)
        harness.show(SettingsSecurityScreen)
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun field() = compose.onNode(hasSetTextAction())

    @Test
    fun passwordConfirmStoresIt() {
        harness.click("Set CBZ archive password")
        field().performTextReplacement("pw")
        compose.onNode(hasClickAction() and hasAnyAncestor(hasSetTextAction())).performClick()
        compose.onNode(hasClickAction() and hasAnyAncestor(hasSetTextAction())).performClick()
        field().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        koin.security.cbzPassword.get() shouldBe "enc:pw"
        harness.count("CBZ archive password") shouldBe 0
    }

    @Test
    fun passwordImeActionStoresIt() {
        harness.click("Set CBZ archive password")
        field().performTextReplacement("ime")
        field().performImeAction()
        compose.waitForIdle()
        koin.security.cbzPassword.get() shouldBe "enc:ime"
    }

    @Test
    fun passwordCancel() {
        harness.click("Set CBZ archive password")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("CBZ archive password") shouldBe 0
    }

    @Test
    fun lockDaysToggleAndStore() {
        koin.security.authenticatorDays.set(0)
        compose.waitForIdle()
        harness.click("Biometric lock days")
        compose.onNodeWithText("Monday").performClick()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        koin.security.authenticatorDays.get() shouldBe
            SettingsSecurityScreen.DayOption.Monday.day
    }

    @Test
    fun lockDaysCancel() {
        harness.click("Biometric lock days")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Monday") shouldBe 0
    }
}
