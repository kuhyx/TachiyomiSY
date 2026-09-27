package eu.kanade.presentation.browse.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import eu.kanade.presentation.browse.UiDispatcherReset
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceDialogsTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    @Test
    fun removeMangaConfirms() {
        compose.setContent {
            MaterialTheme {
                RemoveMangaDialog(
                    onDismissRequest = { events += "dismiss" },
                    onConfirm = { events += "confirm" },
                    mangaToRemove = Manga.create().copy(ogTitle = "Needle"),
                )
            }
        }
        compose.onNodeWithText("You are about to remove \"Needle\" from your library").assertExists()
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss", "confirm", "dismiss")
    }

    @Test
    fun deleteSavedSearch() {
        compose.setContent {
            MaterialTheme {
                SavedSearchDeleteDialog(
                    onDismissRequest = { events += "dismiss" },
                    name = "Mine",
                    deleteSavedSearch = { events += "delete" },
                )
            }
        }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("delete", "dismiss", "dismiss")
    }

    @Test
    fun createSavedSearchValidates() {
        compose.setContent {
            MaterialTheme {
                SavedSearchCreateDialog(
                    onDismissRequest = { events += "dismiss" },
                    currentSavedSearches = listOf("taken"),
                    saveSearch = { events += "save $it" },
                )
            }
        }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("My search name").performTextInput("taken")
        compose.onNodeWithText("OK").performClick()
        ShadowToast.getTextOfLatestToast() shouldBe "Invalid saved search name"
        compose.onNodeWithText("taken").performTextClearance()
        compose.onNodeWithText("My search name").performTextInput(" fresh ")
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("save fresh", "dismiss", "dismiss")
    }
}
