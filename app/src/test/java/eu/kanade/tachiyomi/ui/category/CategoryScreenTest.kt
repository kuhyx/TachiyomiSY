package eu.kanade.tachiyomi.ui.category

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.tachiyomi.ui.base.ScreenHost
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class CategoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val category = Category(id = 1, name = "Reading", order = 1, flags = 0)
    private val getCategories = mockk<GetCategories> {
        every { subscribe() } returns MutableStateFlow(listOf(category))
    }
    private val create = mockk<CreateCategoryWithName>()
    private val delete = mockk<DeleteCategory>()
    private val rename = mockk<RenameCategory>()

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { getCategories }
                    single { create }
                    single { delete }
                    single { mockk<ReorderCategory>() }
                    single { rename }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun show(loaded: Boolean = true) {
        compose.setContent { ScreenHost(CategoryScreen()) }
        compose.waitForIdle()
        if (loaded) {
            compose.waitUntil(WAIT) { compose.onAllNodesWithText("Edit categories").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun loadingHidesTheList() {
        every { getCategories.subscribe() } returns MutableSharedFlow()
        show(loaded = false)
        compose.onAllNodesWithText("Edit categories").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun createFailureIsToasted() {
        coEvery { create.await("New") } returns CreateCategoryWithName.Result.InternalError(IllegalStateException())
        show()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("New")
        compose.onAllNodesWithText("Add").onLast().performClick()
        compose.waitForIdle()
        coVerify { create.await("New") }
        compose.waitUntil(WAIT) { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "InternalError: Check crash logs for further information"
    }

    @Test
    fun renameAndDeleteUseTheCategory() {
        coEvery { rename.await(category, "Done") } returns RenameCategory.Result.Success
        coEvery { delete.await(categoryId = 1) } returns DeleteCategory.Result.Success
        show()
        compose.onNodeWithContentDescription("Rename category").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Done")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        coVerify { rename.await(category, "Done") }
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        coVerify { delete.await(categoryId = 1) }
    }

    @Test
    fun cancellingClosesTheDialog() {
        show()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Cancel").fetchSemanticsNodes().size shouldBe 0
        compose.onNodeWithContentDescription("Navigate up").performClick()
    }
}

private const val WAIT = 5_000L
