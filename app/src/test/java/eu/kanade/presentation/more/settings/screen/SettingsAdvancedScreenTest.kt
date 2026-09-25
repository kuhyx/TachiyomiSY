package eu.kanade.presentation.more.settings.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.PowerManager
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.more.settings.screen.advanced.ClearDatabaseScreen
import eu.kanade.presentation.more.settings.screen.debug.DebugInfoScreen
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import eu.kanade.tachiyomi.util.CrashLogUtil
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsAdvancedScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val advanced = AdvancedScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkConstructor(CrashLogUtil::class)
        coEvery { anyConstructed<CrashLogUtil>().dumpLogs(any()) } just runs
        stubTextureLimits()
        koin.start(advanced.module())
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun toast(): String = ShadowToast.getTextOfLatestToast().toString()

    @Test
    fun topRowsNavigateAndShare() {
        harness.show(SettingsAdvancedScreen)
        harness.click("Share crash logs")
        coVerify(timeout = 5_000) { anyConstructed<CrashLogUtil>().dumpLogs(any()) }
        harness.click("Debug info")
        verify { harness.navigator.push(any<DebugInfoScreen>()) }
        harness.click("Onboarding guide")
        verify { harness.navigator.push(any<OnboardingScreen>()) }
        harness.click("Manage notifications")
        shadowOf(koin.context).nextStartedActivity.action shouldBe "android.settings.APP_NOTIFICATION_SETTINGS"
    }

    @Test
    fun batteryOptimizationStates() {
        harness.show(SettingsAdvancedScreen)
        harness.click("Disable battery optimization")
        shadowOf(koin.context).nextStartedActivity.action shouldBe
            "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
        val power = koin.context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIgnoringBatteryOptimizations(koin.context.packageName, true)
        harness.click("Disable battery optimization")
        toast() shouldBe "Battery optimization is already disabled"
    }

    @Test
    fun batterySettingsMissing() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent?) {
                throw ActivityNotFoundException()
            }
        }
        harness.show(SettingsAdvancedScreen, context = context)
        harness.click("Disable battery optimization")
        toast() shouldBe "Couldn't open device settings"
    }

    @Test
    fun dataRows() {
        harness.show(SettingsAdvancedScreen)
        harness.click("Reindex downloads")
        verify { advanced.downloadCache.invalidateCache() }
        toast() shouldBe "Recreating download index"
        harness.click("Clear database")
        verify { harness.navigator.push(any<ClearDatabaseScreen>()) }
    }

    @Test
    fun dontKillMyAppOpensSite() {
        harness.show(SettingsAdvancedScreen)
        harness.click("Don't kill my app!")
        verify { harness.uriHandler.openUri("https://dontkillmyapp.com/") }
    }
}
