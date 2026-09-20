package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class ColorTest {
    @get:Rule
    val compose = createComposeRule()

    private var active: Color? = null

    @Test
    @Config(qualifiers = "notnight")
    fun activeIsAmber500InLight() {
        compose.setContent { MaterialTheme { active = MaterialTheme.colorScheme.active } }
        compose.runOnIdle { active shouldBe Color(0xFFFFC107) }
    }

    @Test
    @Config(qualifiers = "night")
    fun activeIsAmber200InDark() {
        compose.setContent { MaterialTheme { active = MaterialTheme.colorScheme.active } }
        compose.runOnIdle { active shouldBe Color(0xFFFFEB3B) }
    }
}
