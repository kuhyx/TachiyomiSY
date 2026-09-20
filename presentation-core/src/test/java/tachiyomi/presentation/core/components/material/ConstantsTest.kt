package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ConstantsTest {
    @Test
    fun alphas() {
        DISABLED_ALPHA shouldBe 0.38f
        SECONDARY_ALPHA shouldBe 0.78f
    }

    @Test
    fun paddingScale() {
        val padding = Padding()
        padding.extraLarge shouldBe 32.dp
        padding.large shouldBe 24.dp
        padding.medium shouldBe 16.dp
        padding.small shouldBe 8.dp
        padding.extraSmall shouldBe 4.dp
    }

    @Test
    fun themePaddingIsTheScale() {
        MaterialTheme.padding.medium shouldBe 16.dp
        MaterialTheme.padding.extraSmall shouldBe 4.dp
    }

    @Test
    fun topSmallPaddingIsTopOnly() {
        topSmallPaddingValues.calculateTopPadding() shouldBe 8.dp
        topSmallPaddingValues.calculateBottomPadding() shouldBe 0.dp
        topSmallPaddingValues.calculateStartPadding(LayoutDirection.Ltr) shouldBe 0.dp
        topSmallPaddingValues.calculateEndPadding(LayoutDirection.Ltr) shouldBe 0.dp
    }

    @Test
    fun iconButtonStateLayerSize() {
        IconButtonTokens.StateLayerSize shouldBe 40.dp
    }
}
