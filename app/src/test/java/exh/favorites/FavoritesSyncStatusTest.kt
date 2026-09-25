package exh.favorites

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class FavoritesSyncStatusTest {
    private val json = Json

    private fun roundTrip(status: FavoritesSyncStatus): FavoritesSyncStatus =
        json.decodeFromString<FavoritesSyncStatus>(json.encodeToString<FavoritesSyncStatus>(status))

    @Test
    fun errorsSerialize() {
        roundTrip(FavoritesSyncStatus.SyncError.NotLoggedInSyncError) shouldBe
            FavoritesSyncStatus.SyncError.NotLoggedInSyncError
        roundTrip(FavoritesSyncStatus.SyncError.FailedToFetchFavorites) shouldBe
            FavoritesSyncStatus.SyncError.FailedToFetchFavorites
        roundTrip(FavoritesSyncStatus.SyncError.UnknownSyncError("x")) shouldBe
            FavoritesSyncStatus.SyncError.UnknownSyncError("x")
        roundTrip(FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote("t", "g")) shouldBe
            FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote("t", "g")
        roundTrip(FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote) shouldBe
            FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote
        roundTrip(FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail("t", "r")) shouldBe
            FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail("t", "r")
        roundTrip(FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail("t", "u")) shouldBe
            FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail("t", "u")
    }

    @Test
    fun statesSerialize() {
        roundTrip(FavoritesSyncStatus.Idle) shouldBe FavoritesSyncStatus.Idle
        roundTrip(FavoritesSyncStatus.Initializing) shouldBe FavoritesSyncStatus.Initializing
        val bad = FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories(1, "m", listOf("a", "b"))
        roundTrip(bad) shouldBe bad
        val complete = FavoritesSyncStatus.CompleteWithErrors(
            listOf(FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail("t", "r")),
        )
        roundTrip(complete) shouldBe complete
        json.encodeToString<FavoritesSyncStatus>(FavoritesSyncStatus.Idle) shouldBe
            """{"type":"exh.favorites.FavoritesSyncStatus.Idle"}"""
    }

    @Test
    fun processingStatesCarryProgress() {
        FavoritesSyncStatus.Processing.VerifyingLibrary.shouldBeInstanceOf<FavoritesSyncStatus.Processing>()
        FavoritesSyncStatus.Processing.DownloadingFavorites.toString() shouldBe "DownloadingFavorites"
        FavoritesSyncStatus.Processing.CalculatingRemoteChanges.toString() shouldBe "CalculatingRemoteChanges"
        FavoritesSyncStatus.Processing.CalculatingLocalChanges.toString() shouldBe "CalculatingLocalChanges"
        FavoritesSyncStatus.Processing.SyncingCategoryNames.toString() shouldBe "SyncingCategoryNames"
        FavoritesSyncStatus.Processing.CleaningUp.toString() shouldBe "CleaningUp"
        FavoritesSyncStatus.Processing.RemovingRemoteGalleries(3).galleryCount shouldBe 3
        val remote = FavoritesSyncStatus.Processing.AddingGalleryToRemote(1, 2, isThrottling = true, title = "t")
        remote.copy(index = 2) shouldBe FavoritesSyncStatus.Processing.AddingGalleryToRemote(2, 2, true, "t")
        remote.hashCode() shouldBe remote.copy().hashCode()
        FavoritesSyncStatus.Processing.RemovingGalleryFromLocal(1, 5).total shouldBe 5
        val local = FavoritesSyncStatus.Processing.AddingGalleryToLocal(1, 2, isThrottling = false, title = "t")
        local.toString() shouldBe "AddingGalleryToLocal(index=1, total=2, isThrottling=false, title=t)"
        (local == local.copy(title = "u")) shouldBe false
    }
}
