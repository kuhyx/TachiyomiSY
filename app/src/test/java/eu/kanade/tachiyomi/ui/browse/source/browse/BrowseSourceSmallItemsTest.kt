package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.EXHSavedSearch

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceSmallItemsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun savedSearchChipsClick() {
        val clicked = mutableListOf<String>()
        val pressed = mutableListOf<String>()
        val search = EXHSavedSearch(1L, "Mine", null, null)
        compose.setContent {
            MaterialTheme {
                SavedSearchItem(listOf(search), onSavedSearch = { clicked += it.name }, onSavedSearchPress = {
                    pressed += it.name
                })
            }
        }
        compose.onNodeWithText("Saved Searches").assertExists()
        compose.onNodeWithText("Mine").performClick()
        compose.onNodeWithText("Mine").performTouchInput { longClick() }
        compose.waitForIdle()
        clicked shouldBe listOf("Mine")
        pressed shouldBe listOf("Mine")
    }

    @Test
    fun noSavedSearchesShowNothing() {
        compose.setContent { MaterialTheme { SavedSearchItem(emptyList(), {}, {}) } }
        compose.onNodeWithText("Saved Searches").assertDoesNotExist()
    }

    @Test
    fun mangaDexHeaderButtons() {
        var random = 0
        var follows = 0
        compose.setContent {
            MaterialTheme { MangaDexFilterHeader(openMangaDexRandom = { random++ }, openMangaDexFollows = { follows++ }) }
        }
        compose.onNodeWithText("Random").performClick()
        compose.onNodeWithText("MangaDex follows").performClick()
        random shouldBe 1
        follows shouldBe 1
    }
}
