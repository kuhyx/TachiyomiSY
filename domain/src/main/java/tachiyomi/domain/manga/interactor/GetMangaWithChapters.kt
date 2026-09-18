package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Reads a manga together with its chapters. */
public class GetMangaWithChapters(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    /** The manga [id] paired with its chapters, as a flow that re-emits on every change of either. */
    public suspend fun subscribe(id: Long, applyScanlatorFilter: Boolean = false): Flow<Pair<Manga, List<Chapter>>> {
        return combine(
            mangaRepository.getMangaByIdAsFlow(id),
            chapterRepository.getChapterByMangaIdAsFlow(id, applyScanlatorFilter),
        ) { manga, chapters ->
            Pair(manga, chapters)
        }
    }

    /** The manga with [id]; throws when there is none. */
    public suspend fun awaitManga(id: Long): Manga = mangaRepository.getMangaById(id)

    /** Chapters of manga [id]; [applyScanlatorFilter] drops the manga's excluded scanlators. */
    public suspend fun awaitChapters(id: Long, applyScanlatorFilter: Boolean = false): List<Chapter> =
        chapterRepository.getChapterByMangaId(id, applyScanlatorFilter)
}
