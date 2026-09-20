package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.util.removeCovers
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.source.isEhBasedManga
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import exh.util.nullIfBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import uy.kohesive.injekt.api.get

/**
 * Queues the amount specified of unread chapters from the list of selected manga.
 */
internal fun LibraryScreenModel.performDownloadAction(action: DownloadAction) {
    val mangas = state.value.selectedManga
    screenModelScope.launchNonCancellable {
        if (action == DownloadAction.BOOKMARKED_CHAPTERS) {
            downloads.downloadBookmarkedChapters(mangas)
        } else {
            downloads.downloadNextChapters(mangas, action.nextChapters)
        }
    }
    clearSelection()
}

// SY -->
internal fun LibraryScreenModel.cleanTitles() {
    state.value.selectedManga.fastFilter {
        it.isEhBasedManga() ||
            it.source in nHentaiSourceIds
    }.fastForEach { manga ->
        val editedTitle =
            manga.title.replace("\\[.*?]".toRegex(), "").trim().replace("\\(.*?\\)".toRegex(), "").trim()
                .replace("\\{.*?\\}".toRegex(), "")
                .trim()
                .let {
                    if (it.contains("|")) {
                        it.replace(".*\\|".toRegex(), "").trim()
                    } else {
                        it
                    }
                }
        if (manga.title != editedTitle) {
            val mangaInfo = CustomMangaInfo(
                id = manga.id,
                title = editedTitle.nullIfBlank(),
                author = manga.author.takeUnless { it == manga.ogAuthor },
                artist = manga.artist.takeUnless { it == manga.ogArtist },
                thumbnailUrl = manga.thumbnailUrl.takeUnless { it == manga.ogThumbnailUrl },
                description = manga.description.takeUnless { it == manga.ogDescription },
                genre = manga.genre.takeUnless { it == manga.ogGenre },
                status = manga.status.takeUnless { it == manga.ogStatus },
            )

            setCustomMangaInfo.set(mangaInfo)
        }
    }
    clearSelection()
}

@OptIn(DelicateCoroutinesApi::class)
internal fun LibraryScreenModel.syncMangaToDex() {
    launchIO {
        MdUtil.getEnabledMangaDex(sourcePreferences, sourceManager)?.let { mdex ->
            state.value.selectedManga.fastFilter { it.source in mangaDexSourceIds }.fastForEach { manga ->
                mdex.updateFollowStatus(MdUtil.getMangaId(manga.url), FollowStatus.READING)
            }
        }
        clearSelection()
    }
}

internal fun LibraryScreenModel.resetInfo() {
    state.value.selectedManga.fastForEach { manga ->
        val mangaInfo = CustomMangaInfo(
            id = manga.id,
            title = null,
            author = null,
            artist = null,
            thumbnailUrl = null,
            description = null,
            genre = null,
            status = null,
        )

        setCustomMangaInfo.set(mangaInfo)
    }
    clearSelection()
}

/**
 * Marks mangas' chapters read status.
 */
internal fun LibraryScreenModel.markReadSelection(read: Boolean) {
    val selection = state.value.selectedManga
    screenModelScope.launchNonCancellable {
        selection.forEach { manga ->
            setReadStatus.await(
                manga = manga,
                read = read,
            )
        }
    }
    clearSelection()
}

/**
 * Remove the selected manga.
 *
 * @param mangas the list of manga to delete.
 * @param deleteFromLibrary whether to delete manga from library.
 * @param deleteChapters whether to delete downloaded chapters.
 */
internal fun LibraryScreenModel.removeMangas(mangas: List<Manga>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
    screenModelScope.launchNonCancellable {
        if (deleteFromLibrary) {
            val toDelete = mangas.map {
                it.removeCovers(coverCache)
                MangaUpdate(
                    favorite = false,
                    id = it.id,
                )
            }
            updateManga.awaitAll(toDelete)
        }

        if (deleteChapters) {
            mangas.forEach { manga ->
                val source = sourceManager.get(manga.source) as? HttpSource
                if (source != null) {
                    if (source is MergedSource) {
                        val mergedMangas = getMergedMangaById.await(manga.id)
                        val sources = mergedMangas.distinctBy {
                            it.source
                        }.map { sourceManager.getOrStub(it.source) }
                        mergedMangas.forEach merge@{ mergedManga ->
                            val mergedSource =
                                sources.firstOrNull { mergedManga.source == it.id } as? HttpSource ?: return@merge
                            downloadManager.deleteManga(mergedManga, mergedSource)
                        }
                    } else {
                        downloadManager.deleteManga(manga, source)
                    }
                }
            }
        }
    }
}

/**
 * Bulk update categories of manga using old and new common categories.
 *
 * @param mangaList the list of manga to move.
 * @param addCategories the categories to add for all mangas.
 * @param removeCategories the categories to remove in all mangas.
 */
internal fun LibraryScreenModel.setMangaCategories(
    mangaList: List<Manga>,
    addCategories: List<Long>,
    removeCategories: List<Long>,
) {
    screenModelScope.launchNonCancellable {
        mangaList.forEach { manga ->
            val categoryIds = getCategories.await(manga.id)
                .map { it.id }
                .subtract(removeCategories.toSet())
                .plus(addCategories)
                .toList()

            setMangaCategories.await(manga.id, categoryIds)
        }
    }
}
