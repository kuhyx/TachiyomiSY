package eu.kanade.tachiyomi.util.system

import android.os.Build
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
    fun samsungNeedsSOrMaterial() {
        dynamicColorAvailable(sdkInt = S, isSamsung = true, materialAvailable = false) shouldBe true
        dynamicColorAvailable(sdkInt = S, isSamsung = false, materialAvailable = false) shouldBe false
        dynamicColorAvailable(sdkInt = S - 1, isSamsung = true, materialAvailable = false) shouldBe false
        dynamicColorAvailable(sdkInt = S - 1, isSamsung = true, materialAvailable = true) shouldBe true
    }

    private companion object {
        const val S = Build.VERSION_CODES.S
    }
}
