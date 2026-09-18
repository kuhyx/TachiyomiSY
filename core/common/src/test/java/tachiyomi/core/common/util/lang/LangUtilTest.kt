package tachiyomi.core.common.util.lang

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class LangUtilTest {
    @Test
    fun booleanToLong() {
        true.toLong() shouldBe 1L
        false.toLong() shouldBe 0L
    }

    @Test
    fun collatorIgnoresCase() {
        "apple".compareToWithCollator("APPLE") shouldBe 0
        "apple".compareToWithCollator("banana") shouldBeLessThan 0
        "cherry".compareToWithCollator("banana") shouldBeGreaterThan 0
    }
}
