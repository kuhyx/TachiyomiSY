package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric's stock device: the MIUI property exists but is empty and the manufacturer is
 * "robolectric". Own sandbox (see [DeviceUtilVendorTest]) so nothing else can evaluate the
 * lazies first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["eu.kanade.tachiyomi.util.system.stock.sandbox"])
internal class DeviceUtilStockTest {
    @Test
    fun emptyMiuiPropertyMeansNotMiui() {
        DeviceUtil.isMiui shouldBe false
        DeviceUtil.miuiMajorVersion.shouldBeNull()
    }

    @Test
    fun otherManufacturerIsNotSamsung() {
        DeviceUtil.isSamsung shouldBe false
        DeviceUtil.oneUiVersion.shouldBeNull()
    }
}
