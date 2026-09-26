package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.Dialog
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class LibraryModelDialogsTest {
    private val harness = LibraryHarness()
    private val system = Category(id = 0, name = "", order = 0, flags = 0)
    private val shared = category(1)
    private val some = category(2)
    private val other = category(3)
    private val unused = category(4)

    private fun category(id: Long) = Category(id = id, name = "C$id", order = id, flags = 0)

    @Before
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        coEvery { harness.getCategories.await(1L) } returns listOf(shared, some)
        coEvery { harness.getCategories.await(2L) } returns listOf(shared, other)
    }

    @After
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun selectedModel(): LibraryScreenModel {
        harness.categories.value = listOf(system, shared, some, other, unused)
        harness.library.value = listOf(
            libraryManga(1, manga(1), categories = listOf(1L, 2L)),
            libraryManga(2, manga(2), categories = listOf(1L, 3L)),
        )
        val model = harness.model()
        model.state.await { it.groupedFavorites[shared]?.toSet() == setOf(1L, 2L) }
        model.selectAll()
        return model
    }

    @Test
    fun commonAndMixCategories() {
        val model = harness.model()
        runBlocking {
            model.getCommonCategories(emptyList()).shouldBeEmpty()
            model.getMixCategories(emptyList()).shouldBeEmpty()
            model.getCommonCategories(listOf(manga(1), manga(2))) shouldBe setOf(shared)
            model.getMixCategories(listOf(manga(1), manga(2))) shouldBe setOf(some, other)
        }
    }

    @Test
    fun changeCategoryPreselects() {
        val model = selectedModel()
        model.openChangeCategoryDialog()
        val dialog = model.state.await { it.dialog != null }.dialog as Dialog.ChangeCategory
        dialog.manga.map { it.id }.toSet() shouldBe setOf(1L, 2L)
        dialog.initialSelection shouldBe listOf(
            CheckboxState.State.Checked(shared),
            CheckboxState.TriState.Exclude(some),
            CheckboxState.TriState.Exclude(other),
            CheckboxState.State.None(unused),
        )
    }

    @Test
    fun simpleDialogs() {
        val model = selectedModel()
        model.showSettingsDialog()
        model.state.value.dialog shouldBe Dialog.SettingsSheet
        model.showRecommendationSearchDialog()
        (model.state.value.dialog as Dialog.RecommendationSearchSheet).manga.size shouldBe 2
        model.openDeleteMangaDialog()
        (model.state.value.dialog as Dialog.DeleteManga).manga.size shouldBe 2
        model.closeDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun favoritesSyncShowsTheIntroOnce() {
        val model = harness.model()
        harness.exhPreferences.exhShowSyncIntro.set(true)
        model.openFavoritesSyncDialog()
        model.state.value.dialog shouldBe Dialog.SyncFavoritesWarning
        model.onAcceptSyncWarning()
        model.openFavoritesSyncDialog()
        model.state.value.dialog shouldBe Dialog.SyncFavoritesConfirm
    }
}
