package eu.kanade.presentation.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source

@RunWith(RobolectricTestRunner::class)
internal class SourcesDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun source(id: Long, pins: Pins = Pins.unpinned, excluded: Boolean = false) = Source(
        id = id,
        lang = "en",
        name = "Name",
        supportsLatest = false,
        isStub = false,
        pin = pins,
        isExcludedFromDataSaver = excluded,
        categories = setOf("a"),
    )

    private fun showOptions(source: Source, extras: Boolean) {
        compose.setContent {
            MaterialTheme {
                SourceOptionsDialog(
                    source = source,
                    onClickPin = { events += "pin" },
                    onClickDisable = { events += "disable" },
                    onClickSetCategories = { events += "categories" }.takeIf { extras },
                    onClickToggleDataSaver = { events += "saver" }.takeIf { extras },
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun optionsForARemoteSource() {
        showOptions(source(1L), extras = true)
        compose.onNodeWithText("Name (EN)").assertExists()
        compose.onNodeWithText("Pin").performClick()
        compose.onNodeWithText("Disable").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithText("Exclude from data saver").performClick()
        events shouldContainExactly listOf("pin", "disable", "categories", "saver")
    }

    @Test
    fun optionsForAPinnedLocalSource() {
        showOptions(source(0L, pins = Pins.pinned, excluded = true), extras = false)
        compose.onNodeWithText("Unpin").assertExists()
        compose.onNodeWithText("Disable").assertDoesNotExist()
        compose.onNodeWithText("Categories").assertDoesNotExist()
    }

    @Test
    fun excludedSourcesCanStop() {
        showOptions(source(1L, excluded = true), extras = true)
        compose.onNodeWithText("Stop excluding from data saver").assertExists()
    }

    @Test
    fun categoriesDialogTogglesChecks() {
        compose.setContent {
            MaterialTheme {
                SourceCategoriesDialog(
                    source = source(1L),
                    categories = listOf("a", "b"),
                    onClickCategories = { events += it.joinToString() },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithText("a").performClick()
        compose.onNodeWithText("b").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("b")
    }
}
