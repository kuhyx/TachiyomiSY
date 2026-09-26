package eu.kanade.tachiyomi.ui.category

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR

internal class CategoryScreenModelTest {
    private val getCategories = mockk<GetCategories>()
    private val create = mockk<CreateCategoryWithName>()
    private val delete = mockk<DeleteCategory>()
    private val reorder = mockk<ReorderCategory>()
    private val rename = mockk<RenameCategory>()
    private val system = Category(id = 0, name = "", order = 0, flags = 0)
    private val user = Category(id = 1, name = "A", order = 1, flags = 0)
    private val error = IllegalStateException("db")

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        every { getCategories.subscribe() } returns flowOf(listOf(system, user))
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun model() = CategoryScreenModel(getCategories, create, delete, reorder, rename)

    private fun CategoryScreenModel.success() = state.value.shouldBeInstanceOf<CategoryScreenState.Success>()

    private fun CategoryScreenModel.nextEvent(): CategoryEvent = events.await { true }

    @Test
    fun systemCategoriesAreHidden() {
        val state = model().success()
        state.categories shouldBe listOf(user)
        state.isEmpty shouldBe false
        state.copy(categories = emptyList()).isEmpty shouldBe true
    }

    @Test
    fun failuresEmitAnError() {
        coEvery { create.await("B") } returns CreateCategoryWithName.Result.InternalError(error)
        coEvery { delete.await(categoryId = 1) } returns DeleteCategory.Result.InternalError(error)
        coEvery { reorder.await(user, 2) } returns ReorderCategory.Result.InternalError(error)
        coEvery { rename.await(user, "C") } returns RenameCategory.Result.InternalError(error)
        val model = model()
        model.createCategory("B")
        model.nextEvent() shouldBe CategoryEvent.InternalError
        model.deleteCategory(1)
        model.nextEvent() shouldBe CategoryEvent.InternalError
        model.changeOrder(user, 2)
        model.nextEvent() shouldBe CategoryEvent.InternalError
        model.renameCategory(user, "C")
        model.nextEvent().shouldBeInstanceOf<CategoryEvent.LocalizedMessage>().stringRes shouldBe
            MR.strings.internal_error
    }

    @Test
    fun successesEmitNothing() {
        coEvery { create.await("B") } returns CreateCategoryWithName.Result.Success(user)
        coEvery { delete.await(categoryId = 1) } returns DeleteCategory.Result.Success
        coEvery { reorder.await(user, 2) } returns ReorderCategory.Result.Unchanged
        coEvery { rename.await(user, "C") } returns RenameCategory.Result.Success
        coEvery { rename.await(user, "D") } returns RenameCategory.Result.InternalError(error)
        val model = model()
        model.createCategory("B")
        model.deleteCategory(1)
        model.changeOrder(user, 2)
        model.renameCategory(user, "C")
        model.renameCategory(user, "D")
        // Only the last call failed, so the first event is its error.
        model.nextEvent() shouldBe CategoryEvent.InternalError
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = model()
        model.showDialog(CategoryDialog.Create)
        model.success().dialog shouldBe CategoryDialog.Create
        model.showDialog(CategoryDialog.Rename(user))
        model.success().dialog shouldBe CategoryDialog.Rename(user)
        model.showDialog(CategoryDialog.Delete(user))
        model.success().dialog shouldBe CategoryDialog.Delete(user)
        model.dismissDialog()
        model.success().dialog shouldBe null
    }

    @Test
    fun loadingIgnoresDialogs() {
        // A dispatcher nobody advances keeps the model's first load from running.
        val main = StandardTestDispatcher()
        Dispatchers.setMain(main)
        val model = model()
        model.showDialog(CategoryDialog.Create)
        model.dismissDialog()
        model.state.value shouldBe CategoryScreenState.Loading
        main.scheduler.advanceUntilIdle()
        model.success().dialog shouldBe null
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { getCategories }
                    single { create }
                    single { delete }
                    single { reorder }
                    single { rename }
                },
            )
        }
        CategoryScreenModel().success().categories shouldBe listOf(user)
    }

    @Test
    fun dialogMembers() {
        CategoryDialog.Rename(user).copy().category shouldBe user
        CategoryDialog.Delete(user).hashCode() shouldBe CategoryDialog.Delete(user).hashCode()
        CategoryDialog.Create.toString() shouldBe "Create"
    }
}
