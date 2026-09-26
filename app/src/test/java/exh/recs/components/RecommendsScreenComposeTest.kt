package exh.recs.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.recs.RecommendationItemResult
import exh.recs.RecommendsScreenModel
import exh.recs.sources.RecommendationPagingSource
import exh.recs.sources.StaticResultPagingSource
import exh.recs.sources.rankedResults
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class RecommendsScreenComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SourceTestHarness()
    private val manga = Manga.create().copy(id = 3L, url = "/m", source = 7L, ogTitle = "Recommended")
    private val clickedSources = mutableListOf<String>()
    private val clickedItems = mutableListOf<Manga>()
    private val longClickedItems = mutableListOf<Manga>()

    @Before
    fun setUp() {
        harness.install()
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        harness.serve(networkToLocal)
    }

    @After
    fun tearDown() = harness.uninstall()

    private fun stateOf(result: RecommendationItemResult): RecommendsScreenModel.State {
        val source = StaticResultPagingSource(rankedResults(1))
        return RecommendsScreenModel.State(title = "Needle", items = mapOf(source to result))
    }

    private fun show(state: RecommendsScreenModel.State) {
        compose.setContent {
            MaterialTheme {
                RecommendsScreen(
                    title = "Recommendations",
                    state = state,
                    navigateUp = {},
                    onClickSource = { clickedSources += it.name },
                    onClickItem = { clickedItems += it },
                    onLongClickItem = { longClickedItems += it },
                    getManga = { manga -> remember(manga) { mutableStateOf(manga) } },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun loadingSourceShowsHeader() {
        show(stateOf(RecommendationItemResult.Loading))
        compose.onNodeWithText("Recommendations").assertIsDisplayed()
        compose.onNodeWithText("Static").assertIsDisplayed()
        compose.onNodeWithText("Similar titles").assertIsDisplayed()
    }

    @Test
    fun successShowsCardsAndClicks() {
        show(stateOf(RecommendationItemResult.Success(listOf(manga))))
        compose.onNodeWithText("Recommended").assertIsDisplayed()
        compose.onNodeWithText("Recommended").performClick()
        compose.waitForIdle()
        clickedItems.map { it.id } shouldContainExactly listOf(3L)
        compose.onNodeWithText("Static").performClick()
        compose.waitForIdle()
        clickedSources shouldContainExactly listOf("Static")
        longClickedItems.isEmpty() shouldBe true
    }

    @Test
    fun emptySuccessShowsNoResults() {
        show(stateOf(RecommendationItemResult.Success(emptyList())))
        compose.onNodeWithText("No results found").assertIsDisplayed()
    }

    @Test
    fun errorShowsMessage() {
        show(stateOf(RecommendationItemResult.Error(IllegalStateException("boom"))))
        compose.onNodeWithText("IllegalStateException: boom").assertIsDisplayed()
    }

    @Test
    fun contentWithoutItems() {
        show(RecommendsScreenModel.State())
        compose.onNodeWithText("Recommendations").assertIsDisplayed()
    }

    @Test
    fun forwardedContentRecomposes() {
        val source = StaticResultPagingSource(rankedResults(1))
        var handlers by mutableStateOf(Handlers({ clickedSources += it.name }, { clickedItems += it }))
        var items by mutableStateOf<Map<RecommendationPagingSource, RecommendationItemResult>>(
            mapOf(source to RecommendationItemResult.Loading),
        )
        var tick by mutableIntStateOf(0)
        compose.setContent { MaterialTheme { Forwarding(items, handlers, tick) } }
        compose.waitForIdle()
        // Same arguments, then every argument new: the content sees them as same, then different.
        tick++
        compose.waitForIdle()
        // New handlers with the same map, then a new map.
        handlers = Handlers({ clickedSources += "other" }, { longClickedItems += it })
        compose.waitForIdle()
        items = mapOf(source to RecommendationItemResult.Success(listOf(manga)))
        compose.waitForIdle()
        compose.onNodeWithText("Recommended").performClick()
        compose.waitForIdle()
        longClickedItems.map { it.id } shouldContainExactly listOf(3L)
    }

    @Test
    fun unstableItemsRecompose() {
        val source = StaticResultPagingSource(rankedResults(1))
        // A concrete LinkedHashMap is known unstable, so the host passes it on marked for an instance check.
        var items by mutableStateOf(linkedMapOf<RecommendationPagingSource, RecommendationItemResult>())
        var shown by mutableStateOf(false)
        var getManga by mutableStateOf<@Composable (Manga) -> State<Manga>>({ manga -> mutableStateOf(manga) })
        compose.setContent { MaterialTheme { UnstableHost(items, shown, getManga) } }
        compose.waitForIdle()
        // Shown later with the same map: its first composition is told "same, unstable".
        shown = true
        compose.waitForIdle()
        // A new getManga with the same map, then a new map.
        getManga = { manga -> remember(manga) { mutableStateOf(manga.copy(ogTitle = "Swapped")) } }
        compose.waitForIdle()
        items = linkedMapOf(source to RecommendationItemResult.Success(listOf(manga)))
        compose.waitForIdle()
        compose.onNodeWithText("Swapped").assertIsDisplayed()
    }
}

private data class Handlers(val onSource: (RecommendationPagingSource) -> Unit, val onItem: (Manga) -> Unit)

@Composable
private fun Forwarding(
    items: Map<RecommendationPagingSource, RecommendationItemResult>,
    handlers: Handlers,
    tick: Int,
) {
    tick.hashCode()
    ForwardingLambdas(items, handlers.onSource, handlers.onItem)
}

@Composable
private fun ForwardingLambdas(
    items: Map<RecommendationPagingSource, RecommendationItemResult>,
    onSource: (RecommendationPagingSource) -> Unit,
    onItem: (Manga) -> Unit,
) {
    RecommendsContent(
        items = items,
        contentPadding = PaddingValues(),
        onClickSource = onSource,
        onClickItem = onItem,
        onLongClickItem = onItem,
        getManga = { manga -> remember(manga) { mutableStateOf(manga) } },
    )
}

@Composable
private fun UnstableHost(
    items: LinkedHashMap<RecommendationPagingSource, RecommendationItemResult>,
    shown: Boolean,
    getManga: @Composable (Manga) -> State<Manga>,
) {
    if (shown) {
        RecommendsContent(
            items = items,
            contentPadding = PaddingValues(),
            onClickSource = {},
            onClickItem = {},
            onLongClickItem = {},
            getManga = getManga,
        )
    }
}
