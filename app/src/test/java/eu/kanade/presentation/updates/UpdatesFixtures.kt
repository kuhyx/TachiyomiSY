package eu.kanade.presentation.updates

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

/** One updates row; the chapter id is [mangaId] * 10 so every row keys uniquely. */
internal fun updatesItem(
    mangaId: Long = 1L,
    read: Boolean = false,
    bookmark: Boolean = false,
    lastPageRead: Long = 0L,
    sourceId: Long = 2L,
    dateFetch: Long = 0L,
    state: Download.State = Download.State.NOT_DOWNLOADED,
    selected: Boolean = false,
): UpdatesItem = UpdatesItem(
    update = UpdatesWithRelations(
        mangaId = mangaId,
        ogMangaTitle = "Manga $mangaId",
        chapterId = mangaId * 10,
        chapterName = "Chapter $mangaId",
        scanlator = null,
        chapterUrl = "/c/$mangaId",
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = true,
            ogUrl = null,
            lastModified = 0L,
        ),
    ),
    downloadStateProvider = { state },
    downloadProgressProvider = { 0 },
    selected = selected,
)
