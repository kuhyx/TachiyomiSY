package eu.kanade.presentation.more.settings.screen

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import exh.log.EHLogLevel
import exh.uconfig.EHConfigurator
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class ConfigureExhDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val eh = EhScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        // The failed upload is logged through XLog, which the app initializes at start-up.
        XLog.init(LogConfiguration.Builder().logLevel(LogLevel.ALL).build(), Printer { _, _, _ -> })
        // The configurator's constructor builds its client through the EH logger, which reads the level.
        EHLogLevel.init(ApplicationProvider.getApplicationContext<Application>())
        mockkConstructor(EHConfigurator::class)
        koin.start(eh.module())
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    // Any change to an uploaded setting after the first composition asks to re-upload the profile.
    private fun changeSetting() {
        harness.show(SettingsEhScreen)
        koin.exh.imageQuality.set("low")
        compose.waitForIdle()
    }

    @Test
    fun warningThenUploadSucceeds() {
        coEvery { anyConstructed<EHConfigurator>().configureAll() } just runs
        changeSetting()
        harness.count("Settings profile note") shouldBe 1
        compose.onNodeWithText("OK").performClick()
        compose.awaitMain(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        koin.exh.exhShowSettingsUploadWarning.get() shouldBe false
        ShadowToast.getTextOfLatestToast().toString() shouldBe "Settings successfully uploaded!"
        compose.awaitMain(timeoutMillis = 10_000) { harness.count("Uploading settings to server") == 0 }
    }

    @Test
    fun failedUploadShowsError() {
        koin.exh.exhShowSettingsUploadWarning.set(false)
        coEvery { anyConstructed<EHConfigurator>().configureAll() } throws IllegalStateException("offline")
        changeSetting()
        compose.awaitMain(timeoutMillis = 10_000) { harness.count("Configuration failed!") == 1 }
        compose.onNodeWithText("configuration process: offline", substring = true).assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        harness.count("Configuration failed!") shouldBe 0
    }

    @Test
    fun failureWithoutMessage() {
        koin.exh.exhShowSettingsUploadWarning.set(false)
        coEvery { anyConstructed<EHConfigurator>().configureAll() } throws IllegalStateException()
        changeSetting()
        compose.awaitMain(timeoutMillis = 10_000) { harness.count("Configuration failed!") == 1 }
        harness.count("An error occurred during the configuration process: ") shouldBe 1
    }
}
