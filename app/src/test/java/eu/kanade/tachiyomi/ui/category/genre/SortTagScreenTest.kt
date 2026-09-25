package eu.kanade.tachiyomi.ui.category.genre

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import eu.kanade.domain.manga.interactor.CreateSortTag
import eu.kanade.domain.manga.interactor.DeleteSortTag
import eu.kanade.domain.manga.interactor.GetSortTag
import eu.kanade.domain.manga.interactor.ReorderSortTag
import eu.kanade.tachiyomi.ui.base.ScreenHost
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SortTagScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val getSortTag = mockk<GetSortTag> { every { subscribe() } returns MutableStateFlow(listOf("a", "b")) }
    private val create = mockk<CreateSortTag>()
    private val delete = mockk<DeleteSortTag>(relaxed = true)
    private val reorder = mockk<ReorderSortTag> {
        every { await(any(), any()) } returns ReorderSortTag.Result.Unchanged
    }

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { getSortTag }
                    single { create }
                    single { delete }
                    single { reorder }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun show(loaded: Boolean = true) {
        compose.setContent { ScreenHost(SortTagScreen()) }
        compose.waitForIdle()
        if (loaded) {
            compose.waitUntil(WAIT) { compose.onAllNodesWithText("Edit tags").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    // Each row's icons have empty descriptions: label, move up, move down, delete; four per tag.
    private fun icon(index: Int) = compose.onAllNodesWithContentDescription("")[index]

    @Test
    fun loadingHidesTheList() {
        every { getSortTag.subscribe() } returns MutableSharedFlow()
        show(loaded = false)
        compose.onAllNodesWithText("Edit tags").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun existingTagIsToasted() {
        every { create.await("new") } returns CreateSortTag.Result.TagExists
        show()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("new")
        compose.onAllNodesWithText("Add").onLast().performClick()
        compose.waitUntil(WAIT) { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "This tag exists!"
    }

    @Test
    fun rowActionsReachTheModel() {
        show()
        icon(2).performClick()
        icon(5).performClick()
        verify(timeout = WAIT) { reorder.await("a", 1) }
        verify(timeout = WAIT) { reorder.await("b", 0) }
        icon(3).performClick()
        compose.onNodeWithText("OK").performClick()
        verify(timeout = WAIT) { delete.await("a") }
        compose.onNodeWithContentDescription("Navigate up").performClick()
    }
}

private const val WAIT = 5_000L
