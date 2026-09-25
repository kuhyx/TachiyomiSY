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
}
