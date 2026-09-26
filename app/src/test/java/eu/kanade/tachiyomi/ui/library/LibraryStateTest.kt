package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.LibraryData
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.State
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo

internal class LibraryStateTest {
    private val savedNHentai = nHentaiSourceIds
    private val savedMangaDex = mangaDexSourceIds
    private val system = Category(id = 0, name = "", order = 0, flags = 0)
    private val named = Category(id = 1, name = "Named", order = 1, flags = 0)

    @BeforeEach
    fun setUp() {
        nHentaiSourceIds = listOf(NHENTAI)
        mangaDexSourceIds = listOf(MANGADEX)
        startKoin { modules(module { single { GetCustomMangaInfo(EditedInfo) } }) }
    }

    @AfterEach
    fun tearDown() {
        nHentaiSourceIds = savedNHentai
        mangaDexSourceIds = savedMangaDex
        stopKoin()
    }

    private fun state(vararg items: LibraryItem, selection: Set<Long> = items.map { it.id }.toSet()) = State(
        selection = selection,
        libraryData = LibraryData(favorites = items.toList()),
        groupedFavorites = mapOf(system to items.map { it.id }, named to listOf(99L)),
    )

    @Test
    fun selectedMangaSkipsUnknownIds() {
        val item = libraryItem(manga(1))
        state(item, selection = setOf(1L, 2L)).selectedManga shouldBe listOf(item.libraryManga.manga)
    }

    @Test
    fun cleanTitlesNeedsAGallerySource() {
        state(libraryItem(manga(1))).showCleanTitles.shouldBeFalse()
        state(libraryItem(manga(1).copy(source = EH_SOURCE_ID))).showCleanTitles.shouldBeTrue()
        state(libraryItem(manga(1).copy(source = NHENTAI))).showCleanTitles.shouldBeTrue()
    }

    @Test
    fun mangadexNeedsAMangadexSource() {
        state(libraryItem(manga(1))).showAddToMangadex.shouldBeFalse()
        state(libraryItem(manga(1).copy(source = MANGADEX))).showAddToMangadex.shouldBeTrue()
    }

    @Test
    fun resetInfoNeedsAnEditedField() {
        state(libraryItem(manga(1).copy(favorite = true))).showResetInfo.shouldBeFalse()
        // Each of ids 11..17 has one field edited in [EditedInfo].
        (11L..17L).forEach { id -> state(libraryItem(manga(id).copy(favorite = true))).showResetInfo.shouldBeTrue() }
    }

    @Test
    fun itemsForACategory() {
        val item = libraryItem(manga(1))
        val state = state(item)
        state.getItemsForCategoryId(null) shouldBe emptyList()
        state.getItemsForCategoryId(7L) shouldBe emptyList()
        state.getItemsForCategoryId(0L) shouldBe listOf(item)
        state.getItemsForCategory(named) shouldBe emptyList()
        state.getItemsForCategory(Category(id = 5, name = "", order = 0, flags = 0)) shouldBe emptyList()
    }

    @Test
    fun countsShowOnRequestOrSearch() {
        val state = state(libraryItem(manga(1)))
        state.getItemCountForCategory(system).shouldBeNull()
        state.copy(searchQuery = "").getItemCountForCategory(system).shouldBeNull()
        state.copy(searchQuery = "q").getItemCountForCategory(system) shouldBe 1
        state.copy(showMangaCount = true).getItemCountForCategory(named) shouldBe 1
        state.copy(showMangaCount = true).getItemCountForCategory(Category(5, "", 0, 0)).shouldBeNull()
    }

    @Test
    fun toolbarTitles() {
        val state = state(libraryItem(manga(1)), libraryItem(manga(2)))
        state.getToolbarTitle("Library", "Default", page = 5) shouldBe LibraryToolbarTitle("Library")
        state.getToolbarTitle("Library", "Default", page = 0) shouldBe LibraryToolbarTitle("Default")
        state.getToolbarTitle("Library", "Default", page = 1) shouldBe LibraryToolbarTitle("Named")
        val counted = state.copy(showMangaCount = true)
        counted.getToolbarTitle("Library", "Default", page = 1) shouldBe LibraryToolbarTitle("Named", 1)
        counted.copy(showCategoryTabs = true).getToolbarTitle("Library", "Default", page = 1) shouldBe
            LibraryToolbarTitle("Library", 2)
    }

    private companion object {
        const val NHENTAI = 6_907L
        const val MANGADEX = 2_499L
    }
}
