package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.ui.model.ThemeMode
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WidgetPreviewsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textPreviewRenders() {
        compose.setContent { TextPreferenceWidgetPreview() }
        compose.onAllNodesWithText("Text preference summary").fetchSemanticsNodes().size shouldBe 3
    }

    @Test
    fun switchPreviewRenders() {
        compose.setContent { SwitchPreferenceWidgetPreview() }
        compose.onNodeWithText("Text preference no summary").assertExists()
    }

    @Test
    fun infoPreviewRenders() {
        compose.setContent { InfoWidgetPreview() }
        compose.onNodeWithText("Only works", substring = true).assertExists()
    }

    @Test
    fun switchClickFlipsValue() {
        var seen: Boolean? = null
        compose.setContent {
            MaterialTheme { SwitchPreferenceWidget(title = "Flip", onCheckedChanged = { seen = it }) }
        }
        compose.onNodeWithText("Flip").performClick()
        seen shouldBe true
    }

    @Test
    fun themeModeClickReports() {
        var seen: ThemeMode? = null
        compose.setContent {
            MaterialTheme { AppThemeModePreferenceWidget(value = ThemeMode.DARK, onItemClick = { seen = it }) }
        }
        compose.onNodeWithText("Light").performClick()
        seen shouldBe ThemeMode.LIGHT
    }

    @Test
    fun groupHeaderRenders() {
        compose.setContent { MaterialTheme { PreferenceGroupHeader(title = "Header") } }
        compose.onNodeWithText("Header").assertExists()
    }
}
