package eu.kanade.presentation.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2000dp")
internal class SourceFeedScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private var loading by mutableStateOf(false)
    private var query by mutableStateOf<String?>(null)

    private val saved = SavedSearch(id = 5L, source = 1L, name = "Mine", query = "q", filtersJson = null)
    private val feed = FeedSavedSearch(id = 9L, source = 1L, savedSearch = 5L, global = false)
    private val manga = Manga.create().copy(id = 3L, ogTitle = "Found")

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(items: List<SourceFeedUI>, hasFilters: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                SourceFeedScreen(
                    name = "Feed source",
                    isLoading = loading,
                    items = items,
                    hasFilters = hasFilters,
                    onFabClick = { events += "fab" },
                    onClickBrowse = { events += "browse" },
                    onClickLatest = { events += "latest" },
                    onClickSavedSearch = { events += "saved ${it.id}" },
                    onClickDelete = { events += "delete ${it.id}" },
                    onClickManga = { events += "manga ${it.id}" },
                    onClickSearch = { events += "search $it" },
                    searchQuery = query,
                    onSearchQueryChange = { events += "query $it" },
                    getMangaState = { remember { mutableStateOf(it) } },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun feedRowsForward() {
        show(
            listOf(
                SourceFeedUI.Browse(null),
                SourceFeedUI.Latest(emptyList()),
                SourceFeedUI.SourceSavedSearch(feed, saved, listOf(manga)),
            ),
        )
        compose.onNodeWithText("Browse").performClick()
        compose.onNodeWithText("Latest").performClick()
        compose.onNodeWithText("Mine").performClick()
        compose.onNodeWithText("Mine").performTouchInput { longClick() }
        compose.onNodeWithText("Found").performClick()
        compose.onNodeWithText("Found").performTouchInput { longClick() }
        compose.onNodeWithText("No results found").assertExists()
        compose.onNodeWithText("Filter", useUnmergedTree = true).performClick()
        events shouldContainExactly listOf("browse", "latest", "saved 5", "delete 9", "manga 3", "manga 3", "fab")
    }

    @Test
    fun loadingThenSearch() {
        compose.mainClock.autoAdvance = false
        loading = true
        query = "abc"
        show(listOf(SourceFeedUI.Latest(null)), hasFilters = false)
        compose.mainClock.advanceTimeBy(1_000L)
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithContentDescription("Reset").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.mainClock.advanceTimeBy(1_000L)
        loading = false
        compose.mainClock.advanceTimeBy(1_000L)
        events shouldContainExactly listOf("search abc", "query ", "query null")
    }
}
