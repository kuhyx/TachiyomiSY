package tachiyomi.core.common.util.system

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

/**
 * Pins the answers the original chooseBackground gave for every scenario, portrait then
 * landscape, recorded before the algorithm was decomposed.
 */
internal class BackgroundChooserGoldenTest {
    private val expected: Map<String, Pair<PageBackground, PageBackground>> = mapOf(
        "tiny" to (solid(WHITE) to solid(WHITE)),
        "allWhite" to (solid(WHITE) to solid(WHITE)),
        "allBlack" to (solid(BLACK) to solid(BLACK)),
        "allGray" to (solid(GRAY) to solid(GRAY)),
        "blackTopHalf" to (darkTop() to solid(BLACK)),
        "blackBottomHalf" to (darkBottom() to solid(WHITE)),
        "blackTopThird" to (darkTop() to solid(WHITE)),
        "blackBottomThird" to (darkBottom() to solid(WHITE)),
        "blackLeftStripe" to (solid(BLACK) to solid(BLACK)),
        "blackRightStripe" to (solid(BLACK) to solid(BLACK)),
        "blackLeftStripeShort" to (solid(WHITE) to solid(WHITE)),
        "blackRightStripeShort" to (solid(WHITE) to solid(WHITE)),
        "blackWithWhiteBottomCorners" to (darkTop() to solid(BLACK)),
        "blackWithWhiteTopCorners" to (darkBottom() to solid(BLACK)),
        "blackWithThreeWhiteCorners" to (darkBottom() to solid(BLACK)),
        "grayWithBlackCorners" to (solid(BLACK) to solid(BLACK)),
        "grayWithWhiteCorners" to (solid(WHITE) to solid(WHITE)),
        "stripedRows" to (solid(WHITE) to solid(WHITE)),
        "stripedRowsGray" to (solid(WHITE) to solid(WHITE)),
        "darkEdgesOnlyNotOffset" to (solid(WHITE) to solid(WHITE)),
        "darkTopBandThin" to (darkTop() to solid(WHITE)),
        "darkBottomBandThin" to (darkBottom() to solid(WHITE)),
        "grayTopBlackBottom" to (darkBottom() to solid(WHITE)),
        "blackTopGrayBottom" to (solid(BLACK) to solid(BLACK)),
        "checker" to (solid(WHITE) to solid(WHITE)),
        "blackTopWhiteMidBlackBottom" to (solid(BLACK) to solid(BLACK)),
    )

    private fun solid(color: Int) = PageBackground.Solid(color)
    private fun darkTop() = PageBackground.Gradient(listOf(BLACK, BLACK, WHITE, WHITE))
    private fun darkBottom() = PageBackground.Gradient(listOf(WHITE, WHITE, BLACK, BLACK))

    @Test
    fun everyScenarioIsPinned() {
        BackgroundScenarios.all.keys shouldBe expected.keys
    }

    @Test
    fun portraitAnswersMatch() {
        BackgroundScenarios.all.forEach { (name, grid) ->
            withClue(name) {
                BackgroundChooser.choose(grid, isLandscape = false) shouldBe expected.getValue(name).first
            }
        }
    }

    @Test
    fun landscapeAnswersMatch() {
        BackgroundScenarios.all.forEach { (name, grid) ->
            withClue(name) {
                BackgroundChooser.choose(grid, isLandscape = true) shouldBe expected.getValue(name).second
            }
        }
    }
}
