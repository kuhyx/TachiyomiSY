package tachiyomi.domain.manga.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Looks up single manga by id or by source url. */
public class GetManga(
    private val mangaRepository: MangaRepository,
) : MetadataSource.GetMangaId {

    /** The manga with [id]; null when there is none or the store fails (logged). */
    public suspend fun await(id: Long): Manga? {
        return try {
            mangaRepository.getMangaById(id)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            null
        }
    }

    /** The manga with [id], as a flow that re-emits on every change. */
    public suspend fun subscribe(id: Long): Flow<Manga> = mangaRepository.getMangaByIdAsFlow(id)

    /** The manga at [url] in source [sourceId], or null, as a flow that re-emits on every change. */
    public fun subscribe(url: String, sourceId: Long): Flow<Manga?> =
        mangaRepository.getMangaByUrlAndSourceIdAsFlow(url, sourceId)

    // SY -->

    /** The manga at [url] in source [sourceId], or null. */
    public suspend fun await(url: String, sourceId: Long): Manga? =
        mangaRepository.getMangaByUrlAndSourceId(url, sourceId)

    override suspend fun awaitId(url: String, sourceId: Long): Long? = await(url, sourceId)?.id
    // SY <--
}
