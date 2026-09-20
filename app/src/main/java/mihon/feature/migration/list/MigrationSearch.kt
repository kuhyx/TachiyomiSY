package mihon.feature.migration.list

import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import mihon.feature.migration.list.MigrationListScreenModel.ChapterInfo
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal suspend fun MigrationListScreenModel.getChapterInfo(
    id: Long,
) = getChaptersByMangaId.await(id).let { chapters ->
    ChapterInfo(
        latestChapter = chapters.maxOfOrNull { it.chapterNumber },
        chapterCount = chapters.size,
    )
}

// Every source in parallel; the match with the most chapters wins (none = not found).
internal suspend fun MigrationListScreenModel.searchByMostChapters(
    manga: MigratingManga,
    sources: List<Source>,
    deepSearchMode: Boolean,
): Pair<Manga, ChapterInfo>? = coroutineScope {
    val sourceSemaphore = Semaphore(MAX_CONCURRENT_SOURCES)
    sources.map { source ->
        async {
            sourceSemaphore.withPermit {
                searchSource(manga.manga, source, deepSearchMode)?.takeIf { it.second.chapterCount > 0 }
            }
        }
    }
        .mapNotNull { it.await() }
        .maxByOrNull { it.second.latestChapter ?: 0.0 }
}

// Sources in order; the first that knows the manga wins.
internal suspend fun MigrationListScreenModel.searchFirstMatch(
    manga: MigratingManga,
    sources: List<Source>,
    deepSearchMode: Boolean,
): Pair<Manga, ChapterInfo>? = sources.firstNotNullOfOrNull { searchSource(manga.manga, source = it, deepSearchMode) }

internal suspend fun MigrationListScreenModel.searchSource(
    manga: Manga,
    source: Source,
    deepSearchMode: Boolean,
): Pair<Manga, ChapterInfo>? {
    return try {
        val searchResult = if (deepSearchMode) {
            smartSearchEngine.deepSearch(source, manga.title)
        } else {
            smartSearchEngine.regularSearch(source, manga.title)
        }

        if (searchResult == null || (searchResult.url == manga.url && source.id == manga.source)) return null

        val localManga = networkToLocalManga(searchResult)
        try {
            updateMangaFromRemote(
                localManga,
                fetchChapters = true,
                // SY -->
                throttleFunc = throttleManager::throttle,
                // SY <--
            ).getOrThrow()
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
        localManga to getChapterInfo(localManga.id)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
