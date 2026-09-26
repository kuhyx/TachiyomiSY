@file:OptIn(ExperimentalAtomicApi::class)

package eu.kanade.tachiyomi.data.library

import exh.source.LIBRARY_UPDATE_EXCLUDED_SOURCES
import exh.source.MERGED_SOURCE_ID
import exh.source.mangaDexSourceIds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val MAX_CONCURRENT_SOURCES = 5

// Method that updates manga in [mangaToUpdate]. It's called in a background thread, so it's safe
// to do heavy operations or network calls here.
// For each manga it calls [updateManga] and updates the notification showing the current
// progress.
// @return an observable delivering the progress of each update.
internal suspend fun LibraryUpdateJob.updateChapterList() {
    val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)
    val run = LibraryUpdateRun(fetchInterval.getWindow(ZonedDateTime.now()))
    // SY -->
    val mdlistLogged = mdList.isLoggedIn
    // SY <--

    coroutineScope {
        mangaToUpdate.groupBy { it.manga.source }
            // SY -->
            .filterNot { it.key in LIBRARY_UPDATE_EXCLUDED_SOURCES }
            // SY <--
            .values
            .map { mangaInSource ->
                async {
                    semaphore.withPermit {
                        // SY -->
                        val isMangaDex = mangaInSource.first().manga.source in mangaDexSourceIds
                        if (mdlistLogged && isMangaDex) {
                            launch { addInitialMdListTracks(mangaInSource) }
                        }
                        // SY <--
                        mangaInSource.forEach { libraryManga ->
                            ensureActive()
                            updateIfInLibrary(libraryManga.manga, run)
                        }
                    }
                }
            }
            .awaitAll()
    }
    reportRun(run)
}

internal fun LibraryUpdateJob.downloadChapters(manga: Manga, chapters: List<Chapter>) {
    // We don't want to start downloading while the library is updating, because websites
    // may don't like it and they could ban the user.
    // SY -->
    if (manga.source == MERGED_SOURCE_ID) {
        val downloadingManga = runBlocking { getMergedMangaForDownloading.await(manga.id) }
            .associateBy { it.id }
        chapters.groupBy { it.mangaId }
            .forEach { (mangaId, mangaChapters) ->
                downloadingManga[mangaId]?.let { downloadManager.downloadChapters(it, mangaChapters, false) }
            }

        return
    }
    // SY <--
    downloadManager.downloadChapters(manga, chapters, false)
}

// Updates the chapters for the given manga and adds them to the database.
// @param manga the manga to update.
// @return a pair of the inserted and removed chapters.
internal suspend fun LibraryUpdateJob.updateManga(manga: Manga, fetchWindow: Pair<Long, Long>): List<Chapter> {
    val source = sourceManager.getOrStub(manga.source)

    val update = updateMangaFromRemote(
        source = source,
        manga = manga,
        fetchDetails = libraryPreferences.autoUpdateMetadata.get(),
        fetchChapters = true,
        fetchWindow = fetchWindow,
    )
        .getOrThrow()

    return if (update.manga.favorite) update.newChapters else emptyList()
}
