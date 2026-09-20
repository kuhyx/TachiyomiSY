package tachiyomi.presentation.widget.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class GlanceUtilsTest {

    @Test
    fun countsRowsAndColumnsFromSize() {
        DpSize(200.dp, 300.dp).calculateRowAndColumnCount(0.dp, 0.dp) shouldBe (3 to 3)
        DpSize(130.dp, 300.dp).calculateRowAndColumnCount(0.dp, 0.dp) shouldBe (3 to 2)
    }

    @Test
    fun subtractsVerticalPadding() {
        DpSize(200.dp, 300.dp).calculateRowAndColumnCount(8.dp, 4.dp) shouldBe (3 to 3)
        DpSize(200.dp, 300.dp).calculateRowAndColumnCount(0.dp, 24.dp) shouldBe (2 to 3)
    }

    @Test
    fun takesAtLeastOneRowAndColumn() {
        DpSize(10.dp, 10.dp).calculateRowAndColumnCount(0.dp, 0.dp) shouldBe (1 to 1)
        DpSize(0.dp, 0.dp).calculateRowAndColumnCount(50.dp, 50.dp) shouldBe (1 to 1)
    }

    @Test
    fun capsAtGlanceChildLimit() {
        DpSize(2000.dp, 2000.dp).calculateRowAndColumnCount(0.dp, 0.dp) shouldBe (10 to 10)
    }

    @Test
    fun backgroundRadiusRoundsCorners() {
        GlanceModifier.appWidgetBackgroundRadius().any { it.toString().contains("CornerRadius") } shouldBe true
    }

    @Test
    fun innerRadiusAddsCornerRadius() {
        GlanceModifier.appWidgetInnerRadius().any { it.toString().contains("CornerRadius") } shouldBe true
    }
}
