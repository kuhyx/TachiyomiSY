package eu.kanade.tachiyomi.util.system

import eu.kanade.tachiyomi.BuildConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The build-type flags of this (debug) unit-test build. */
internal class BuildConfigTest {

    @Test
    fun onlyTheDebugFlagIsSet() {
        BuildConfig.BUILD_TYPE shouldBe "debug"
        isDebugBuildType shouldBe true
        isPreviewBuildType shouldBe false
        isReleaseBuildType shouldBe false
        isBenchmarkBuildType shouldBe false
    }

    // The benchmark flag has an inline getter, so its own line belongs to the compiled copy.
    @Test
    fun theCompiledBenchmarkGetter() {
        val getter = Class.forName("eu.kanade.tachiyomi.util.system.BuildConfigKt")
            .getDeclaredMethod("isBenchmarkBuildType")
        getter.isAccessible = true
        getter.invoke(null) shouldBe false
    }
}
