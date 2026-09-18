package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

/**
 * A Samsung running MIUI: contrived, but every vendor lazy in [DeviceUtil] is evaluated once per
 * sandbox, and this sandbox exists to read the positive answers. The `instrumentedPackages` value
 * names no real package; it only keeps the sandbox separate from the other DeviceUtil tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["eu.kanade.tachiyomi.util.system.vendor.sandbox"])
internal class DeviceUtilVendorTest {
    @Before
    fun setUp() {
        ShadowSystemProperties.override("ro.miui.ui.version.name", "V140")
        ShadowBuild.setVersionIncremental("V14.0.5.0.TKHMIXM")
        ShadowBuild.setManufacturer("Samsung")
    }

    @Test
    fun miuiComesFromTheProperty() {
        DeviceUtil.isMiui shouldBe true
    }

    @Test
    fun majorVersionFromIncremental() {
        DeviceUtil.miuiMajorVersion shouldBe 14
    }

    @Test
    fun samsungIsMatchedIgnoringCase() {
        DeviceUtil.isSamsung shouldBe true
    }
}
