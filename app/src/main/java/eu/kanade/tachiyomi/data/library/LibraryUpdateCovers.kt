@file:OptIn(ExperimentalAtomicApi::class)

package eu.kanade.tachiyomi.data.library

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

private const val MAX_CONCURRENT_SOURCES = 5

internal suspend fun LibraryUpdateJob.updateCovers() {
    val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)
    val progressCount = AtomicInt(0)
    val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()

    coroutineScope {
        mangaToUpdate.groupBy { it.manga.source }
            .values
            .map { mangaInSource ->
                async {
                    semaphore.withPermit {
                        mangaInSource.forEach { libraryManga ->
                            val manga = libraryManga.manga
                            ensureActive()

                            withUpdateNotification(
                                currentlyUpdatingManga,
                                progressCount,
                                manga,
                            ) {
                                refreshCover(manga)
                            }
                        }
                    }
                }
            }
            .awaitAll()
    }

    notifier.cancelProgressNotification()
}

internal suspend fun LibraryUpdateJob.withUpdateNotification(
    updatingManga: CopyOnWriteArrayList<Manga>,
    completed: AtomicInt,
    manga: Manga,
    block: suspend () -> Unit,
) = coroutineScope {
    ensureActive()

    updatingManga.add(manga)
    notifier.showProgressNotification(
        updatingManga,
        completed.load(),
        mangaToUpdate.size,
    )

    block()

    ensureActive()

    updatingManga.remove(manga)
    completed.incrementAndFetch()
    notifier.showProgressNotification(
        updatingManga,
        completed.load(),
        mangaToUpdate.size,
    )
}
