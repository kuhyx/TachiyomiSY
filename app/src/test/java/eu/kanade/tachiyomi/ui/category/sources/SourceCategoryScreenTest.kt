package eu.kanade.tachiyomi.ui.category.sources

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.domain.source.interactor.CreateSourceCategory
import eu.kanade.domain.source.interactor.DeleteSourceCategory
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.RenameSourceCategory
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
internal class SourceCategoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val get = mockk<GetSourceCategories> { every { subscribe() } returns MutableStateFlow(listOf("cat")) }
    private val create = mockk<CreateSourceCategory>()
    private val rename = mockk<RenameSourceCategory>()
    private val delete = mockk<DeleteSourceCategory>(relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { get }
                    single { create }
                    single { rename }
                    single { delete }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun show(loaded: Boolean = true) {
        compose.setContent { ScreenHost(SourceCategoryScreen()) }
        compose.waitForIdle()
        if (loaded) {
            compose.waitUntil(WAIT) { compose.onAllNodesWithText("Edit categories").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    // The list item's icons have empty descriptions: label, rename, delete.
    private fun icon(index: Int) = compose.onAllNodesWithContentDescription("")[index]

    @Test
    fun loadingHidesTheList() {
        every { get.subscribe() } returns MutableSharedFlow()
        show(loaded = false)
        compose.onAllNodesWithText("Edit categories").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun invalidNameIsToasted() {
        every { create.await("new") } returns CreateSourceCategory.Result.InvalidName
        show()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("new")
        compose.onAllNodesWithText("Add").onLast().performClick()
        compose.waitUntil(WAIT) { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "Invalid category name"
    }

    @Test
    fun rowActionsReachTheModel() {
        every { rename.await("cat", "dog") } returns CreateSourceCategory.Result.Success
        show()
        icon(1).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("dog")
        compose.onNodeWithText("OK").performClick()
        verify(timeout = WAIT) { rename.await("cat", "dog") }
        icon(2).performClick()
        compose.onNodeWithText("OK").performClick()
        verify(timeout = WAIT) { delete.await("cat") }
        compose.onNodeWithContentDescription("Navigate up").performClick()
    }
}

private const val WAIT = 5_000L
