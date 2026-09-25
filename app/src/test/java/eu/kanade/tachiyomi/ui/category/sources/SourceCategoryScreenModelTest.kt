package eu.kanade.tachiyomi.ui.category.sources

import eu.kanade.domain.source.interactor.CreateSourceCategory
import eu.kanade.domain.source.interactor.DeleteSourceCategory
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.RenameSourceCategory
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal class SourceCategoryScreenModelTest {
    private val get = mockk<GetSourceCategories>()
    private val create = mockk<CreateSourceCategory>()
    private val rename = mockk<RenameSourceCategory>()
    private val delete = mockk<DeleteSourceCategory>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        every { get.subscribe() } returns flowOf(listOf("x"))
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun model() = SourceCategoryScreenModel(get, create, rename, delete)

    private fun SourceCategoryScreenModel.success(): SourceCategoryScreenState.Success =
        state.await { it is SourceCategoryScreenState.Success }.shouldBeInstanceOf()

    @Test
    fun categoriesAreLoaded() {
        val state = model().success()
        state.categories shouldBe listOf("x")
        state.isEmpty shouldBe false
        state.copy(categories = emptyList()).isEmpty shouldBe true
    }

    @Test
    fun invalidNamesEmitAnError() {
        every { create.await("ok") } returns CreateSourceCategory.Result.Success
        every { create.await("") } returns CreateSourceCategory.Result.InvalidName
        every { rename.await("x", "y") } returns CreateSourceCategory.Result.Success
        every { rename.await("x", "") } returns CreateSourceCategory.Result.InvalidName
        val model = model()
        model.createCategory("ok")
        model.renameCategory("x", "y")
        model.createCategory("")
        model.events.await { true } shouldBe SourceCategoryEvent.InvalidName
        model.renameCategory("x", "")
        model.events.await { true }.shouldBeInstanceOf<SourceCategoryEvent.LocalizedMessage>().stringRes shouldBe
            SYMR.strings.invalid_category_name
    }

    @Test
    fun deleteRemovesTheCategory() {
        model().deleteCategory("x")
        verify(timeout = 5_000) { delete.await("x") }
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = model()
        model.success()
        model.showDialog(SourceCategoryDialog.Create)
        model.success().dialog shouldBe SourceCategoryDialog.Create
        model.showDialog(SourceCategoryDialog.Rename("x"))
        model.success().dialog shouldBe SourceCategoryDialog.Rename("x")
        model.showDialog(SourceCategoryDialog.Delete("x"))
        model.success().dialog shouldBe SourceCategoryDialog.Delete("x")
        model.dismissDialog()
        model.success().dialog shouldBe null
    }

    @Test
    fun loadingIgnoresDialogs() {
        every { get.subscribe() } returns MutableSharedFlow()
        val model = model()
        model.showDialog(SourceCategoryDialog.Create)
        model.dismissDialog()
        model.state.value shouldBe SourceCategoryScreenState.Loading
    }

    @Test
    fun defaultsComeFromInjekt() {
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
        SourceCategoryScreenModel().success().categories shouldBe listOf("x")
    }

    @Test
    fun eventMembers() {
        SourceCategoryEvent.InternalError.stringRes shouldBe MR.strings.internal_error
        SourceCategoryDialog.Rename("a").copy(category = "b").category shouldBe "b"
        SourceCategoryDialog.Delete("a").hashCode() shouldBe SourceCategoryDialog.Delete("a").hashCode()
    }
}
