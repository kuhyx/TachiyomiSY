package tachiyomi.domain.chapter.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.SetMangaChapterFlags
import tachiyomi.domain.manga.model.Manga

internal class SetMangaDefaultChapterFlagsTest {

    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())
    private val setMangaChapterFlags = mockk<SetMangaChapterFlags>()
    private val getFavorites = mockk<GetFavorites>()
    private val setDefaults = SetMangaDefaultChapterFlags(libraryPreferences, setMangaChapterFlags, getFavorites)

    @Test
    fun awaitAppliesThePreferences() = runTest {
        libraryPreferences.filterChapterByRead.set(Manga.CHAPTER_SHOW_UNREAD)
        libraryPreferences.filterChapterByDownloaded.set(Manga.CHAPTER_SHOW_DOWNLOADED)
        libraryPreferences.filterChapterByBookmarked.set(Manga.CHAPTER_SHOW_BOOKMARKED)
        libraryPreferences.sortChapterBySourceOrNumber.set(Manga.CHAPTER_SORTING_NUMBER)
        libraryPreferences.sortChapterByAscendingOrDescending.set(Manga.CHAPTER_SORT_ASC)
        libraryPreferences.displayChapterByNameOrNumber.set(Manga.CHAPTER_DISPLAY_NUMBER)
        coEvery {
            setMangaChapterFlags.awaitSetAllFlags(
                mangaId = 7L,
                unreadFilter = Manga.CHAPTER_SHOW_UNREAD,
                downloadedFilter = Manga.CHAPTER_SHOW_DOWNLOADED,
                bookmarkedFilter = Manga.CHAPTER_SHOW_BOOKMARKED,
                sortingMode = Manga.CHAPTER_SORTING_NUMBER,
                sortingDirection = Manga.CHAPTER_SORT_ASC,
                displayMode = Manga.CHAPTER_DISPLAY_NUMBER,
            )
        } returns true

        setDefaults.await(Manga.create().copy(id = 7L))

        coVerify(exactly = 1) {
            setMangaChapterFlags.awaitSetAllFlags(
                mangaId = 7L,
                unreadFilter = Manga.CHAPTER_SHOW_UNREAD,
                downloadedFilter = Manga.CHAPTER_SHOW_DOWNLOADED,
                bookmarkedFilter = Manga.CHAPTER_SHOW_BOOKMARKED,
                sortingMode = Manga.CHAPTER_SORTING_NUMBER,
                sortingDirection = Manga.CHAPTER_SORT_ASC,
                displayMode = Manga.CHAPTER_DISPLAY_NUMBER,
            )
        }
    }

    @Test
    fun awaitAllCoversEveryFavorite() = runTest {
        coEvery { getFavorites.await() } returns listOf(Manga.create().copy(id = 1L), Manga.create().copy(id = 2L))
        coEvery {
            setMangaChapterFlags.awaitSetAllFlags(
                mangaId = any(),
                unreadFilter = any(),
                downloadedFilter = any(),
                bookmarkedFilter = any(),
                sortingMode = any(),
                sortingDirection = any(),
                displayMode = any(),
            )
        } returns true

        setDefaults.awaitAll()

        listOf(1L, 2L).forEach { id ->
            coVerify(exactly = 1) {
                setMangaChapterFlags.awaitSetAllFlags(
                    mangaId = id,
                    unreadFilter = Manga.SHOW_ALL,
                    downloadedFilter = Manga.SHOW_ALL,
                    bookmarkedFilter = Manga.SHOW_ALL,
                    sortingMode = Manga.CHAPTER_SORTING_SOURCE,
                    sortingDirection = Manga.CHAPTER_SORT_DESC,
                    displayMode = Manga.CHAPTER_DISPLAY_NAME,
                )
            }
        }
    }
}
