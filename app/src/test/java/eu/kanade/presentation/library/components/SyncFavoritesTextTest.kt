package eu.kanade.presentation.library.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import exh.favorites.FavoritesSyncStatus
import exh.favorites.FavoritesSyncStatus.Processing
import exh.favorites.FavoritesSyncStatus.SyncError
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SyncFavoritesTextTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val calls = mutableListOf<String>()

    private fun props(status: FavoritesSyncStatus) =
        context.syncProperties(status, setStatusIdle = { calls += "idle" }, openManga = { calls += "open $it" })

    @Test
    fun idleShowsNothing() {
        props(FavoritesSyncStatus.Idle).shouldBeNull()
    }

    @Test
    fun badLibraryOffersTheGallery() {
        val props = props(FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories(7L, "Title", listOf("a", "b")))
            .shouldNotBeNull()
        props.text shouldContain "Title"
        props.positiveButton?.invoke()
        props.negativeButton?.invoke()
        calls shouldBe listOf("open 7", "idle", "idle")
    }

    @Test
    fun completeWithErrorsListsThem() {
        val errors = listOf(
            SyncError.GallerySyncError.GalleryAddFail("A", "why"),
            SyncError.GallerySyncError.InvalidGalleryFail("B", "url"),
            SyncError.GallerySyncError.UnableToAddGalleryToRemote("C", "gid"),
            SyncError.GallerySyncError.UnableToDeleteFromRemote,
        )
        val props = props(FavoritesSyncStatus.CompleteWithErrors(errors)).shouldNotBeNull()
        props.text shouldContain "why"
        props.text shouldContain "gid"
        props.positiveButton?.invoke()
        calls shouldBe listOf("idle")
    }

    @Test
    fun initializingHasNoButtons() {
        props(FavoritesSyncStatus.Initializing).shouldNotBeNull().positiveButton.shouldBeNull()
    }

    @Test
    fun syncErrorsExplainThemselves() {
        listOf(
            SyncError.NotLoggedInSyncError,
            SyncError.FailedToFetchFavorites,
            SyncError.UnknownSyncError("mystery"),
            SyncError.GallerySyncError.UnableToDeleteFromRemote,
        ).map { props(it).shouldNotBeNull().text }.last() shouldBe
            props(SyncError.GallerySyncError.UnableToDeleteFromRemote)?.text
        props(SyncError.UnknownSyncError("mystery"))?.text.orEmpty() shouldContain "mystery"
    }

    @Test
    fun processingStepsHaveText() {
        val steps = listOf(
            Processing.VerifyingLibrary,
            Processing.DownloadingFavorites,
            Processing.CalculatingRemoteChanges,
            Processing.CalculatingLocalChanges,
            Processing.SyncingCategoryNames,
            Processing.RemovingRemoteGalleries(3),
            Processing.AddingGalleryToRemote(index = 1, total = 2, isThrottling = true, title = "R"),
            Processing.AddingGalleryToRemote(index = 1, total = 2, isThrottling = false, title = "R"),
            Processing.RemovingGalleryFromLocal(index = 1, total = 2),
            Processing.AddingGalleryToLocal(index = 1, total = 2, isThrottling = true, title = "L"),
            Processing.AddingGalleryToLocal(index = 1, total = 2, isThrottling = false, title = "L"),
            Processing.CleaningUp,
        )
        steps.map { props(it).shouldNotBeNull().text }.toSet().size shouldBe steps.size
    }

    @Test
    fun onlyAddingStepsNameTheGallery() {
        val remote = Processing.AddingGalleryToRemote(index = 1, total = 2, isThrottling = false, title = "R")
        val local = Processing.AddingGalleryToLocal(index = 1, total = 2, isThrottling = false, title = "L")
        remote.slowGalleryTitle() shouldBe "R"
        local.slowGalleryTitle() shouldBe "L"
        Processing.CleaningUp.slowGalleryTitle().shouldBeNull()
    }
}
