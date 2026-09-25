package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.manga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceFavoritesTest {
    private val harness = BrowseSourceHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun category(categoryId: Long, system: Boolean = false): Category = mockk {
        every { id } returns categoryId
        every { isSystemCategory } returns system
    }

    @Test
    fun favouritingBindsTrackers() {
        val model = harness.model()
        model.changeMangaFavorite(manga())
        coVerify(timeout = 5_000) { harness.setDefaultFlags.await(manga()) }
        coVerify(timeout = 5_000) { harness.addTracks.bindEnhancedTrackers(manga(), harness.source) }
        coVerify(timeout = 5_000) { harness.updateManga.await(match { it.favorite == true }) }
    }

    @Test
    fun unfavouritingDropsCovers() {
        harness.model().changeMangaFavorite(manga(favorite = true))
        coVerify(timeout = 5_000) { harness.updateManga.await(match { it.favorite == false && it.dateAdded == 0L }) }
        coVerify { harness.coverCache.deleteFromCache(any(), true) }
    }

    @Test
    fun noCategoriesAddsDirectly() {
        harness.model().addFavorite(manga())
        coVerify(timeout = 5_000) { harness.setMangaCategories.await(mangaId = 1L, categoryIds = emptyList()) }
        coVerify(timeout = 5_000) { harness.updateManga.await(any()) }
    }

    @Test
    fun defaultCategoryIsUsed() {
        harness.categories.value = listOf(category(0L, system = true), category(4L))
        harness.koin.libraryPreferences.defaultCategory.set(4)
        harness.model().addFavorite(manga())
        coVerify(timeout = 5_000) { harness.setMangaCategories.await(mangaId = 1L, categoryIds = listOf(4L)) }
    }

    @Test
    fun unsetDefaultAsksForCategories() {
        val user = category(4L)
        harness.categories.value = listOf(user)
        harness.koin.libraryPreferences.defaultCategory.set(9)
        coEvery { harness.getCategories.await(1L) } returns listOf(user)
        val model = harness.model()
        model.addFavorite(manga())
        eventually { model.state.value.dialog is BrowseSourceScreenModel.Dialog.ChangeMangaCategory }
    }

    @Test
    fun categoriesSkipTheSystemOne() {
        harness.categories.value = listOf(category(0L, system = true), category(4L))
        val model = harness.model()
        runBlocking { model.getCategories() }.map { it.id } shouldBe listOf(4L)
        every { harness.getCategories.subscribe() } returns emptyFlow()
        runBlocking { model.getCategories() } shouldBe emptyList()
        model.moveMangaToCategories(manga(), category(0L), category(5L))
        coVerify(timeout = 5_000) { harness.setMangaCategories.await(mangaId = 1L, categoryIds = listOf(5L)) }
    }
}
