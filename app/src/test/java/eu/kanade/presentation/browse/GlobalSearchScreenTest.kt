package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class GlobalSearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val manga = Manga.create().copy(id = 5L, ogTitle = "Found")
    private val items: Map<Source, SearchItemResult> = mapOf(
        browseSource(1L) to SearchItemResult.Loading,
        browseSource(2L) to SearchItemResult.Success(listOf(manga)),
        browseSource(3L) to SearchItemResult.Error(IllegalStateException("down")),
    )

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(state: SearchScreenModel.State) {
        compose.setContent {
            MaterialTheme {
                GlobalSearchScreen(
                    state = state,
                    navigateUp = { events += "up" },
                    onChangeSearchQuery = {},
                    onSearch = {},
                    onChangeSearchFilter = { events += "filter $it" },
                    onToggleResults = { events += "toggle" },
                    onClickSource = { events += "source ${it.id}" },
                    onClickItem = { events += "item ${it.id}" },
                    onLongClickItem = { events += "long ${it.id}" },
                    getManga = { remember(it) { mutableStateOf(it) } },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun resultsShowEveryState() {
        show(SearchScreenModel.State(searchQuery = "q", items = items))
        compose.onNodeWithText("down").assertExists()
        compose.onNodeWithText("Found").performClick()
        compose.onNodeWithText("Source 2").performClick()
        compose.onNodeWithText("Pinned").performClick()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("Has results").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly
            listOf("item 5", "source 2", "filter PinnedOnly", "filter All", "toggle", "up")
    }

    @Test
    fun finishedSearchHidesProgress() {
        show(
            SearchScreenModel.State(
                searchQuery = "q",
                sourceFilter = SourceFilter.All,
                items = mapOf(browseSource(2L) to SearchItemResult.Success(emptyList())),
            ),
        )
        compose.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun contentMarksTheOriginSource() {
        compose.setContent {
            MaterialTheme {
                GlobalSearchContent(
                    items = mapOf(
                        browseSource(1L) to SearchItemResult.Loading,
                        browseSource(2L) to SearchItemResult.Loading,
                    ),
                    contentPadding = PaddingValues(),
                    onClickSource = {},
                    onClickItem = {},
                    onLongClickItem = {},
                    fromSourceId = 1L,
                    getManga = { remember(it) { mutableStateOf(it) } },
                )
            }
        }
        compose.onNodeWithText("▶ Source 1").assertExists()
        compose.onNodeWithText("Source 2").assertExists()
    }
}
