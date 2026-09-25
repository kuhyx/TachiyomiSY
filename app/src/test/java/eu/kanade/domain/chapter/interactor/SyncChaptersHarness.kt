package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.model.SChapter
import io.mockk.coEvery
import io.mockk.mockk
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga

/** The sync interactor with every collaborator mocked; the mocks are reachable as properties. */
internal class SyncChaptersHarness {
    val downloadManager = mockk<DownloadManager>()
    val downloadProvider = mockk<DownloadProvider>()
    val chapterRepository = mockk<ChapterRepository>()
    val shouldUpdateDbChapter = mockk<ShouldUpdateDbChapter>()
    val updateManga = mockk<UpdateManga>()
    val updateChapter = mockk<UpdateChapter>()
    val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    val getExcludedScanlators = mockk<GetExcludedScanlators>()
    val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())

    val interactor = SyncChaptersWithSource(
        SyncChaptersWithSource.Collaborators(
            downloadManager = downloadManager,
            downloadProvider = downloadProvider,
            chapterRepository = chapterRepository,
            shouldUpdateDbChapter = shouldUpdateDbChapter,
            updateManga = updateManga,
            updateChapter = updateChapter,
            getChaptersByMangaId = getChaptersByMangaId,
            getExcludedScanlators = getExcludedScanlators,
            libraryPreferences = libraryPreferences,
        ),
    )

    /** Makes the repository and manga updates succeed and hands back whatever chapters are added. */
    fun stubWrites() {
        coEvery { chapterRepository.removeChaptersWithIds(any()) } returns Unit
        coEvery { chapterRepository.addAll(any()) } answers { firstArg<List<Chapter>>().map { it.copy(id = 100) } }
        coEvery { updateChapter.awaitAll(any()) } returns Unit
        coEvery { updateManga.awaitUpdateFetchInterval(any(), any(), any()) } returns true
        coEvery { updateManga.awaitUpdateLastUpdate(any()) } returns true
        coEvery { getExcludedScanlators.await(any()) } returns emptySet()
    }
}

/** A library manga with [id] on [source]. */
internal fun libraryManga(id: Long = 1, source: Long = 7, title: String = "Title"): Manga =
    Manga.create().copy(id = id, source = source, ogTitle = title)

/** A source chapter at [url] named [name]. */
internal fun sChapter(url: String, name: String = "Chapter $url", dateUpload: Long = 0): SChapter =
    SChapter.create().apply {
        this.url = url
        this.name = name
        this.date_upload = dateUpload
    }

/** A stored chapter of [mangaId] at [url]. */
internal fun dbChapter(
    url: String,
    mangaId: Long = 1,
    chapterNumber: Double = -1.0,
    read: Boolean = false,
    bookmark: Boolean = false,
): Chapter = Chapter.create().copy(
    id = url.hashCode().toLong(),
    mangaId = mangaId,
    url = url,
    name = "Chapter $url",
    chapterNumber = chapterNumber,
    read = read,
    bookmark = bookmark,
)
