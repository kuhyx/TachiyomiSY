package exh.favorites

import exh.GalleryAddEvent
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.getUrl
import tachiyomi.i18n.sy.SYMR

// The local half of a favourites sync: what the remote change set does to the library.

internal suspend fun FavoritesSyncHelper.applyChangeSetToLocal(
    errorList: MutableList<FavoritesSyncStatus.SyncError.GallerySyncError>,
    changeSet: ChangeSet,
) {
    val removedManga = removeFromLocal(changeSet)
    // Can't do too many DB OPs in one go
    removedManga.forEach {
        setMangaCategories.await(it.id, emptyList())
    }
    val insertedMangaCategories = addToLocal(changeSet, errorList)
    // Can't do too many DB OPs in one go
    insertedMangaCategories.forEach { (category, manga) ->
        setMangaCategories.await(manga.id, listOf(category))
    }
}

// Unfavourites every library entry the remote removed, on both the EX and EH sources.
private suspend fun FavoritesSyncHelper.removeFromLocal(changeSet: ChangeSet): List<Manga> {
    val removedManga = mutableListOf<Manga>()
    changeSet.removed.forEachIndexed { index, gallery ->
        status.value = FavoritesSyncStatus.Processing.RemovingGalleryFromLocal(
            index = index + 1,
            total = changeSet.removed.size,
        )
        val url = gallery.getUrl()
        // Consider both EX and EH sources
        listOf(EXH_SOURCE_ID, EH_SOURCE_ID).forEach {
            val manga = getManga.await(url, it)
            if (manga?.favorite == true) {
                updateManga.awaitUpdateFavorite(manga.id, false)
                removedManga += manga
            }
        }
    }
    return removedManga
}

// Imports every gallery the remote added; returns the category each landed in, to be applied afterwards.
private suspend fun FavoritesSyncHelper.addToLocal(
    changeSet: ChangeSet,
    errorList: MutableList<FavoritesSyncStatus.SyncError.GallerySyncError>,
): List<Pair<Long, Manga>> {
    val insertedMangaCategories = mutableListOf<Pair<Long, Manga>>()
    val categories = getCategories.await().filterNot(Category::isSystemCategory)
    throttleManager.resetThrottle()
    changeSet.added.forEachIndexed { index, gallery ->
        status.value = FavoritesSyncStatus.Processing.AddingGalleryToLocal(
            index = index + 1,
            total = changeSet.added.size,
            isThrottling = needWarnThrottle(),
            title = gallery.title,
        )
        throttleManager.throttle()
        // Import using gallery adder
        val result = galleryAdder.addGallery(
            context = context,
            url = "${exh.baseUrl}${gallery.getUrl()}",
            fav = true,
            forceSource = exh,
            throttleFunc = throttleManager::throttle,
            retry = 3,
        )
        when (result) {
            // Skip this gallery, it no longer exists
            is GalleryAddEvent.Fail.NotFound -> {
                logger.e(context.stringResource(SYMR.strings.favorites_sync_remote_not_exist, gallery.getUrl()))
            }
            is GalleryAddEvent.Fail -> {
                val error = galleryAddError(result, gallery.title)
                if (exhPreferences.exhLenientSync.get()) {
                    errorList += error
                } else {
                    status.value = error
                    throw FavoritesSyncHelper.IgnoredException(error)
                }
            }
            is GalleryAddEvent.Success -> {
                insertedMangaCategories += categories[gallery.category].id to result.manga
            }
        }
    }
    return insertedMangaCategories
}

private fun galleryAddError(
    result: GalleryAddEvent.Fail,
    title: String,
): FavoritesSyncStatus.SyncError.GallerySyncError {
    return when (result) {
        is GalleryAddEvent.Fail.Error ->
            FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail(title, result.logMessage)
        is GalleryAddEvent.Fail.UnknownType ->
            FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail(title, result.galleryUrl)
        is GalleryAddEvent.Fail.UnknownSource ->
            FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail(title, result.galleryUrl)
    }
}
