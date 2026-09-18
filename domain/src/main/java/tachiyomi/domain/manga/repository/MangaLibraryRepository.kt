package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

/** Library-wide reads of [MangaRepository]: favourites and the library view. */
public interface MangaLibraryRepository {

    /** Every favourite. */
    public suspend fun getFavorites(): List<Manga>

    /** Non-favourites with read chapters. */
    public suspend fun getReadMangaNotInLibrary(): List<Manga>

    /** The library view: favourites with chapter counts and categories. */
    public suspend fun getLibraryManga(): List<LibraryManga>

    /** [getLibraryManga] as a flow. */
    public fun getLibraryMangaAsFlow(): Flow<List<LibraryManga>>

    // SY -->

    /** [getReadMangaNotInLibrary] with chapter counts and categories. */
    public suspend fun getReadMangaNotInLibraryView(): List<LibraryManga>
    // SY <--
}
