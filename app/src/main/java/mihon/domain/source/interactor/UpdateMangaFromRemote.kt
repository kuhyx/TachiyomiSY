package mihon.domain.source.interactor

import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MergedSource
import logcat.LogPriority
import mihon.domain.source.models.RemoteMangaUpdate
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import java.time.Instant

internal class UpdateMangaFromRemote(collaborators: Collaborators) {
    private val sourceManager = collaborators.sourceManager
    private val chapterRepository = collaborators.chapterRepository
    private val mangaRepository = collaborators.mangaRepository
    private val syncChaptersWithSource = collaborators.syncChaptersWithSource
    private val coverCache = collaborators.coverCache
    private val libraryPreferences = collaborators.libraryPreferences
    private val downloadManager = collaborators.downloadManager

    /** Everything [UpdateMangaFromRemote] talks to, resolved by the DI graph. */
    data class Collaborators(
        val sourceManager: SourceManager,
        val chapterRepository: ChapterRepository,
        val mangaRepository: MangaRepository,
        val syncChaptersWithSource: SyncChaptersWithSource,
        val coverCache: CoverCache,
        val libraryPreferences: LibraryPreferences,
        val downloadManager: DownloadManager,
    )

    suspend operator fun invoke(
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
        // SY -->
        throttleFunc: suspend () -> Unit = {},
        // SY <--
    ): Result<RemoteMangaUpdate> {
        val source = sourceManager.getOrStub(manga.source)
        return invoke(
            source = source,
            manga = manga,
            fetchDetails = fetchDetails,
            fetchChapters = fetchChapters,
            manualFetch = manualFetch,
            // SY -->
            throttleFunc = throttleFunc,
        )
    }

    suspend operator fun invoke(
        source: Source,
        manga: Manga,
        fetchDetails: Boolean = false,
        fetchChapters: Boolean = false,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
        throttleFunc: suspend () -> Unit = {},
    ): Result<RemoteMangaUpdate> {
        return try {
            val chapters = chapterRepository.getChapterByMangaId(manga.id)
                .sortedBy { it.sourceOrder }
            val update = withIOContext {
                // SY -->
                if (source is EHentai) {
                    source.getMangaUpdate(
                        manga = manga.toSManga(),
                        chapters = chapters.map(Chapter::toSChapter),
                        fetchDetails = fetchDetails,
                        fetchChapters = fetchChapters,
                        throttleFunc = throttleFunc,
                    )
                } else {
                    source.getMangaUpdate(
                        manga = manga.toSManga(),
                        chapters = chapters.map(Chapter::toSChapter),
                        fetchDetails = fetchDetails,
                        fetchChapters = fetchChapters,
                    )
                }
                // SY <--
            }
            awaitUpdateFromSource(manga, update.manga, manualFetch)
            // SY -->
            val newChapters = if (source is MergedSource) {
                source.fetchChaptersAndSync(manga, downloadChapters = manualFetch)
            } else {
                syncChaptersWithSource.await(
                    rawSourceChapters = update.chapters,
                    manga = manga,
                    source = source,
                    manualFetch = manualFetch,
                    fetchWindow = fetchWindow,
                )
            }
            // SY <--
            val updatedManga = mangaRepository.getMangaById(manga.id)

            Result.success(RemoteMangaUpdate(manga = updatedManga, newChapters = newChapters))
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            Result.failure(expected)
        }
    }

    private suspend fun awaitUpdateFromSource(
        localManga: Manga,
        remoteManga: SManga,
        manualFetch: Boolean,
    ): Boolean {
        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite (or 'update titles' preference is enabled), set its title from source and
        // update in db
        val title =
            if (remoteTitle.isNotEmpty() && (!localManga.favorite || libraryPreferences.updateMangaTitles.get())) {
                remoteTitle
            } else {
                null
            }

        val coverLastModified = when {
            // Never refresh covers if the url is empty to avoid "losing" existing covers
            remoteManga.thumbnail_url.isNullOrEmpty() -> {
                null
            }
            !manualFetch && localManga.thumbnailUrl == remoteManga.thumbnail_url -> {
                null
            }
            localManga.isLocal() -> {
                Instant.now().toEpochMilli()
            }
            localManga.hasCustomCover(coverCache) -> {
                coverCache.deleteFromCache(localManga, false)
                null
            }
            else -> {
                coverCache.deleteFromCache(localManga, false)
                Instant.now().toEpochMilli()
            }
        }

        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        val success = mangaRepository.update(
            MangaUpdate(
                id = localManga.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
                memo = remoteManga.memo,
            ),
        )
        if (success && title != null) {
            downloadManager.renameManga(localManga, title)
        }
        return success
    }
}
