package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The EGL queries behind [GLUtil] are native-bound; only the pure stepping is testable here. */
internal class GLUtilTest {
    @Test
    fun optionsStepDownFromLimit() {
        textureLimitOptions(8192) shouldContainExactly listOf(8192, 7168, 6144, 5120, 4096, 3072, 2048)
    }

    @Test
    fun oddLimitKeepsLowerMultiples() {
        textureLimitOptions(5000) shouldContainExactly listOf(5000, 4096, 3072, 2048)
    }

    @Test
    fun safeLimitIsTheOnlyFloorOption() {
        textureLimitOptions(GLUtil.SAFE_TEXTURE_LIMIT) shouldContainExactly listOf(2048)
        GLUtil.SAFE_TEXTURE_LIMIT shouldBe 2048
    }
}
