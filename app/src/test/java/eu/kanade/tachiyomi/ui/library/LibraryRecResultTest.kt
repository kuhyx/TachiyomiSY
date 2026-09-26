package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.base.ScreenHost
import exh.recs.batch.SearchStatus
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class LibraryRecResultTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = LibraryHarness()
    private lateinit var model: LibraryScreenModel

    @Before
    fun setUp() {
        harness.start()
        model = harness.model()
        compose.setContent { ScreenHost(ComposableScreen { RecommendationResultEffect(model) }) }
        compose.waitForIdle()
    }

    @After
    fun tearDown() = harness.stop()

    private fun settle(status: SearchStatus) {
        model.recommendationSearch.status.value = status
        compose.waitUntil(WAIT) { model.recommendationSearch.status.value == SearchStatus.Idle }
    }

    @Test
    fun resultsOpenTheRecommendations() {
        settle(SearchStatus.Finished.WithResults(emptyList()))
        compose.waitForLabel("opened:RecommendsScreen")
    }

    @Test
    fun noResultsToast() {
        settle(SearchStatus.Finished.WithoutResults)
        ShadowToast.getTextOfLatestToast() shouldBe "No recommendations found"
    }

    @Test
    fun cancellingTearsDown() {
        model.recommendationSearch.status.value = SearchStatus.Initializing
        compose.waitForIdle()
        settle(SearchStatus.Cancelling)
        model.recommendationSearchJob shouldBe null
    }
}
