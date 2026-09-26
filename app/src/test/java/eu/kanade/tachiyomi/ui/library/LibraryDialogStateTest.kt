package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.CheckboxState

@RunWith(RobolectricTestRunner::class)
internal class LibraryDialogStateTest {
    private val harness = LibraryHarness()
    private val categories = listOf(libCategory(0L), libCategory(1L), libCategory(2L), libCategory(3L))

    @Before
    fun setUp() {
        harness.start()
        harness.categories.value = categories
        coEvery { harness.getCategories.await(1L) } returns listOf(categories[1], categories[2])
        coEvery { harness.getCategories.await(2L) } returns listOf(categories[1])
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun commonAndMixCategories() {
        val model = harness.model()
        val mangas = listOf(libManga(1L), libManga(2L))
        runBlocking { model.getCommonCategories(emptyList()) }.shouldBeEmpty()
        runBlocking { model.getMixCategories(emptyList()) }.shouldBeEmpty()
        runBlocking { model.getCommonCategories(mangas) } shouldContainExactly listOf(categories[1])
        runBlocking { model.getMixCategories(mangas) } shouldContainExactly listOf(categories[2])
    }

    @Test
    fun changeCategoryPreselects() {
        val model = harness.selecting(libEntry(libManga(1L)), libEntry(libManga(2L)))
        model.openChangeCategoryDialog()
        val dialog = model.await { it.dialog != null }.dialog as LibraryScreenModel.Dialog.ChangeCategory
        dialog.manga.map { it.id } shouldContainExactlyInAnyOrder listOf(1L, 2L)
        dialog.initialSelection shouldContainExactly listOf(
            CheckboxState.State.Checked(categories[1]),
            CheckboxState.TriState.Exclude(categories[2]),
            CheckboxState.State.None(categories[3]),
        )
    }

    @Test
    fun simpleDialogsOpenAndClose() {
        val model = harness.selecting(libEntry(libManga(1L)))
        model.showSettingsDialog()
        model.state.value.dialog shouldBe LibraryScreenModel.Dialog.SettingsSheet
        model.showRecommendationSearchDialog()
        model.state.value.dialog shouldBe LibraryScreenModel.Dialog.RecommendationSearchSheet(listOf(libManga(1L)))
        model.openDeleteMangaDialog()
        model.state.value.dialog shouldBe LibraryScreenModel.Dialog.DeleteManga(listOf(libManga(1L)))
        model.closeDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun favoritesSyncWarnsOnce() {
        val model = harness.model()
        model.openFavoritesSyncDialog()
        model.state.value.dialog shouldBe LibraryScreenModel.Dialog.SyncFavoritesWarning
        harness.exhPreferences.exhShowSyncIntro.set(false)
        model.openFavoritesSyncDialog()
        model.state.value.dialog shouldBe LibraryScreenModel.Dialog.SyncFavoritesConfirm
    }
}
