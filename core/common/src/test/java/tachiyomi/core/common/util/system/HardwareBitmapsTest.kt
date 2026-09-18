package tachiyomi.core.common.util.system

import eu.kanade.tachiyomi.util.system.GLUtil
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** On the plain JVM `Build.VERSION.SDK_INT` is 0, so the device list is never consulted. */
internal class HardwareBitmapsTest {
    @AfterEach
    fun tearDown() {
        HardwareBitmaps.hardwareBitmapThreshold = GLUtil.SAFE_TEXTURE_LIMIT
    }

    @Test
    fun thresholdStartsAtTheSafeLimit() {
        HardwareBitmaps.hardwareBitmapThreshold shouldBe GLUtil.SAFE_TEXTURE_LIMIT
        HardwareBitmaps.HARDWARE_BITMAP_UNSUPPORTED shouldBe false
    }

    @Test
    fun largerSideIsCompared() {
        HardwareBitmaps.canUseHardwareBitmap(width = 2048, height = 10) shouldBe true
        HardwareBitmaps.canUseHardwareBitmap(width = 10, height = 2049) shouldBe false
        HardwareBitmaps.canUseHardwareBitmap(width = 2049, height = 10) shouldBe false
    }

    @Test
    fun thresholdCanBeLowered() {
        HardwareBitmaps.hardwareBitmapThreshold = 100
        HardwareBitmaps.canUseHardwareBitmap(width = 100, height = 100) shouldBe true
        HardwareBitmaps.canUseHardwareBitmap(width = 101, height = 1) shouldBe false
    }
}
