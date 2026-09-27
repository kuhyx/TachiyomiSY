package eu.kanade.presentation.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h3000dp")
internal class MangaScreenLargeTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val host = MangaScreenHost(compose)

    @Before
    fun setUp() = host.start()

    @After
    fun tearDown() = host.stop()

    private fun pages() = (1..6).map { PagePreview(index = it, imageUrl = "https://h/$it", source = 7L) }

    private fun frames() {
        repeat(times = 5) {
            compose.mainClock.advanceTimeBy(100L)
            compose.waitForIdle()
        }
    }

    @Test
    fun tabletLayoutForwards() {
        host.show(screenState(chapters = listOf(chapterItem(mangaChapter(1L)))), isTablet = true)
        compose.onNodeWithText("Add to library").performClick()
        compose.onNodeWithText("Chapter 1").performClick()
        compose.onNodeWithText("Chapter 1").performTouchInput { longClick() }
        compose.onNodeWithText("1 chapter").performClick()
        compose.onNodeWithText("Start", useUnmergedTree = true).performClick()
        compose.onNodeWithText("See Recommendations").performClick()
        compose.onNodeWithContentDescription("Cover").performClick()
        val titles = compose.onAllNodesWithText("Needle")
        titles.fetchSemanticsNodes().indices.forEach { titles[it].performClick() }
        host.events shouldContainExactly listOf(
            "favorite",
            "open 1",
            "select 1 true true",
            "filter",
            "continue",
            "recommend",
            "cover",
            "search Needle true",
        )
    }

    @Test
    fun tabletPreviewsLoadThenShow() {
        compose.mainClock.autoAdvance = false
        host.show(screenState(previews = PagePreviewState.Loading), isTablet = true)
        frames()
        host.state = screenState(previews = PagePreviewState.Success(pages()))
        frames()
        compose.onNodeWithText("More previews").performClick()
        compose.onNodeWithText("1").performClick()
        frames()
        host.events shouldContainExactly listOf("more previews", "preview 0")
    }

    @Test
    fun phonePreviewsLoadThenShow() {
        compose.mainClock.autoAdvance = false
        host.show(screenState(previews = PagePreviewState.Loading))
        frames()
        host.state = screenState(previews = PagePreviewState.Success(pages()))
        frames()
        compose.onNodeWithText("More previews").performClick()
        compose.onNodeWithText("2").performClick()
        frames()
        host.events shouldContainExactly listOf("more previews", "preview 1")
    }

    @Test
    fun previewsHiddenOrFailed() {
        host.actions = host.recorder.build(rows = 0)
        host.show(screenState(previews = PagePreviewState.Success(pages())), isTablet = true)
        compose.onNodeWithText("More previews").assertDoesNotExist()
        host.state = screenState(previews = PagePreviewState.Error(IllegalStateException("x")))
        host.actions = host.recorder.build()
        compose.waitForIdle()
        host.tablet = false
        compose.waitForIdle()
        compose.onNodeWithText("More previews").assertDoesNotExist()
    }

    @Test
    fun refreshingShowsIndicator() {
        compose.mainClock.autoAdvance = false
        host.show(screenState(refreshing = true), isTablet = true)
        frames()
        host.tablet = false
        frames()
        compose.onNodeWithText("Add to library").assertExists()
    }
}
