package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenState
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
internal class FeedScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private var state by mutableStateOf(FeedScreenState())

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun feed(id: Long) = FeedSavedSearch(id = id, source = 1L, savedSearch = null, global = true)

    private fun item(id: Long, saved: Boolean, source: Boolean, results: List<Manga>?) = FeedItemUI(
        feed = feed(id),
        savedSearch = SavedSearch(id = 1L, source = 1L, name = "S", query = null, filtersJson = null).takeIf { saved },
        source = browseSource(id).takeIf { source },
        title = "Feed $id",
        subtitle = "Sub $id",
        results = results,
    )

    private fun show() {
        compose.setContent {
            MaterialTheme {
                FeedScreen(
                    state = state,
                    contentPadding = PaddingValues(),
                    onClickSavedSearch = { saved, source -> events += "saved ${saved.id} ${source.id}" },
                    onClickSource = { events += "source ${it.id}" },
                    onClickDelete = { events += "delete ${it.id}" },
                    onClickManga = { events += "manga ${it.id}" },
                    onRefresh = { events += "refresh" },
                    getMangaState = { remember { mutableStateOf(it) } },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun loadingThenEmpty() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(500L)
        state = FeedScreenState(items = emptyList())
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithText("You don't have any sources in your feed", substring = true).assertExists()
    }

    @Test
    fun rowsForward() {
        val manga = Manga.create().copy(id = 7L, ogTitle = "Found")
        state = FeedScreenState(
            items = listOf(
                item(1L, saved = true, source = true, results = listOf(manga)),
                item(2L, saved = false, source = true, results = emptyList()),
                item(3L, saved = true, source = false, results = emptyList()),
            ),
        )
        show()
        compose.onNodeWithText("Feed 1").performClick()
        compose.onNodeWithText("Feed 2").performClick()
        compose.onNodeWithText("Feed 3").performClick()
        compose.onNodeWithText("Feed 3").performTouchInput { longClick() }
        compose.onNodeWithText("Found").performClick()
        events shouldContainExactly listOf("saved 1 1", "source 2", "delete 3", "manga 7")
    }

    @Test
    fun pullToRefresh() {
        state = FeedScreenState(items = listOf(item(1L, saved = false, source = true, results = emptyList())))
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(100L)
        compose.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        compose.mainClock.advanceTimeBy(100L)
        state = FeedScreenState(items = listOf(item(1L, saved = false, source = true, results = null)))
        compose.mainClock.advanceTimeBy(100L)
        compose.mainClock.advanceTimeBy(2_000L)
        events shouldContainExactly listOf("refresh")
    }
}
