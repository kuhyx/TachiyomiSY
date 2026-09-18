package eu.kanade.tachiyomi.util.system

import android.miui.AppOpsUtils
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import logcat.LogPriority
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.RecordingLogcatLogger

/**
 * The plain JVM has no `android.os.SystemProperties` and null `Build` fields, which is the
 * "not a MIUI, not a Samsung" device. The positive cases live in the Robolectric tests.
 */
internal class DeviceUtilTest {
    private val logger = RecordingLogcatLogger

    @BeforeEach
    fun setUp() {
        logger.start()
    }

    @AfterEach
    fun tearDown() {
        AppOpsUtils.xOptMode = { throw UnsupportedOperationException("not MIUI") }
    }

    @Test
    fun noSystemPropertiesMeansNotMiui() {
        DeviceUtil.isMiui shouldBe false
        DeviceUtil.miuiMajorVersion.shouldBeNull()
    }

    @Test
    fun missingPropertiesAreLogged() {
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe false
        val entry = logger.entries.first()
        entry.priority shouldBe LogPriority.WARN
        entry.message shouldContain "Unable to use SystemProperties.get()"
        entry.message shouldContain "ClassNotFoundException"
    }

    @Test
    fun nullManufacturerIsNotSamsung() {
        DeviceUtil.isSamsung shouldBe false
        DeviceUtil.oneUiVersion.shouldBeNull()
    }

    @Test
    fun oneUiVersionDecodesSemInt() {
        // Samsung firmware: SEM_PLATFORM_INT = 90_000 + major * 10_000 + minor * 100.
        DeviceUtil.oneUiVersion { 130_100 } shouldBe 4.1
        DeviceUtil.oneUiVersion { 90_000 } shouldBe 0.0
        DeviceUtil.oneUiVersion { 80_000 } shouldBe 1.0
        DeviceUtil.oneUiVersion { throw NoSuchFieldException("SEM_PLATFORM_INT") }.shouldBeNull()
    }

    @Test
    fun optimisationAsksAppOps() {
        AppOpsUtils.xOptMode = { true }
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe true
        AppOpsUtils.xOptMode = { false }
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe false
    }

    @Test
    fun appOpsFailureMeansEnabled() {
        DeviceUtil.isMiuiOptimizationDisabled() shouldBe false
    }

    @Test
    fun invalidBrowsersAreListed() {
        DeviceUtil.invalidDefaultBrowsers shouldContain "android"
        DeviceUtil.invalidDefaultBrowsers shouldContainExactly listOf(
            "android",
            "com.hihonor.android.internal.app",
            "com.huawei.android.internal.app",
            "com.zui.resolver",
            "com.transsion.resolver",
            "com.android.intentresolver",
        )
    }
}
