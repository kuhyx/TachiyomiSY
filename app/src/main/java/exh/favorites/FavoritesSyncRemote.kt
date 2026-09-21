package exh.favorites

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import exh.favorites.FavoritesSyncHelper.IgnoredException
import okhttp3.FormBody
import okhttp3.Request
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

private const val EXH_REQUEST_RETRIES = 10

internal suspend fun FavoritesSyncHelper.applyRemoteCategories(categories: List<String>) {
    val localCategories = getCategories.await()
        .filterNot(Category::isSystemCategory)

    categories.forEachIndexed { index, remote ->
        val local = localCategories.getOrElse(index) {
            when (val createCategoryWithNameResult = createCategoryWithName.await(remote)) {
                is CreateCategoryWithName.Result.InternalError -> throw createCategoryWithNameResult.error
                is CreateCategoryWithName.Result.Success -> createCategoryWithNameResult.category
            }
        }

        // Ensure consistent ordering and naming
        if (local.name != remote || local.order != index.toLong()) {
            val result = updateCategory.await(
                CategoryUpdate(
                    id = local.id,
                    order = index.toLong().takeIf { it != local.order },
                    name = remote.takeIf { it != local.name },
                ),
            )
            if (result is UpdateCategory.Result.Error) {
                throw result.error
            }
        }
    }
}

internal suspend fun FavoritesSyncHelper.addGalleryRemote(
    errorList: MutableList<FavoritesSyncStatus.SyncError.GallerySyncError>,
    gallery: FavoriteEntry,
) {
    val url = "${exh.baseUrl}/gallerypopups.php?gid=${gallery.gid}&t=${gallery.token}&act=addfav"

    val request = POST(
        url = url,
        body = FormBody.Builder()
            .add("favcat", gallery.category.toString())
            .add("favnote", "")
            .add("apply", "Add to Favorites")
            .add("update", "1")
            .build(),
    )

    if (!explicitlyRetryExhRequest(EXH_REQUEST_RETRIES, request)) {
        val error = FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote(
            gallery.title,
            gallery.gid,
        )

        if (exhPreferences.exhLenientSync.get()) {
            errorList += error
        } else {
            status.value = error
            throw IgnoredException(error)
        }
    }
}

internal suspend fun FavoritesSyncHelper.explicitlyRetryExhRequest(retryCount: Int, request: Request): Boolean {
    var success = false

    repeat(retryCount) {
        if (!success) {
            try {
                val resp = withIOContext { exh.client.newCall(request).await() }
                success = resp.isSuccessful
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logger.w(context.stringResource(SYMR.strings.favorites_sync_network_error), expected)
            }
        }
    }

    return success
}

internal suspend fun FavoritesSyncHelper.applyChangeSetToRemote(
    errorList: MutableList<FavoritesSyncStatus.SyncError.GallerySyncError>,
    changeSet: ChangeSet,
) {
    // Apply removals
    if (changeSet.removed.isNotEmpty()) {
        status.value = FavoritesSyncStatus.Processing.RemovingRemoteGalleries(changeSet.removed.size)

        val formBody = FormBody.Builder()
            .add("ddact", "delete")
            .add("apply", "Apply")

        // Add change set to form
        changeSet.removed.forEach {
            formBody.add("modifygids[]", it.gid)
        }

        val request = POST(
            url = "https://exhentai.org/favorites.php",
            body = formBody.build(),
        )

        if (!explicitlyRetryExhRequest(EXH_REQUEST_RETRIES, request)) {
            if (exhPreferences.exhLenientSync.get()) {
                errorList += FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote
            } else {
                status.value = FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote
                throw IgnoredException(FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote)
            }
        }
    }

    // Apply additions
    throttleManager.resetThrottle()
    changeSet.added.forEachIndexed { index, gallery ->
        status.value = FavoritesSyncStatus.Processing.AddingGalleryToRemote(
            index = index + 1,
            total = changeSet.added.size,
            isThrottling = needWarnThrottle(),
            title = gallery.title,
        )

        throttleManager.throttle()

        addGalleryRemote(errorList, gallery)
    }
}
