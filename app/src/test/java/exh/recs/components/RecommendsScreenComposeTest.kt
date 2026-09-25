package exh.recs.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.recs.RecommendationItemResult
import exh.recs.RecommendsScreenModel
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
                    getManga = { mutableStateOf(it) },
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
}
