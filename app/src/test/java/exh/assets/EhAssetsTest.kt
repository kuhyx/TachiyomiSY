package exh.assets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.VectorPath
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class EhAssetsTest {
    @Test
    fun ehLogoIsNinePaths() {
        val icon = EhAssets.EhLogo
        icon.name shouldBe "EhLogo"
        icon.viewportWidth shouldBe 8.0f
        icon.viewportHeight shouldBe 7.0f
        icon.root.iterator().asSequence().count() shouldBe 9
        val first = icon.root.iterator().next().shouldBeInstanceOf<VectorPath>()
        first.fill.shouldBeInstanceOf<SolidColor>().value shouldBe Color(0xFF660611)
        // The vector is built once and cached.
        icon shouldBeSameInstanceAs EhAssets.EhLogo
    }

    @Test
    fun mangadexLogoKeepsEveryPath() {
        val icon = EhAssets.MangadexLogo
        icon.name shouldBe "MangadexLogo"
        icon.viewportWidth shouldBe 19.94664f
        icon.root.iterator().asSequence().count() shouldBe 13
        val fills = icon.root.iterator().asSequence()
            .map { (it as VectorPath).fill }
            .map { (it as SolidColor).value }
            .toList()
        fills.first() shouldBe Color(0xFFF79421)
        fills.last() shouldBe Color(0xFFF79421)
        fills.distinct().size shouldBe 6
        icon shouldBeSameInstanceAs EhAssets.MangadexLogo
    }
}
