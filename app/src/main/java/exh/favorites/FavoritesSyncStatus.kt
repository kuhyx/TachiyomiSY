package exh.favorites

import kotlinx.serialization.Serializable
import uy.kohesive.injekt.api.get

@Serializable
internal sealed class FavoritesSyncStatus {
    @Serializable
    sealed class SyncError : FavoritesSyncStatus() {
        @Serializable
        data object NotLoggedInSyncError : SyncError()

        @Serializable
        data object FailedToFetchFavorites : SyncError()

        @Serializable
        data class UnknownSyncError(val message: String) : SyncError()

        @Serializable
        sealed class GallerySyncError : SyncError() {
            @Serializable
            data class UnableToAddGalleryToRemote(val title: String, val gid: String) : GallerySyncError()

            @Serializable
            data object UnableToDeleteFromRemote : GallerySyncError()

            @Serializable
            data class GalleryAddFail(val title: String, val reason: String) : GallerySyncError()

            @Serializable
            data class InvalidGalleryFail(val title: String, val url: String) : GallerySyncError()
        }
    }

    @Serializable
    data object Idle : FavoritesSyncStatus()

    @Serializable
    sealed class BadLibraryState : FavoritesSyncStatus() {
        @Serializable
        data class MangaInMultipleCategories(
            val mangaId: Long,
            val mangaTitle: String,
            val categories: List<String>,
        ) : BadLibraryState()
    }

    @Serializable
    data object Initializing : FavoritesSyncStatus()

    @Serializable
    sealed class Processing : FavoritesSyncStatus() {
        data object VerifyingLibrary : Processing()
        data object DownloadingFavorites : Processing()
        data object CalculatingRemoteChanges : Processing()
        data object CalculatingLocalChanges : Processing()
        data object SyncingCategoryNames : Processing()
        data class RemovingRemoteGalleries(val galleryCount: Int) : Processing()
        data class AddingGalleryToRemote(
            val index: Int,
            val total: Int,
            val isThrottling: Boolean,
            val title: String,
        ) : Processing()
        data class RemovingGalleryFromLocal(
            val index: Int,
            val total: Int,
        ) : Processing()
        data class AddingGalleryToLocal(
            val index: Int,
            val total: Int,
            val isThrottling: Boolean,
            val title: String,
        ) : Processing()
        data object CleaningUp : Processing()
    }

    @Serializable
    data class CompleteWithErrors(val messages: List<SyncError.GallerySyncError>) : FavoritesSyncStatus()
}
