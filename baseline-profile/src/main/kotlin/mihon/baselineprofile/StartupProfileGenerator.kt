package mihon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the Startup Profile (the classes touched before the first frame) on a device;
 * it never runs on the JVM.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
internal class StartupProfileGenerator {

    /** Collects the profile for [TARGET_PACKAGE_NAME] while [generate] launches the app. */
    @get:Rule
    val rule: BaselineProfileRule = BaselineProfileRule()

    /** A cold launch from the home screen is the whole startup path. */
    @Test
    fun generate() {
        rule.collect(
            packageName = TARGET_PACKAGE_NAME,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
