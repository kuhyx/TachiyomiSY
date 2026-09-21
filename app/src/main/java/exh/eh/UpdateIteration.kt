package exh.eh

import exh.metadata.metadata.EHentaiSearchMetadata
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

// Mutable bookkeeping for one updater run.
internal class UpdateIteration(private val worker: EHentaiUpdateWorker, private val total: Int) {
    var failures: Int = 0
    var updated: Int = 0
    val updatedManga: MutableList<Pair<Manga, Array<Chapter>>> = mutableListOf()
    private val modified = mutableSetOf<Long>()

    suspend fun updateGallery(index: Int, entry: UpdateEntry) {
        val (manga, meta) = entry
        worker.logger.d(
            "Updating gallery (index: %s, manga.id: %s, meta.gId: %s, meta.gToken: %s, " +
                "failures-so-far: %s, modifiedThisIteration.size: %s)...",
            index,
            manga.id,
            meta.gId,
            meta.gToken,
            failures,
            modified.size,
        )

        if (manga.id in modified) {
            // We already processed this manga!
            worker.logger.w("Gallery already updated this iteration, skipping...")
            updated++
            return
        }

        val (new, chapters) = fetchChapters(manga, meta) ?: return

        // Find accepted root and discard others
        val (acceptedRoot, discardedRoots, exhNew) =
            worker.updateHelper.acceptRootAndDiscardOthers(manga.source, chapters)

        if (new.isNotEmpty() && manga.id == acceptedRoot.manga.id) {
            worker.libraryPreferences.newUpdatesCount.getAndSet { it + new.size }
            updatedManga += acceptedRoot.manga to new.toTypedArray()
        } else if (exhNew.isNotEmpty() && updatedManga.none { it.first.id == acceptedRoot.manga.id }) {
            worker.libraryPreferences.newUpdatesCount.getAndSet { it + exhNew.size }
            updatedManga += acceptedRoot.manga to exhNew.toTypedArray()
        }

        modified += acceptedRoot.manga.id
        modified += discardedRoots.map { it.manga.id }
        updated++
    }

    // (new, current) chapters, or null when the gallery could not be updated or came back empty.
    private suspend fun fetchChapters(
        manga: Manga,
        meta: EHentaiSearchMetadata,
    ): Pair<List<Chapter>, List<Chapter>>? {
        val fetched = fetchOrNull(manga, meta)
        if (fetched != null && fetched.second.isEmpty()) {
            worker.logger.e(
                "No chapters found for gallery (manga.id: %s, meta.gId: %s, meta.gToken: %s, " +
                    "failures-so-far: %s)!",
                manga.id,
                meta.gId,
                meta.gToken,
                failures,
            )
            return null
        }
        return fetched
    }

    // Network failures count towards the run's failure tally.
    private suspend fun fetchOrNull(
        manga: Manga,
        meta: EHentaiSearchMetadata,
    ): Pair<List<Chapter>, List<Chapter>>? =
        try {
            worker.updateNotifier.showProgressNotification(manga, updated + failures, total)
            worker.updateEntryAndGetChapters(manga)
        } catch (e: GalleryNotUpdatedException) {
            if (e.network) {
                failures++

                worker.logger.e("> Network error while updating gallery!", e)
                worker.logger.e(
                    "> (manga.id: %s, meta.gId: %s, meta.gToken: %s, failures-so-far: %s)",
                    manga.id,
                    meta.gId,
                    meta.gToken,
                    failures,
                )
            }
            null
        }
}
