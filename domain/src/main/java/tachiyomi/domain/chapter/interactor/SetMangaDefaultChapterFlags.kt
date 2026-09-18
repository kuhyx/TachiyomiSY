package tachiyomi.domain.chapter.interactor

import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.SetMangaChapterFlags
import tachiyomi.domain.manga.model.Manga

/** Applies the library-wide default chapter filters, sort and display mode to manga. */
public class SetMangaDefaultChapterFlags(
    private val libraryPreferences: LibraryPreferences,
    private val setMangaChapterFlags: SetMangaChapterFlags,
    private val getFavorites: GetFavorites,
) {

    /**
     * Overwrites [manga]'s chapter flags with the defaults from [LibraryPreferences]; not cancellable
     * once started.
     */
    public suspend fun await(manga: Manga) {
        withNonCancellableContext {
            with(libraryPreferences) {
                setMangaChapterFlags.awaitSetAllFlags(
                    mangaId = manga.id,
                    unreadFilter = filterChapterByRead.get(),
                    downloadedFilter = filterChapterByDownloaded.get(),
                    bookmarkedFilter = filterChapterByBookmarked.get(),
                    sortingMode = sortChapterBySourceOrNumber.get(),
                    sortingDirection = sortChapterByAscendingOrDescending.get(),
                    displayMode = displayChapterByNameOrNumber.get(),
                )
            }
        }
    }

    /** [await] for every favourite. */
    public suspend fun awaitAll() {
        withNonCancellableContext {
            getFavorites.await().forEach { await(it) }
        }
    }
}
