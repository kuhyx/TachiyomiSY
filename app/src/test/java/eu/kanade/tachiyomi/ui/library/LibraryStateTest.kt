package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.library.components.LibraryToolbarTitle
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

internal class LibraryStateTest {
    private val category = libCategory(1L)
    private val items = listOf(libItem(1L), libItem(2L))

    private fun selected(vararg manga: Manga): LibraryScreenModel.State =
        stateOf(category, manga.map { libItem(libEntry(it)) }, selection = manga.map { it.id }.toSet())

    @Test
    fun itemsByCategory() {
        val state = stateOf(category, items).copy(groupedFavorites = mapOf(category to listOf(1L, 2L, 9L)))
        state.getItemsForCategoryId(null).shouldBeEmpty()
        state.getItemsForCategoryId(5L).shouldBeEmpty()
        state.getItemsForCategoryId(1L) shouldContainExactly items
        state.getItemsForCategory(libCategory(5L)).shouldBeEmpty()
    }

    @Test
    fun itemCountNeedsCountOrSearch() {
        val state = stateOf(category, items)
        state.getItemCountForCategory(category).shouldBeNull()
        state.copy(searchQuery = "").getItemCountForCategory(category).shouldBeNull()
        state.copy(searchQuery = "q").getItemCountForCategory(category) shouldBe 2
        state.copy(showMangaCount = true).getItemCountForCategory(category) shouldBe 2
        state.copy(showMangaCount = true).getItemCountForCategory(libCategory(5L)).shouldBeNull()
    }

    @Test
    fun toolbarTitleFollowsTheCategory() {
        val state = stateOf(category, items)
        state.getToolbarTitle("Lib", "Default", page = 3) shouldBe LibraryToolbarTitle("Lib")
        state.getToolbarTitle("Lib", "Default", page = 0) shouldBe LibraryToolbarTitle("Cat 1", null)
        state.copy(showMangaCount = true).getToolbarTitle("Lib", "Default", page = 0) shouldBe
            LibraryToolbarTitle("Cat 1", 2)
        state.copy(showMangaCount = true, showCategoryTabs = true).getToolbarTitle("Lib", "Default", page = 0) shouldBe
            LibraryToolbarTitle("Lib", 2)
        val system = stateOf(libCategory(0L), items)
        system.getToolbarTitle("Lib", "Default", page = 0) shouldBe LibraryToolbarTitle("Default", null)
    }

    @Test
    fun activeCategoryIsCoerced() {
        LibraryScreenModel.State(activeCategoryIndex = 4).activeCategory.shouldBeNull()
        stateOf(category, items).copy(activeCategoryIndex = 4).activeCategory shouldBe category
        LibraryScreenModel.State().isLibraryEmpty shouldBe true
    }

    @Test
    fun selectionFlagsReadTheSources() {
        val plain = selected(libManga(1L))
        plain.selectionMode shouldBe true
        plain.selectedManga.map { it.id } shouldContainExactly listOf(1L)
        plain.showCleanTitles shouldBe false
        plain.showAddToMangadex shouldBe false
        plain.showResetInfo shouldBe false
        // Read twice: each flag is a lazy.
        plain.showCleanTitles shouldBe false
        selected(libManga(1L, source = EH_SOURCE_ID)).showCleanTitles shouldBe true
        stateOf(category, items, selection = setOf(7L)).selectedManga.shouldBeEmpty()
    }

    @Test
    fun forkSourceListsCount() {
        val nhentai = nHentaiSourceIds
        val mangadex = mangaDexSourceIds
        try {
            nHentaiSourceIds = listOf(55L)
            mangaDexSourceIds = listOf(66L)
            selected(libManga(1L, source = 55L)).showCleanTitles shouldBe true
            selected(libManga(1L, source = 66L)).showAddToMangadex shouldBe true
        } finally {
            nHentaiSourceIds = nhentai
            mangaDexSourceIds = mangadex
        }
    }

    @Test
    fun anyEditShowsReset() {
        val edits: List<(Manga) -> Unit> = listOf(
            { every { it.title } returns "x" },
            { every { it.author } returns "x" },
            { every { it.artist } returns "x" },
            { every { it.thumbnailUrl } returns "x" },
            { every { it.description } returns "x" },
            { every { it.genre } returns listOf("x") },
            { every { it.status } returns 9L },
        )
        edits.forEach { edit ->
            val edited = spyk(libManga(2L)).also(edit)
            selected(libManga(1L), edited).showResetInfo shouldBe true
        }
    }

    @Test
    fun modelTypesAreValues() {
        val manga = listOf(libManga(1L))
        LibraryScreenModel.Dialog.ChangeCategory(manga, emptyList()).copy().manga shouldBe manga
        LibraryScreenModel.Dialog.DeleteManga(manga).copy() shouldBe LibraryScreenModel.Dialog.DeleteManga(manga)
        LibraryScreenModel.Dialog.RecommendationSearchSheet(manga).hashCode() shouldBe
            LibraryScreenModel.Dialog.RecommendationSearchSheet(manga).hashCode()
        itemPrefs().copy(filterLewd = itemPrefs().filterUnread) shouldBe itemPrefs()
        val data = LibraryScreenModel.LibraryData(favorites = items)
        data.favoritesById.keys shouldContainExactly setOf(1L, 2L)
        data.copy(showSystemCategory = true).toString().contains("showSystemCategory=true") shouldBe true
        LibraryScreenModel.State().copy(isLoading = false).toString().contains("isLoading=false") shouldBe true
    }
}
