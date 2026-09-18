package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Lists library entries still expecting new chapters. */
public class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
) {

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    /** Ongoing and publishing-finished favourites, as a flow that re-emits on every change. */
    public suspend fun subscribe(): Flow<List<Manga>> = mangaRepository.getUpcomingManga(includedStatuses)
}
