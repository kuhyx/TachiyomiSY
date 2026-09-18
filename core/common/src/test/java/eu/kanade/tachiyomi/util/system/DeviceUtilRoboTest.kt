package eu.kanade.tachiyomi.util.system

import android.app.ActivityManager
import android.content.Context
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemProperties

/**
 * The non-lazy members, which are safe to test in the shared sandbox; the vendor lazies live in
 * [DeviceUtilVendorTest] and [DeviceUtilStockTest].
 */
@RunWith(RobolectricTestRunner::class)
internal class DeviceUtilRoboTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun lowRamIsUnderThreeGigabytes() {
        val manager = shadowOf(context.getSystemService(ActivityManager::class.java))
        manager.setMemoryInfo(ActivityManager.MemoryInfo().apply { totalMem = 3L * 1024 * 1024 * 1024 - 1 })
        DeviceUtil.isLowRamDevice(context) shouldBe true
        manager.setMemoryInfo(ActivityManager.MemoryInfo().apply { totalMem = 3L * 1024 * 1024 * 1024 })
        DeviceUtil.isLowRamDevice(context) shouldBe false
    }

    @Test
    fun zeroOrFalsePropertyDisables() {
        ShadowSystemProperties.override("persist.sys.miui_optimization", "0")
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe true
        ShadowSystemProperties.override("persist.sys.miui_optimization", "false")
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe true
    }

    @Test
    fun otherPropertyValuesAskAppOps() {
        ShadowSystemProperties.override("persist.sys.miui_optimization", "1")
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe false
    }
}
