package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DeviceUtilExtensionsTest {

    @Test
    fun dynamicColourIsCachedOnce() {
        val first = DeviceUtil.isDynamicColorAvailable
        DeviceUtil.isDynamicColorAvailable shouldBe first
    }

    @Test
    fun samsungOnSOrMaterialDecides() {
        dynamicColorAvailable(sdkInt = 30, isSamsung = true, materialAvailable = false) shouldBe false
        dynamicColorAvailable(sdkInt = 30, isSamsung = false, materialAvailable = true) shouldBe true
        dynamicColorAvailable(sdkInt = 31, isSamsung = false, materialAvailable = false) shouldBe false
        dynamicColorAvailable(sdkInt = 31, isSamsung = true, materialAvailable = false) shouldBe true
    }
}
