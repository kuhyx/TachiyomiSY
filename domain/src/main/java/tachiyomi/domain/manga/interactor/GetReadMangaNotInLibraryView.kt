package tachiyomi.domain.manga.interactor

import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.repository.MangaRepository

/** Lists non-favourites the user has read chapters of, in library-view form. */
public class GetReadMangaNotInLibraryView(
    private val mangaRepository: MangaRepository,
) {

    /** Non-favourites with read chapters, with chapter counts and categories. */
    public suspend fun await(): List<LibraryManga> = mangaRepository.getReadMangaNotInLibraryView()
}
