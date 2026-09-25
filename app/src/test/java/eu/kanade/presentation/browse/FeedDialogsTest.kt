package eu.kanade.presentation.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.browse.components.SourceFeedAddDialog
import eu.kanade.presentation.browse.components.SourceFeedDeleteDialog
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch

@RunWith(RobolectricTestRunner::class)
internal class FeedDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val feed = FeedSavedSearch(id = 1L, source = 2L, savedSearch = null, global = true)
    private val search = SavedSearch(id = 3L, source = 2L, name = "Saved", query = null, filtersJson = null)

    @Test
    fun addFeedPicksASource() {
        val sources = listOf(browseSource(1L), browseSource(2L))
        compose.setContent {
            MaterialTheme {
                FeedAddDialog(sources = sources, onDismiss = {}, onClickAdd = { events += "add ${it?.id}" })
            }
        }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText(sources[1].toString()).performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("add null", "add 2")
    }

    @Test
    fun addSearchPicksASavedSearch() {
        compose.setContent {
            MaterialTheme {
                FeedAddSearchDialog(
                    source = browseSource(2L),
                    savedSearches = listOf(null, search),
                    onDismiss = {},
                    onClickAdd = { source, saved -> events += "add ${source.id} ${saved?.name}" },
                )
            }
        }
        compose.onNodeWithText("Source 2").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Latest").performClick()
        compose.onNodeWithText("Saved").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("add 2 null", "add 2 Saved")
    }

    @Test
    fun deleteConfirmation() {
        compose.setContent {
            MaterialTheme {
                FeedDeleteConfirmDialog(
                    feed = feed,
                    onDismiss = {},
                    onClickDeleteConfirm = { events += "delete ${it.id}" },
                )
            }
        }
        compose.onNodeWithText("Delete feed item?").assertExists()
        compose.onNodeWithText("Delete").performClick()
        events shouldContainExactly listOf("delete 1")
    }

    @Test
    fun sourceFeedDialogs() {
        compose.setContent {
            MaterialTheme {
                SourceFeedAddDialog(
                    onDismissRequest = { events += "dismiss" },
                    name = "Mine",
                    addFeed = { events += "add" },
                )
                SourceFeedDeleteDialog(onDismissRequest = { events += "dismiss" }, deleteFeed = { events += "delete" })
            }
        }
        compose.onNodeWithText("Add Mine to feed?").assertExists()
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Delete").performClick()
        events shouldContainExactly listOf("add", "delete")
    }

    @Test
    fun feedModelsAreData() {
        val latest = SourceFeedUI.Latest(null)
        val browse = SourceFeedUI.Browse(null)
        val saved = SourceFeedUI.SourceSavedSearch(feed, search, null)
        latest.withResults(emptyList()).results shouldBe emptyList()
        browse.withResults(emptyList()).results shouldBe emptyList()
        saved.withResults(emptyList()).results shouldBe emptyList()
        listOf(latest.id, browse.id, saved.id) shouldContainExactly listOf(-1L, -2L, 1L)
        compose.setContent { Text("${latest.title}|${browse.title}|${saved.title}") }
        compose.onNodeWithText("Latest|Browse|Saved").assertExists()
        val item = FeedItemUI(
            feed = feed,
            savedSearch = search,
            source = null,
            title = "t",
            subtitle = "s",
            results = null,
        )
        item.copy(title = "u").title shouldBe "u"
    }
}
