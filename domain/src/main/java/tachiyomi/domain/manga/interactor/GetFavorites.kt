package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Lists the library's favourites, all of them or per source. */
public class GetFavorites(
    private val mangaRepository: MangaRepository,
) {

    /** Every favourite. */
    public suspend fun await(): List<Manga> = mangaRepository.getFavorites()

    /** Favourites of source [sourceId], as a flow that re-emits on every change. */
    public fun subscribe(sourceId: Long): Flow<List<Manga>> = mangaRepository.getFavoritesBySourceId(sourceId)
}
