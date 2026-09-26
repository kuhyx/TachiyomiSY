package exh.pagepreview.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.source.Source
import exh.pagepreview.PagePreviewState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class PagePreviewScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<Int>()
    private val selected = mutableListOf<Int>()
    private var dialogOpened = 0
    private var dialogDismissed = 0
    private var navigatedUp = 0

    private fun success(pageCount: Int?, page: Int = 1, previews: Int = 2) = PagePreviewState.Success(
        page = page,
        pagePreviews = List(previews) { PagePreview(index = it + 1, imageUrl = "u$it", source = 1L) },
        hasNextPage = false,
        pageCount = pageCount,
        manga = Manga.create(),
        chapter = Chapter.create(),
        source = mockk<Source>(),
    )

    private fun show(state: PagePreviewState, pageDialogOpen: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                PagePreviewScreen(
                    state = state,
                    pageDialogOpen = pageDialogOpen,
                    onPageSelected = { selected += it },
                    onOpenPage = { opened += it },
                    onOpenPageDialog = { dialogOpened++ },
                    onDismissPageDialog = { dialogDismissed++ },
                    navigateUp = { navigatedUp++ },
                )
            }
        }
    }

    @Test
    fun errorsShowTheirMessage() {
        show(PagePreviewState.Error(IllegalStateException("no previews")))
        compose.onNodeWithText("Page previews").assertIsDisplayed()
        compose.onNodeWithText("no previews").assertIsDisplayed()
        compose.onAllNodesWithText("Go to").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun errorWithoutMessageShowsBlank() {
        show(PagePreviewState.Error(IllegalStateException()))
        compose.onNodeWithText("Page previews").assertIsDisplayed()
    }

    @Test
    fun loadingShowsNoPages() {
        show(PagePreviewState.Loading)
        compose.onNodeWithText("Page previews").assertIsDisplayed()
        compose.onAllNodesWithText("Go to").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun singlePageHidesTheAction() {
        show(success(pageCount = 1))
        compose.onAllNodesWithText("Go to").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun unknownPageCountHidesIt() {
        show(success(pageCount = null))
        compose.onAllNodesWithText("Go to").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun previewsAreClickable() {
        show(success(pageCount = 3))
        compose.onNodeWithContentDescription("Go to").performClick()
        dialogOpened shouldBe 1
        compose.onNodeWithContentDescription("Navigate up").performClick()
        navigatedUp shouldBe 1
    }

    @Test
    fun theDialogPicksAPage() {
        show(success(pageCount = 5, page = 2), pageDialogOpen = true)
        compose.onNodeWithText("Go to").assertIsDisplayed()
        compose.onAllNodesWithText("2").fetchSemanticsNodes().size shouldBe 2
        compose.onNodeWithText("5").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        selected shouldContainExactly listOf(2)
        dialogDismissed shouldBe 1
        compose.onNodeWithText("Cancel").performClick()
        dialogDismissed shouldBe 2
    }

    /** Dragging the slider snaps the page to the nearest whole number. */
    @Test
    fun theSliderSnapsToAPage() {
        show(success(pageCount = 5, page = 2), pageDialogOpen = true)
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(4.4F) }
        compose.waitForIdle()
        compose.onNodeWithText("4").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        selected shouldContainExactly listOf(4)
    }

    @Test
    fun theDialogNeedsASuccessState() {
        show(PagePreviewState.Loading, pageDialogOpen = true)
        compose.onAllNodesWithText("OK").fetchSemanticsNodes().size shouldBe 0
    }
}
