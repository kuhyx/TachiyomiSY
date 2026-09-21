package exh.favorites

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.fetchFavorites
import eu.kanade.tachiyomi.util.system.toast
import exh.GalleryAdder
import exh.eh.EHentaiUpdateWorker
import exh.eh.cancelBackground
import exh.eh.scheduleBackground
import exh.log.xLog
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import exh.source.isEhBasedManga
import exh.util.ThrottleManager
import exh.util.createPartialWakeLock
import exh.util.createWifiLock
import exh.util.ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration.Companion.seconds

// Follow-up: only apply database changes after sync (https://github.com/kuhyx/TachiyomiSY/issues/24)
internal class FavoritesSyncHelper(val context: Context) {
    private val getLibraryManga: GetLibraryManga by injectLazy()
    internal val getCategories: GetCategories by injectLazy()
    internal val getManga: GetManga by injectLazy()
    internal val updateManga: UpdateManga by injectLazy()
    internal val setMangaCategories: SetMangaCategories by injectLazy()
    internal val createCategoryWithName: CreateCategoryWithName by injectLazy()
    internal val updateCategory: UpdateCategory by injectLazy()

    internal val exhPreferences: ExhPreferences by injectLazy()

    internal val exh by lazy {
        Injekt.get<SourceManager>().get(EXH_SOURCE_ID) as? EHentai
            ?: EHentai(0, true, context)
    }

    private val storage by lazy { LocalFavoritesStorage() }

    internal val galleryAdder by lazy { GalleryAdder() }

    internal val throttleManager by lazy { ThrottleManager() }

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    internal val logger by lazy { xLog() }

    val status: MutableStateFlow<FavoritesSyncStatus> = MutableStateFlow(FavoritesSyncStatus.Idle)

    @Synchronized
    fun runSync(scope: CoroutineScope) {
        if (status.value !is FavoritesSyncStatus.Idle) {
            return
        }

        status.value = FavoritesSyncStatus.Initializing

        scope.launch(Dispatchers.IO) { beginSync() }
    }

    private suspend fun beginSync() {
        // Check if logged in
        if (!exhPreferences.enableExhentai.get()) {
            status.value = FavoritesSyncStatus.SyncError.NotLoggedInSyncError
            return
        }
        // Validate library state
        status.value = FavoritesSyncStatus.Processing.VerifyingLibrary
        val favorites = if (libraryStateIsSyncable()) downloadFavorites() else null

        val errorList = mutableListOf<FavoritesSyncStatus.SyncError.GallerySyncError>()
        // A failed step has already set the error status.
        val completed = favorites != null && syncUnderLocks(favorites, errorList)
        if (completed) {
            status.value = if (errorList.isEmpty()) {
                FavoritesSyncStatus.Idle
            } else {
                FavoritesSyncStatus.CompleteWithErrors(errorList)
            }
        }
    }

    // The remote favorites, or null (with [status] set to the error) when they could not be fetched.
    private suspend fun downloadFavorites(): Pair<List<EHentai.ParsedManga>, List<String>>? = try {
        status.value = FavoritesSyncStatus.Processing.DownloadingFavorites
        exh.fetchFavorites()
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        status.value = FavoritesSyncStatus.SyncError.FailedToFetchFavorites
        logger.e(context.stringResource(SYMR.strings.favorites_sync_could_not_fetch), expected)
        null
    }

    // An EH gallery in more than one category cannot be mirrored to a single remote favourite slot.
    private suspend fun libraryStateIsSyncable(): Boolean {
        val libraryManga = getLibraryManga.await()
        val seenManga = HashSet<Long>(libraryManga.size)
        for ((manga) in libraryManga) {
            if (!manga.isEhBasedManga()) continue
            if (manga.id in seenManga) {
                val inCategories = getCategories.await(manga.id)
                status.value = FavoritesSyncStatus.BadLibraryState
                    .MangaInMultipleCategories(manga.id, manga.title, inCategories.map { it.name })
                logger.w(
                    context.stringResource(SYMR.strings.favorites_sync_gallery_multiple_categories_error, manga.id),
                )
                return false
            }
            seenManga += manga.id
        }
        return true
    }

    // Runs the exchange with the wake and wifi locks held and background gallery updates paused.
    // @return false when it failed, with [status] already set to the error.
    private suspend fun syncUnderLocks(
        favorites: Pair<List<EHentai.ParsedManga>, List<String>>,
        errorList: MutableList<FavoritesSyncStatus.SyncError.GallerySyncError>,
    ): Boolean {
        return try {
            // Take wake + wifi locks
            ignore { wakeLock?.release() }
            wakeLock = ignore { context.createPartialWakeLock("teh:ExhFavoritesSyncWakelock") }
            ignore { wifiLock?.release() }
            wifiLock = ignore { context.createWifiLock("teh:ExhFavoritesSyncWifi") }

            // Do not update galleries while syncing favorites
            EHentaiUpdateWorker.cancelBackground(context)

            status.value = FavoritesSyncStatus.Processing.CalculatingRemoteChanges
            val remoteChanges = storage.getChangedRemoteEntries(favorites.first)
            val localChanges = if (exhPreferences.exhReadOnlySync.get()) {
                null // Do not build local changes if they are not going to be applied
            } else {
                status.value = FavoritesSyncStatus.Processing.CalculatingLocalChanges
                storage.getChangedDbEntries()
            }

            // Apply remote categories
            status.value = FavoritesSyncStatus.Processing.SyncingCategoryNames
            applyRemoteCategories(favorites.second)

            // Apply change sets
            applyChangeSetToLocal(errorList, remoteChanges)
            if (localChanges != null) {
                applyChangeSetToRemote(errorList, localChanges)
            }

            status.value = FavoritesSyncStatus.Processing.CleaningUp
            storage.snapshotEntries()

            withUIContext {
                context.toast(SYMR.strings.favorites_sync_complete)
            }
            true
        } catch (e: IgnoredException) {
            // Do not display error as this error has already been reported
            logger.w(context.stringResource(SYMR.strings.favorites_sync_ignoring_exception), e)
            false
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            status.value = FavoritesSyncStatus.SyncError.UnknownSyncError(expected.message.orEmpty())
            logger.e(context.stringResource(SYMR.strings.favorites_sync_sync_error), expected)
            false
        } finally {
            // Release wake + wifi locks
            ignore {
                wakeLock?.release()
                wakeLock = null
            }
            ignore {
                wifiLock?.release()
                wifiLock = null
            }
            // Update galleries again!
            EHentaiUpdateWorker.scheduleBackground(context)
        }
    }

    internal fun needWarnThrottle() =
        throttleManager.throttleTime >= THROTTLE_WARN

    class IgnoredException(
        message: FavoritesSyncStatus.SyncError.GallerySyncError,
    ) : RuntimeException(message.toString())

    companion object {
        private val THROTTLE_WARN = 1.seconds
    }
}
