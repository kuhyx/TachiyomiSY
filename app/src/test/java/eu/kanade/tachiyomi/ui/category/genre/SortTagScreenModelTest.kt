package eu.kanade.tachiyomi.ui.category.genre

import eu.kanade.domain.manga.interactor.CreateSortTag
import eu.kanade.domain.manga.interactor.DeleteSortTag
import eu.kanade.domain.manga.interactor.GetSortTag
import eu.kanade.domain.manga.interactor.ReorderSortTag
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

internal class SortTagScreenModelTest {
    private val getSortTag = mockk<GetSortTag>()
    private val create = mockk<CreateSortTag>()
    private val delete = mockk<DeleteSortTag>(relaxed = true)
    private val reorder = mockk<ReorderSortTag>()

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        every { getSortTag.subscribe() } returns flowOf(listOf("a", "b"))
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun model() = SortTagScreenModel(getSortTag, create, delete, reorder)

    private fun SortTagScreenModel.success(): SortTagScreenState.Success =
        state.await { it is SortTagScreenState.Success }.shouldBeInstanceOf()

    @Test
    fun tagsAreLoaded() {
        val state = model().success()
        state.tags shouldBe listOf("a", "b")
        state.isEmpty shouldBe false
        state.copy(tags = emptyList()).isEmpty shouldBe true
    }

    @Test
    fun existingTagEmitsAnError() {
        every { create.await("a") } returns CreateSortTag.Result.TagExists
        every { create.await("c") } returns CreateSortTag.Result.Success
        val model = model()
        model.createTag("c")
        model.createTag("a")
        model.events.await { true } shouldBe SortTagEvent.TagExists
        verify { create.await("c") }
    }

    @Test
    fun movesReorderByOne() {
        every { reorder.await("a", 0) } returns ReorderSortTag.Result.Success
        every { reorder.await("a", 2) } returns ReorderSortTag.Result.Unchanged
        every { reorder.await("b", 0) } returns ReorderSortTag.Result.InternalError
        every { reorder.await("b", 2) } returns ReorderSortTag.Result.InternalError
        val model = model()
        model.moveUp("a", 1)
        model.moveDown("a", 1)
        model.moveUp("b", 1)
        model.events.await { true } shouldBe SortTagEvent.InternalError
        model.moveDown("b", 1)
        model.events.await { true }.shouldBeInstanceOf<SortTagEvent.LocalizedMessage>().stringRes shouldBe
            MR.strings.internal_error
    }

    @Test
    fun deleteRemovesTheTag() {
        model().delete("a")
        verify(timeout = 5_000) { delete.await("a") }
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = model()
        model.success()
        model.showDialog(SortTagDialog.Create)
        model.success().dialog shouldBe SortTagDialog.Create
        model.showDialog(SortTagDialog.Delete("a"))
        model.success().dialog shouldBe SortTagDialog.Delete("a")
        model.dismissDialog()
        model.success().dialog shouldBe null
    }

    @Test
    fun loadingIgnoresDialogs() {
        every { getSortTag.subscribe() } returns MutableSharedFlow()
        val model = model()
        model.showDialog(SortTagDialog.Create)
        model.dismissDialog()
        model.state.value shouldBe SortTagScreenState.Loading
    }

    @Test
    fun defaultsComeFromInjekt() {
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
        SortTagScreenModel().success().tags shouldBe listOf("a", "b")
    }

    @Test
    fun eventMembers() {
        SortTagEvent.TagExists.stringRes shouldBe SYMR.strings.error_tag_exists
        SortTagDialog.Delete("a").copy(tag = "b").tag shouldBe "b"
        SortTagDialog.Delete("a").hashCode() shouldBe SortTagDialog.Delete("a").hashCode()
    }
}
