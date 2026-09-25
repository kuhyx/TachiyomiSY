package mihon.domain.migration.usecases

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track

/** The migration use case with every collaborator mocked; the mocks are reachable as properties. */
internal class MigrateMangaHarness {
    val sourcePreferences = SourcePreferences(FlowPreferenceStore())
    val trackerManager = mockk<TrackerManager>()
    val sourceManager = mockk<SourceManager>()
    val downloadManager = mockk<DownloadManager>()
    val updateManga = mockk<UpdateManga>()
    val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    val getHistoryByMangaId = mockk<GetHistory>()
    val updateChapter = mockk<UpdateChapter>()
    val updateHistory = mockk<UpsertHistory>()
    val getCategories = mockk<GetCategories>()
    val setMangaCategories = mockk<SetMangaCategories>()
    val getTracks = mockk<GetTracks>()
    val insertTrack = mockk<InsertTrack>()
    val coverCache = mockk<CoverCache>()
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
    val currentSource = mockk<Source>()
    val targetSource = mockk<Source>()

    val useCase = MigrateMangaUseCase(
        MigrateMangaUseCase.Collaborators(
            sourcePreferences = sourcePreferences,
            trackerManager = trackerManager,
            sourceManager = sourceManager,
            downloadManager = downloadManager,
            updateManga = updateManga,
            getChaptersByMangaId = getChaptersByMangaId,
            getHistoryByMangaId = getHistoryByMangaId,
            updateChapter = updateChapter,
            updateHistory = updateHistory,
            getCategories = getCategories,
            setMangaCategories = setMangaCategories,
            getTracks = getTracks,
            insertTrack = insertTrack,
            coverCache = coverCache,
            updateMangaFromRemote = updateMangaFromRemote,
        ),
    )

    val chapterUpdates = mutableListOf<List<ChapterUpdate>>()
    val historyUpdates = mutableListOf<List<HistoryUpdate>>()
    val mangaUpdates = mutableListOf<List<MangaUpdate>>()
    val trackInserts = mutableListOf<List<Track>>()

    /** Both sources known, an empty library state and every write accepted. */
    fun stubHappyPath() {
        every { sourceManager.get(CURRENT_SOURCE) } returns currentSource
        every { sourceManager.get(TARGET_SOURCE) } returns targetSource
        every { trackerManager.trackers } returns emptyList()
        coEvery { updateMangaFromRemote(target, fetchChapters = true, throttleFunc = any()) } returns
            Result.success(RemoteMangaUpdate(target, emptyList()))
        coEvery { getChaptersByMangaId.await(any()) } returns emptyList()
        coEvery { getHistoryByMangaId.await(any()) } returns emptyList()
        coEvery { updateChapter.awaitAll(capture(chapterUpdates)) } returns Unit
        coEvery { updateHistory.awaitAll(capture(historyUpdates)) } returns Unit
        coEvery { getCategories.await(current.id) } returns emptyList()
        coEvery { setMangaCategories.await(target.id, any()) } returns Unit
        coEvery { getTracks.await(current.id) } returns emptyList()
        coEvery { insertTrack.awaitAll(capture(trackInserts)) } returns Unit
        coEvery { updateManga.awaitAll(capture(mangaUpdates)) } returns true
    }

    companion object {
        const val CURRENT_SOURCE = 1L
        const val TARGET_SOURCE = 2L
        val current: Manga = Manga.create().copy(
            id = 10,
            source = CURRENT_SOURCE,
            ogTitle = "Current",
            chapterFlags = 5,
            viewerFlags = 9,
            dateAdded = 1_000,
            notes = "keep me",
        )
        val target: Manga = Manga.create().copy(id = 20, source = TARGET_SOURCE, ogTitle = "Target")
    }
}

/** A chapter of [mangaId] numbered [number]. */
internal fun migratedChapter(
    id: Long,
    mangaId: Long,
    number: Double,
    read: Boolean = false,
    bookmark: Boolean = false,
    lastPageRead: Long = 0,
    dateFetch: Long = 0,
): Chapter = Chapter.create().copy(
    id = id,
    mangaId = mangaId,
    chapterNumber = number,
    read = read,
    bookmark = bookmark,
    lastPageRead = lastPageRead,
    dateFetch = dateFetch,
)
