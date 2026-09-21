package exh

import android.content.Context
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

// Use manga in DB if possible, otherwise make a new one; then fetch details and chapters.
internal suspend fun GalleryAdder.importManga(
    source: UrlImportableSource,
    mangaUrl: String,
    fav: Boolean,
    throttleFunc: suspend () -> Unit,
    retryCount: Int,
): Manga {
    var manga = getManga.await(mangaUrl, source.id)
        ?: networkToLocalManga(Manga.create().copy(source = source.id, url = mangaUrl))
    // Fetch and copy details
    manga = retry(retryCount) {
        updateMangaFromRemote(
            manga,
            fetchDetails = true,
            fetchChapters = true,
            manualFetch = false,
            throttleFunc = throttleFunc,
        ).getOrThrow().manga
    }
    if (fav) {
        updateManga.awaitUpdateFavorite(manga.id, true)
        manga = manga.copy(favorite = true)
    }
    return manga
}

internal suspend fun GalleryAdder.successEvent(
    url: String,
    manga: Manga,
    chapterUrl: String?,
    context: Context,
): GalleryAddEvent {
    if (chapterUrl == null) return GalleryAddEvent.Success(url, manga, context)
    val chapter = getChapter.await(chapterUrl, manga.id)
    return if (chapter != null) {
        GalleryAddEvent.Success(url, manga, context, chapter)
    } else {
        GalleryAddEvent.Fail.Error(
            url,
            context.stringResource(SYMR.strings.gallery_adder_could_not_identify_chapter, url),
        )
    }
}
