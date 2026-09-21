package mihon.domain.migration.usecases

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import mihon.domain.migration.models.MigrationFlag
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.toHistoryUpdate
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import java.time.Instant

internal class MigrateMangaUseCase(collaborators: Collaborators) {
    private val sourcePreferences = collaborators.sourcePreferences
    private val trackerManager = collaborators.trackerManager
    private val sourceManager = collaborators.sourceManager
    private val downloadManager = collaborators.downloadManager
    private val updateManga = collaborators.updateManga
    private val getChaptersByMangaId = collaborators.getChaptersByMangaId
    private val getHistoryByMangaId = collaborators.getHistoryByMangaId
    private val updateChapter = collaborators.updateChapter
    private val updateHistory = collaborators.updateHistory
    private val getCategories = collaborators.getCategories
    private val setMangaCategories = collaborators.setMangaCategories
    private val getTracks = collaborators.getTracks
    private val insertTrack = collaborators.insertTrack
    private val coverCache = collaborators.coverCache
    private val updateMangaFromRemote = collaborators.updateMangaFromRemote

    /** Everything [MigrateMangaUseCase] talks to, resolved by the DI graph. */
    data class Collaborators(
        val sourcePreferences: SourcePreferences,
        val trackerManager: TrackerManager,
        val sourceManager: SourceManager,
        val downloadManager: DownloadManager,
        val updateManga: UpdateManga,
        val getChaptersByMangaId: GetChaptersByMangaId,
        val getHistoryByMangaId: GetHistory,
        val updateChapter: UpdateChapter,
        val updateHistory: UpsertHistory,
        val getCategories: GetCategories,
        val setMangaCategories: SetMangaCategories,
        val getTracks: GetTracks,
        val insertTrack: InsertTrack,
        val coverCache: CoverCache,
        val updateMangaFromRemote: UpdateMangaFromRemote,
    )

    private val enhancedServices by lazy { trackerManager.trackers.filterIsInstance<EnhancedTracker>() }

    suspend operator fun invoke(
        current: Manga,
        target: Manga,
        replace: Boolean,
        // SY -->
        throttleFunc: suspend () -> Unit = {},
        // SY <--
    ) {
        val targetSource = sourceManager.get(target.source) ?: return
        val currentSource = sourceManager.get(current.source)
        val flags = sourcePreferences.migrationFlags.get()

        try {
            updateMangaFromRemote(
                target,
                fetchChapters = true,
                // SY -->
                throttleFunc = throttleFunc,
                // SY <--
            ).getOrThrow()

            // Update chapters read state, history, bookmark and dateFetch
            if (MigrationFlag.CHAPTER in flags) {
                migrateChapters(current, target)
            }
            // Update categories
            if (MigrationFlag.CATEGORY in flags) {
                val categoryIds = getCategories.await(current.id).map { it.id }
                setMangaCategories.await(target.id, categoryIds)
            }
            migrateTracks(current, currentSource, target, targetSource)
            // Delete downloaded
            if (MigrationFlag.REMOVE_DOWNLOAD in flags && currentSource != null) {
                downloadManager.deleteManga(current, currentSource)
            }
            // Update custom cover (recheck if custom cover exists)
            if (MigrationFlag.CUSTOM_COVER in flags && current.hasCustomCover()) {
                coverCache.setCustomCoverToCache(target, coverCache.getCustomCoverFile(current.id).inputStream())
            }
            swapFavorite(current, target, replace, flags)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Rethrown (or wrapped) whatever the cause.
        }
    }

    // SY -->

    // Carries read state, bookmarks, page progress and history over to the target's matching chapter numbers.
    private suspend fun migrateChapters(current: Manga, target: Manga) {
        val chapterUpdates = mutableListOf<ChapterUpdate>()
        val targetChapters = getChaptersByMangaId.await(target.id)
        val currentChapters = getChaptersByMangaId.await(current.id)
        val historyUpdates = mutableListOf<HistoryUpdate>()
        val targetHistory = getHistoryByMangaId.await(target.id)
        val currentHistory = getHistoryByMangaId.await(current.id)
        val maxChapterRead = currentChapters
            .filter { it.read }
            .maxOfOrNull { it.chapterNumber }

        targetChapters.forEach { mangaChapter ->
            var updatedChapter = mangaChapter
            val prevChapter = mangaChapter.takeIf { it.isRecognizedNumber }?.let { chapter ->
                currentChapters.find { it.isRecognizedNumber && it.chapterNumber == chapter.chapterNumber }
            }
            if (prevChapter != null) {
                updatedChapter = updatedChapter.copy(
                    dateFetch = prevChapter.dateFetch,
                    bookmark = prevChapter.bookmark,
                    lastPageRead = prevChapter.lastPageRead,
                )
                // History moves over unless the target chapter is already read with history of its own.
                val updatedHistory = currentHistory.find { it.chapterId == prevChapter.id }
                val chapterHasHistory = mangaChapter.read && targetHistory.any { it.chapterId == mangaChapter.id }
                if (updatedHistory != null && !chapterHasHistory) {
                    historyUpdates.add(updatedHistory.copy(chapterId = updatedChapter.id).toHistoryUpdate())
                }
            }
            val readBefore = maxChapterRead != null && mangaChapter.chapterNumber <= maxChapterRead
            if (mangaChapter.isRecognizedNumber && readBefore) {
                updatedChapter = updatedChapter.copy(read = true)
            }
            chapterUpdates.add(updatedChapter.toChapterUpdate())
        }
        updateChapter.awaitAll(chapterUpdates)
        updateHistory.awaitAll(historyUpdates)
    }
    // SY <--

    // Re-points every track at the target; an enhanced tracker gets to migrate its own remote entry.
    private suspend fun migrateTracks(current: Manga, currentSource: Source?, target: Manga, targetSource: Source) {
        getTracks.await(current.id).mapNotNull { track ->
            val updatedTrack = track.copy(mangaId = target.id)
            val service = enhancedServices.firstOrNull { it.isTrackFrom(updatedTrack, current, currentSource) }
            if (service != null) {
                service.migrateTrack(updatedTrack, target, targetSource)
            } else {
                updatedTrack
            }
        }
            .takeIf { it.isNotEmpty() }
            ?.let { insertTrack.awaitAll(it) }
    }

    private suspend fun swapFavorite(current: Manga, target: Manga, replace: Boolean, flags: Set<MigrationFlag>) {
        val currentMangaUpdate = MangaUpdate(
            id = current.id,
            favorite = false,
            dateAdded = 0,
        )
            .takeIf { replace }
        val targetMangaUpdate = MangaUpdate(
            id = target.id,
            favorite = true,
            chapterFlags = current.chapterFlags,
            viewerFlags = current.viewerFlags,
            dateAdded = if (replace) current.dateAdded else Instant.now().toEpochMilli(),
            notes = if (MigrationFlag.NOTES in flags) current.notes else null,
        )
        updateManga.awaitAll(listOfNotNull(currentMangaUpdate, targetMangaUpdate))
    }
}
