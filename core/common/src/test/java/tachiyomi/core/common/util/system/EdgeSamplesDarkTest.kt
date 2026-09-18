package tachiyomi.core.common.util.system

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.Sample.BOTTOM_CENTER
import tachiyomi.core.common.util.system.Sample.BOTTOM_LEFT
import tachiyomi.core.common.util.system.Sample.BOTTOM_LEFT_OFFSET
import tachiyomi.core.common.util.system.Sample.BOTTOM_RIGHT
import tachiyomi.core.common.util.system.Sample.BOTTOM_RIGHT_OFFSET
import tachiyomi.core.common.util.system.Sample.MID_LEFT
import tachiyomi.core.common.util.system.Sample.MID_RIGHT
import tachiyomi.core.common.util.system.Sample.TOP_CENTER
import tachiyomi.core.common.util.system.Sample.TOP_LEFT
import tachiyomi.core.common.util.system.Sample.TOP_LEFT_OFFSET
import tachiyomi.core.common.util.system.Sample.TOP_RIGHT
import tachiyomi.core.common.util.system.Sample.TOP_RIGHT_OFFSET

/** Every operand of the corner and edge conjunctions in [EdgeSamples], one dark sample set each. */
internal class EdgeSamplesDarkTest {
    private fun suggestsDark(vararg dark: Sample): Boolean =
        EdgeSamples(pageWith(BLACK, *dark)).cornersSuggestDark

    @Test
    fun darkTopLeftNeedsOneSupporter() {
        withClue("bottom left") { suggestsDark(TOP_LEFT, BOTTOM_LEFT) shouldBe true }
        withClue("bottom right") { suggestsDark(TOP_LEFT, BOTTOM_RIGHT) shouldBe true }
        withClue("top right") { suggestsDark(TOP_LEFT, TOP_RIGHT) shouldBe true }
        withClue("mid left") { suggestsDark(TOP_LEFT, MID_LEFT) shouldBe true }
        withClue("top center") { suggestsDark(TOP_LEFT, TOP_CENTER) shouldBe true }
        withClue("alone") { suggestsDark(TOP_LEFT) shouldBe false }
        withClue("mid right does not count") { suggestsDark(TOP_LEFT, MID_RIGHT) shouldBe false }
    }

    @Test
    fun darkTopRightNeedsOneSupporter() {
        withClue("bottom right") { suggestsDark(TOP_RIGHT, BOTTOM_RIGHT) shouldBe true }
        withClue("bottom left") { suggestsDark(TOP_RIGHT, BOTTOM_LEFT) shouldBe true }
        withClue("mid right") { suggestsDark(TOP_RIGHT, MID_RIGHT) shouldBe true }
        withClue("top center") { suggestsDark(TOP_RIGHT, TOP_CENTER) shouldBe true }
        withClue("alone") { suggestsDark(TOP_RIGHT) shouldBe false }
        withClue("mid left does not count") { suggestsDark(TOP_RIGHT, MID_LEFT) shouldBe false }
    }

    @Test
    fun whiteTopCornersAreNotDark() {
        suggestsDark() shouldBe false
        suggestsDark(BOTTOM_LEFT, BOTTOM_RIGHT, MID_LEFT, MID_RIGHT, TOP_CENTER) shouldBe false
    }

    @Test
    fun topEdgeNeedsFourDarkSamples() {
        fun top(dark: List<Sample>, support: Int = 0): Boolean =
            EdgeSamples(pageWith(BLACK, *dark.toTypedArray())).topEdgeIsDark(support)
        val four = listOf(TOP_LEFT, TOP_RIGHT, TOP_LEFT_OFFSET, TOP_RIGHT_OFFSET)
        top(emptyList()) shouldBe false
        top(four.take(1)) shouldBe false
        top(four.take(2)) shouldBe false
        top(four.take(3)) shouldBe false
        top(four) shouldBe false
        top(four + TOP_CENTER) shouldBe true
        top(four, support = 10) shouldBe true
        top(four, support = 9) shouldBe false
    }

    @Test
    fun bottomEdgeNeedsFourDarkSamples() {
        fun bottom(dark: List<Sample>, support: Int = 0): Boolean =
            EdgeSamples(pageWith(BLACK, *dark.toTypedArray())).bottomEdgeIsDark(support)
        val four = listOf(BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT_OFFSET, BOTTOM_RIGHT_OFFSET)
        bottom(emptyList()) shouldBe false
        bottom(four.take(1)) shouldBe false
        bottom(four.take(2)) shouldBe false
        bottom(four.take(3)) shouldBe false
        bottom(four) shouldBe false
        bottom(four + BOTTOM_CENTER) shouldBe true
        bottom(four, support = 10) shouldBe true
        bottom(four, support = 9) shouldBe false
    }
}
