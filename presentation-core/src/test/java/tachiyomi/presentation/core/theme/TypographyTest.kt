package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TypographyTest {
    @get:Rule
    val compose = createComposeRule()

    private var header: TextStyle? = null
    private var bodyMedium: TextStyle? = null
    private var variant: Color? = null

    @Test
    fun headerIsSemiBoldVariantBody() {
        compose.setContent {
            MaterialTheme {
                header = MaterialTheme.typography.header
                bodyMedium = MaterialTheme.typography.bodyMedium
                variant = MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        compose.runOnIdle {
            header?.fontWeight shouldBe FontWeight.SemiBold
            header?.color shouldBe variant
            header?.fontSize shouldBe bodyMedium?.fontSize
        }
    }
}
