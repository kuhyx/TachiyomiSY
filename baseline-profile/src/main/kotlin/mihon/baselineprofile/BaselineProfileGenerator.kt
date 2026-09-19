package mihon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records a Baseline Profile by walking the app's bottom-navigation tabs on a device
 * (`./gradlew :app:generateBaselineProfile`); it never runs on the JVM.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
internal class BaselineProfileGenerator {

    /** Collects the profile for [TARGET_PACKAGE_NAME] while [generate] drives the UI. */
    @get:Rule
    val rule: BaselineProfileRule = BaselineProfileRule()

    /** Opens Updates, History, Browse > Extensions and More so their code paths land in the profile. */
    @Test
    fun generate() {
        rule.collect(TARGET_PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()

            device.waitAndClick(By.text("Updates"))
            device.waitForIdle()

            device.waitAndClick(By.text("History"))
            device.waitForIdle()

            device.waitAndClick(By.text("Browse"))
            device.waitForIdle()
            device.waitAndClick(By.text("Extensions"))
            device.waitForIdle()

            device.waitAndClick(By.text("More"))
            device.waitForIdle()
        }
    }
}

private fun UiDevice.waitAndClick(by: BySelector) {
    wait(Until.findObject(by), UI_WAIT_TIMEOUT_MS).click()
}
