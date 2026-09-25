package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.fakeQuery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.data.Excluded_scanlatorsQueries
import tachiyomi.data.HistoryQueries
import tachiyomi.data.Manga_syncQueries
import tachiyomi.data.Mangas_categoriesQueries
import tachiyomi.data.MergedQueries
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.model.Manga

/**
 * A [MangaRestorer] over the [BackupKoin] mocks, with every query set it reads answering empty and
 * every insert handing back id [NEW_ID]. Koin is up so `Manga`'s custom-info lookup resolves.
 */
internal class MangaRestorerHarness {
    val graph = BackupKoin()
    val history: HistoryQueries = mockk(relaxed = true)
    val excluded: Excluded_scanlatorsQueries = mockk(relaxed = true)
    val merged: MergedQueries = mockk(relaxed = true)
    val mangaCategories: Mangas_categoriesQueries = mockk(relaxed = true)
    val mangaSync: Manga_syncQueries = mockk(relaxed = true)
    private val fetchInterval = mockk<FetchInterval> { every { getWindow(any()) } returns (0L to 0L) }

    fun start() {
        every { graph.database.historyQueries } returns history
        every { graph.database.excluded_scanlatorsQueries } returns excluded
        every { graph.database.mergedQueries } returns merged
        every { graph.database.mangas_categoriesQueries } returns mangaCategories
        every { graph.database.manga_syncQueries } returns mangaSync
        every { history.getHistoryByChapterUrl(any(), any()) } returns fakeQuery(emptyList())
        every { graph.chapters.getChapterByUrl(any()) } returns fakeQuery(emptyList())
        every { excluded.getExcludedScanlatorsByMangaId(any()) } returns fakeQuery(emptyList())
        every { merged.selectAll() } returns fakeQuery(emptyList())
        every { graph.mangas.getMangaByUrlAndSource(any(), any()) } returns fakeQuery(emptyList())
        every {
            graph.mangas.insertReturningId(
                source = any(),
                url = any(),
                artist = any(),
                author = any(),
                description = any(),
                genre = any(),
                title = any(),
                status = any(),
                thumbnailUrl = any(),
                favorite = any(),
                lastUpdate = any(),
                nextUpdate = any(),
                initialized = any(),
                viewerFlags = any(),
                chapterFlags = any(),
                coverLastModified = any(),
                dateAdded = any(),
                updateStrategy = any(),
                calculateInterval = any(),
                version = any(),
                notes = any(),
                memo = any(),
            )
        } returns fakeQuery(listOf(NEW_ID))
        startKoin { modules(graph.module()) }
    }

    fun stop() = stopKoin()

    fun restorer(isSync: Boolean = false): MangaRestorer = MangaRestorer(
        isSync = isSync,
        database = graph.database,
        getCategories = graph.getCategories,
        getMangaByUrlAndSourceId = graph.getMangaByUrlAndSourceId,
        getChaptersByMangaId = graph.getChaptersByMangaId,
        updateManga = graph.updateManga,
        getTracks = graph.getTracks,
        insertTrack = graph.insertTrack,
        fetchInterval = fetchInterval,
        setCustomMangaInfo = graph.setCustomMangaInfo,
        insertFlatMetadata = graph.insertFlatMetadata,
        getFlatMetadataById = graph.getFlatMetadataById,
    )

    companion object {
        const val NEW_ID = 100L
    }
}

/** A stored chapter with the fields the restorer compares. */
internal fun dbChapter(
    id: Long,
    url: String,
    read: Boolean = false,
    bookmark: Boolean = false,
    lastPageRead: Long = 0,
    version: Long = 0,
): Chapter = Chapter.create().copy(
    id = id,
    mangaId = 1L,
    url = url,
    name = url,
    read = read,
    bookmark = bookmark,
    lastPageRead = lastPageRead,
    chapterNumber = 0.0,
    version = version,
)

/** A stored library entry. */
internal fun dbManga(id: Long = 1L, version: Long = 0, favorite: Boolean = true): Manga =
    Manga.create().copy(id = id, source = 1L, url = "/m", ogTitle = "Stored", version = version, favorite = favorite)
