package tachiyomi.domain.history.interactor

import io.mockk.coEvery
import io.mockk.mockk
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga

internal const val NEXT_MANGA_ID: Long = 40L

/** Read first: highest source order comes first with the default (source, ascending) sort. */
internal val readChapter: Chapter =
    Chapter.create().copy(id = 1L, mangaId = NEXT_MANGA_ID, sourceOrder = 2L, read = true)

/** Read second. */
internal val unreadChapter: Chapter = Chapter.create().copy(id = 2L, mangaId = NEXT_MANGA_ID, sourceOrder = 1L)

/** Read last. */
internal val lastChapter: Chapter = Chapter.create().copy(id = 3L, mangaId = NEXT_MANGA_ID, sourceOrder = 0L)

/** The three chapters in an order the sort has to fix. */
internal val unsortedChapters: List<Chapter> = listOf(unreadChapter, lastChapter, readChapter)

/** The collaborators of one [GetNextChapters], each a mock stubbed by the builders below. */
internal class NextChaptersFixture {
    val getChaptersByMangaId: GetChaptersByMangaId = mockk()
    val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = mockk()
    val getManga: GetManga = mockk()
    val historyRepository: HistoryRepository = mockk()

    val interactor = GetNextChapters(
        getChaptersByMangaId = getChaptersByMangaId,
        getMergedChaptersByMangaId = getMergedChaptersByMangaId,
        getManga = getManga,
        historyRepository = historyRepository,
    )

    /** Stubs a manga of [source] with [chapters] from the plain and the merged chapter readers. */
    fun withManga(source: Long, chapters: List<Chapter>) {
        coEvery { getManga.await(NEXT_MANGA_ID) } returns Manga.create().copy(id = NEXT_MANGA_ID, source = source)
        coEvery { getChaptersByMangaId.await(NEXT_MANGA_ID, applyScanlatorFilter = true) } returns chapters
        coEvery {
            getMergedChaptersByMangaId.await(NEXT_MANGA_ID, dedupe = true, applyScanlatorFilter = true)
        } returns chapters
    }

    /** Stubs a manga that does not exist. */
    fun withoutManga() {
        coEvery { getManga.await(NEXT_MANGA_ID) } returns null
    }
}
