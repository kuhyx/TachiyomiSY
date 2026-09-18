package tachiyomi.core.common.util.system

import android.os.Build
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers

/**
 * [HardwareBitmaps.HARDWARE_BITMAP_UNSUPPORTED] is fixed when the object loads, so each device
 * below gets its own sandbox (the `instrumentedPackages` values name no real package; they only
 * make the configurations distinct) and sets `Build` before the first read.
 */
internal object HardwareBitmapsDevice {
    private val apiLevel = Build.VERSION.SDK_INT

    fun unsupported(sdk: Int, model: String?, device: String): Boolean {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        ShadowBuild.setModel(model)
        ShadowBuild.setDevice(device)
        return HardwareBitmaps.HARDWARE_BITMAP_UNSUPPORTED
    }

    fun restore() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", apiLevel)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_samsung"])
internal class HardwareBitmapsOreoSamsungTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun samsungIsUnsupportedOnOreo() {
        HardwareBitmapsDevice.unsupported(
            sdk = Build.VERSION_CODES.O,
            model = "SAMSUNG-SM-G950F",
            device = "dreamlte",
        ) shouldBe true
        HardwareBitmaps.canUseHardwareBitmap(width = 1, height = 1) shouldBe false
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_listed"])
internal class HardwareBitmapsOreoListedTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun listedIsUnsupportedOnOreo() {
        HardwareBitmapsDevice.unsupported(Build.VERSION_CODES.O, model = "Moto E5", device = "nora") shouldBe true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_other"])
internal class HardwareBitmapsOreoOtherTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun otherDevicesAreSupportedOnOreo() {
        HardwareBitmapsDevice.unsupported(Build.VERSION_CODES.O, model = "Pixel 2", device = "walleye") shouldBe false
        HardwareBitmaps.canUseHardwareBitmap(width = 1, height = 1) shouldBe true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_no_model"])
internal class HardwareBitmapsOreoNoModelTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun missingModelIsSupportedOnOreo() {
        HardwareBitmapsDevice.unsupported(Build.VERSION_CODES.O, model = null, device = "nora") shouldBe false
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_mr1_listed"])
internal class HardwareBitmapsOreoMr1ListedTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun listedIsUnsupportedOnOreoMr1() {
        HardwareBitmapsDevice.unsupported(Build.VERSION_CODES.O_MR1, model = "LG-M320", device = "mcv3") shouldBe true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["sandbox.hardware_bitmaps.oreo_mr1_other"])
internal class HardwareBitmapsOreoMr1OtherTest {
    @After
    fun tearDown() {
        HardwareBitmapsDevice.restore()
    }

    @Test
    fun othersAreSupportedOnOreoMr1() {
        HardwareBitmapsDevice.unsupported(
            sdk = Build.VERSION_CODES.O_MR1,
            model = "SM-G950F",
            device = "dreamlte",
        ) shouldBe false
    }
}
