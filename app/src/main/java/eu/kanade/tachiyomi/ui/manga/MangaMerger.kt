package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.model.copyFrom
import eu.kanade.domain.manga.model.toSManga
import exh.source.MERGED_SOURCE_ID
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.manga.interactor.DeleteByMergeId
import tachiyomi.domain.manga.interactor.DeleteMangaById
import tachiyomi.domain.manga.interactor.DeleteMergeById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.interactor.UpdateMergedSettings
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The merged-entry operations of the manga screen (SY): joining a manga into an existing merged
 * entry or into a new one, and editing or removing the references that make one up.
 */
internal class MangaMerger(
    private val context: Context,
    private val getManga: GetManga = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val insertMergedReference: InsertMergedReference = Injekt.get(),
    private val updateMergedSettings: UpdateMergedSettings = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val deleteMangaById: DeleteMangaById = Injekt.get(),
    private val deleteByMergeId: DeleteByMergeId = Injekt.get(),
    private val deleteMergeById: DeleteMergeById = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
) {
    suspend fun smartSearchMerge(manga: Manga, originalMangaId: Long): Manga {
        val originalManga = getManga.await(originalMangaId)
            ?: throw IllegalArgumentException(context.stringResource(SYMR.strings.merge_unknown_entry, originalMangaId))
        return if (originalManga.source == MERGED_SOURCE_ID) {
            addToMergedManga(originalManga, manga)
        } else {
            mergeIntoNewManga(originalManga, manga)
        }
    }

    // [originalManga] is already a merged entry: [manga] becomes one more of its parts.
    private suspend fun addToMergedManga(originalManga: Manga, manga: Manga): Manga {
        val originalMangaId = originalManga.id
        val children = getMergedReferencesById.await(originalMangaId)
        if (children.any { it.mangaSourceId == manga.source && it.mangaUrl == manga.url }) {
            throw IllegalArgumentException(context.stringResource(SYMR.strings.merged_already))
        }

        val mangaReferences = mutableListOf(
            MergedMangaReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = true,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = true,
                mergeId = originalManga.id,
                mergeUrl = originalManga.url,
                mangaId = manga.id,
                mangaUrl = manga.url,
                mangaSourceId = manga.source,
            ),
        )

        if (children.isEmpty() || children.all { it.mangaSourceId != MERGED_SOURCE_ID }) {
            mangaReferences += MergedMangaReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = false,
                chapterSortMode = 0,
                chapterPriority = -1,
                downloadChapters = false,
                mergeId = originalManga.id,
                mergeUrl = originalManga.url,
                mangaId = originalManga.id,
                mangaUrl = originalManga.url,
                mangaSourceId = MERGED_SOURCE_ID,
            )
        }

        // todo
        insertMergedReference.awaitAll(mangaReferences)

        return originalManga
    }

    // Creates the merged entry that holds [originalManga] and [manga].
    private suspend fun mergeIntoNewManga(originalManga: Manga, manga: Manga): Manga {
        val originalMangaId = originalManga.id
        if (manga.id == originalMangaId) {
            throw IllegalArgumentException(context.stringResource(SYMR.strings.merged_already))
        }
        var mergedManga = Manga.create()
            .copy(
                url = originalManga.url,
                ogTitle = originalManga.title,
                source = MERGED_SOURCE_ID,
            )
            .copyFrom(originalManga.toSManga())
            .copy(
                favorite = true,
                lastUpdate = originalManga.lastUpdate,
                viewerFlags = originalManga.viewerFlags,
                chapterFlags = originalManga.chapterFlags,
                dateAdded = System.currentTimeMillis(),
            )

        var existingManga = getManga.await(mergedManga.url, mergedManga.source)
        while (existingManga != null) {
            if (existingManga.favorite) {
                throw IllegalArgumentException(context.stringResource(SYMR.strings.merge_duplicate))
            } else {
                withNonCancellableContext {
                    existingManga?.id?.let {
                        deleteByMergeId.await(it)
                        deleteMangaById.await(it)
                    }
                }
            }
            existingManga = getManga.await(mergedManga.url, mergedManga.source)
        }

        mergedManga = networkToLocalManga(mergedManga)

        getCategories.await(originalMangaId)
            .let {
                setMangaCategories.await(mergedManga.id, it.map { it.id })
            }

        val originalMangaReference = MergedMangaReference(
            id = -1,
            isInfoManga = true,
            getChapterUpdates = true,
            chapterSortMode = 0,
            chapterPriority = 0,
            downloadChapters = true,
            mergeId = mergedManga.id,
            mergeUrl = mergedManga.url,
            mangaId = originalManga.id,
            mangaUrl = originalManga.url,
            mangaSourceId = originalManga.source,
        )

        val newMangaReference = MergedMangaReference(
            id = -1,
            isInfoManga = false,
            getChapterUpdates = true,
            chapterSortMode = 0,
            chapterPriority = 0,
            downloadChapters = true,
            mergeId = mergedManga.id,
            mergeUrl = mergedManga.url,
            mangaId = manga.id,
            mangaUrl = manga.url,
            mangaSourceId = manga.source,
        )

        val mergedMangaReference = MergedMangaReference(
            id = -1,
            isInfoManga = false,
            getChapterUpdates = false,
            chapterSortMode = 0,
            chapterPriority = -1,
            downloadChapters = false,
            mergeId = mergedManga.id,
            mergeUrl = mergedManga.url,
            mangaId = mergedManga.id,
            mangaUrl = mergedManga.url,
            mangaSourceId = MERGED_SOURCE_ID,
        )

        insertMergedReference.awaitAll(listOf(originalMangaReference, newMangaReference, mergedMangaReference))

        // Note that if the manga are merged in a different order, this won't trigger, but I don't care lol
        return mergedManga
    }

    suspend fun updateMergeSettings(mergedMangaReferences: List<MergedMangaReference>) {
        if (mergedMangaReferences.isEmpty()) return
        updateMergedSettings.awaitAll(
            mergedMangaReferences.map {
                MergeMangaSettingsUpdate(
                    id = it.id,
                    isInfoManga = it.isInfoManga,
                    getChapterUpdates = it.getChapterUpdates,
                    chapterPriority = it.chapterPriority,
                    downloadChapters = it.downloadChapters,
                    chapterSortMode = it.chapterSortMode,
                )
            },
        )
    }

    suspend fun deleteMerge(reference: MergedMangaReference) {
        deleteMergeById.await(reference.id)
    }
}

/** Saves the edited merge references off the screen model's scope. */
internal fun MangaScreenModel.updateMergeSettings(mergedMangaReferences: List<MergedMangaReference>) {
    screenModelScope.launchNonCancellable { merger.updateMergeSettings(mergedMangaReferences) }
}

/** Removes one merged entry off the screen model's scope. */
internal fun MangaScreenModel.deleteMerge(reference: MergedMangaReference) {
    screenModelScope.launchNonCancellable { merger.deleteMerge(reference) }
}
