package eu.kanade.presentation.library.components

import android.content.Context
import exh.favorites.FavoritesSyncStatus
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

// What the favourites-sync progress dialog says for each status; pure text so the composable stays a shell.

internal fun Context.syncProperties(
    status: FavoritesSyncStatus,
    setStatusIdle: () -> Unit,
    openManga: (Long) -> Unit,
): SyncFavoritesProgressProperties? = when (status) {
    is FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories -> SyncFavoritesProgressProperties(
        title = stringResource(SYMR.strings.favorites_sync_error),
        text = stringResource(
            SYMR.strings.favorites_sync_bad_library_state,
            stringResource(
                SYMR.strings.favorites_sync_gallery_in_multiple_categories,
                status.mangaTitle,
                status.categories.joinToString(),
            ),
        ),
        positiveButtonText = stringResource(SYMR.strings.show_gallery),
        positiveButton = {
            openManga(status.mangaId)
            setStatusIdle()
        },
        negativeButtonText = stringResource(MR.strings.action_ok),
        negativeButton = setStatusIdle,
    )
    is FavoritesSyncStatus.CompleteWithErrors -> SyncFavoritesProgressProperties(
        title = stringResource(SYMR.strings.favorites_sync_done_errors),
        text = stringResource(
            SYMR.strings.favorites_sync_done_errors_message,
            status.messages.joinToString(separator = "\n") { galleryErrorText(it) },
        ),
        positiveButtonText = stringResource(MR.strings.action_ok),
        positiveButton = setStatusIdle,
    )
    is FavoritesSyncStatus.Idle -> null
    is FavoritesSyncStatus.Initializing -> SyncFavoritesProgressProperties(
        title = stringResource(SYMR.strings.favorites_syncing),
        text = stringResource(SYMR.strings.favorites_sync_initializing),
    )
    is FavoritesSyncStatus.SyncError -> SyncFavoritesProgressProperties(
        title = stringResource(SYMR.strings.favorites_sync_error),
        text = stringResource(SYMR.strings.favorites_sync_error_string, syncErrorText(status)),
        positiveButtonText = stringResource(MR.strings.action_ok),
        positiveButton = setStatusIdle,
    )
    is FavoritesSyncStatus.Processing -> SyncFavoritesProgressProperties(
        title = stringResource(SYMR.strings.favorites_syncing),
        text = processingText(status),
    )
}

private fun Context.syncErrorText(status: FavoritesSyncStatus.SyncError): String = when (status) {
    FavoritesSyncStatus.SyncError.NotLoggedInSyncError -> stringResource(SYMR.strings.please_login)
    FavoritesSyncStatus.SyncError.FailedToFetchFavorites -> stringResource(SYMR.strings.favorites_sync_failed_to_featch)
    is FavoritesSyncStatus.SyncError.UnknownSyncError ->
        stringResource(SYMR.strings.favorites_sync_unknown_error, status.message)
    is FavoritesSyncStatus.SyncError.GallerySyncError -> galleryErrorText(status)
}

internal fun Context.galleryErrorText(error: FavoritesSyncStatus.SyncError.GallerySyncError): String = when (error) {
    is FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail ->
        stringResource(SYMR.strings.favorites_sync_failed_to_add_to_local) +
            stringResource(SYMR.strings.favorites_sync_failed_to_add_to_local_error, error.title, error.reason)
    is FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail ->
        stringResource(SYMR.strings.favorites_sync_failed_to_add_to_local) +
            stringResource(SYMR.strings.favorites_sync_failed_to_add_to_local_unknown_type, error.title, error.url)
    is FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote ->
        stringResource(SYMR.strings.favorites_sync_unable_to_add_to_remote, error.title, error.gid)
    FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote ->
        stringResource(SYMR.strings.favorites_sync_unable_to_delete)
}

private fun Context.processingText(status: FavoritesSyncStatus.Processing): String = when (status) {
    FavoritesSyncStatus.Processing.VerifyingLibrary -> stringResource(SYMR.strings.favorites_sync_verifying_library)
    FavoritesSyncStatus.Processing.DownloadingFavorites -> stringResource(SYMR.strings.favorites_sync_downloading)
    FavoritesSyncStatus.Processing.CalculatingRemoteChanges ->
        stringResource(SYMR.strings.favorites_sync_calculating_remote_changes)
    FavoritesSyncStatus.Processing.CalculatingLocalChanges ->
        stringResource(SYMR.strings.favorites_sync_calculating_local_changes)
    FavoritesSyncStatus.Processing.SyncingCategoryNames ->
        stringResource(SYMR.strings.favorites_sync_syncing_category_names)
    is FavoritesSyncStatus.Processing.RemovingRemoteGalleries ->
        stringResource(SYMR.strings.favorites_sync_removing_galleries, status.galleryCount)
    is FavoritesSyncStatus.Processing.AddingGalleryToRemote -> throttled(
        status.isThrottling,
        stringResource(SYMR.strings.favorites_sync_adding_to_remote, status.index, status.total),
    )
    is FavoritesSyncStatus.Processing.RemovingGalleryFromLocal ->
        stringResource(SYMR.strings.favorites_sync_remove_from_local, status.index, status.total)
    is FavoritesSyncStatus.Processing.AddingGalleryToLocal -> throttled(
        status.isThrottling,
        stringResource(SYMR.strings.favorites_sync_add_to_local, status.index, status.total),
    )
    FavoritesSyncStatus.Processing.CleaningUp -> stringResource(SYMR.strings.favorites_sync_cleaning_up)
}

private fun Context.throttled(isThrottling: Boolean, text: String): String =
    if (isThrottling) stringResource(SYMR.strings.favorites_sync_processing_throttle, text) else text

// The gallery being added is appended after a few seconds so a stuck entry can be identified.
internal fun FavoritesSyncStatus.Processing.slowGalleryTitle(): String? = when (this) {
    is FavoritesSyncStatus.Processing.AddingGalleryToRemote -> title
    is FavoritesSyncStatus.Processing.AddingGalleryToLocal -> title
    else -> null
}
