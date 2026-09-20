package tachiyomi.presentation.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class CustomIconsTest {
    private fun assertBlackPathOn24dpCanvas(icon: ImageVector, name: String, nodes: Int) {
        icon.name shouldBe name
        icon.defaultWidth shouldBe 24.dp
        icon.defaultHeight shouldBe 24.dp
        icon.viewportWidth shouldBe 24f
        icon.viewportHeight shouldBe 24f
        icon.root.size shouldBe 1
        val path = icon.root[0] as VectorPath
        path.fill shouldBe SolidColor(Color.Black)
        path.pathData.size shouldBe nodes
    }

    @Test
    fun discord() {
        assertBlackPathOn24dpCanvas(icon = CustomIcons.Discord, name = "Discord", nodes = 44)
    }

    @Test
    fun facebook() {
        assertBlackPathOn24dpCanvas(icon = CustomIcons.Facebook, name = "Facebook", nodes = 26)
    }

    @Test
    fun github() {
        assertBlackPathOn24dpCanvas(icon = CustomIcons.Github, name = "Github", nodes = 26)
    }

    @Test
    fun reddit() {
        assertBlackPathOn24dpCanvas(icon = CustomIcons.Reddit, name = "Reddit", nodes = 50)
    }

    @Test
    fun xLogo() {
        assertBlackPathOn24dpCanvas(icon = CustomIcons.X, name = "X", nodes = 18)
    }
}
