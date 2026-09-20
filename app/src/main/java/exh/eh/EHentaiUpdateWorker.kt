package exh.eh

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.library.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION
import exh.log.xLog
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.base.raise
import exh.source.ExhPreferences
import exh.util.cancellable
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

private const val FLEX_MINUTES = 10L

internal class EHentaiUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val exhPreferences: ExhPreferences by injectLazy()
    internal val libraryPreferences: LibraryPreferences by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    internal val updateHelper: EHentaiUpdateHelper by injectLazy()
    internal val logger: Logger by lazy { xLog() }
    private val updateMangaFromRemote: UpdateMangaFromRemote by injectLazy()
    private val getChaptersByMangaId: GetChaptersByMangaId by injectLazy()
    private val getFlatMetadataById: GetFlatMetadataById by injectLazy()
    private val insertFlatMetadata: InsertFlatMetadata by injectLazy()
    private val getExhFavoriteMangaWithMetadata: GetExhFavoriteMangaWithMetadata by injectLazy()

    internal val updateNotifier by lazy { EHentaiUpdateNotifier(context) }
    private val libraryUpdateNotifier by lazy { LibraryUpdateNotifier(context) }

    override suspend fun doWork(): Result {
        return try {
            if (requiresWifiConnection(exhPreferences) && !context.isConnectedToWifi()) {
                Result.success() // retry again later
            } else {
                setForegroundSafely()
                startUpdating()
                logger.d("Update job completed!")
                Result.success()
            }
        } catch (_: Exception) {
            // Any failure ends here and the fallback below applies.
            Result.success() // retry again later
        } finally {
            updateNotifier.cancelProgressNotification()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_EHENTAI_PROGRESS,
            updateNotifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun startUpdating() {
        logger.d("Update job started!")
        val startTime = System.currentTimeMillis()

        logger.d("Finding manga with metadata...")
        val metadataManga = getExhFavoriteMangaWithMetadata.await()

        logger.d("Filtering manga and raising metadata...")
        val allMeta = collectUpdateEntries(metadataManga)

        logger.d("Found %s manga to update, starting updates!", allMeta.size)
        val mangaMetaToUpdateThisIter = allMeta.take(UPDATES_PER_ITERATION)

        val iteration = UpdateIteration(mangaMetaToUpdateThisIter.size)
        try {
            for ((index, entry) in mangaMetaToUpdateThisIter.withIndex()) {
                if (iteration.failures > MAX_UPDATE_FAILURES) {
                    logger.w("Too many update failures, aborting...")
                    break
                }
                iteration.updateGallery(index, entry)
            }
        } finally {
            exhPreferences.exhAutoUpdateStats.set(
                Json.encodeToString(
                    EHentaiUpdaterStats(
                        startTime,
                        allMeta.size,
                        iteration.updated,
                    ),
                ),
            )

            updateNotifier.cancelProgressNotification()
            if (iteration.updatedManga.isNotEmpty()) {
                libraryUpdateNotifier.showUpdateNotifications(iteration.updatedManga)
            }
        }
    }

    // Galleries due for a check, oldest check first; recently checked ones are skipped.
    private suspend fun collectUpdateEntries(metadataManga: List<Manga>): List<UpdateEntry> {
        val curTime = System.currentTimeMillis()
        return metadataManga.asFlow().cancellable().mapNotNull { manga ->
            val meta = getFlatMetadataById.await(manga.id)
                ?: return@mapNotNull null

            val raisedMeta = meta.raise(EHentaiSearchMetadata::class)

            // Don't update galleries too frequently
            val checkedRecently = curTime - raisedMeta.lastUpdateCheck < MIN_BACKGROUND_UPDATE_FREQ &&
                DebugToggles.RESTRICT_EXH_GALLERY_UPDATE_CHECK_FREQUENCY.enabled
            if (raisedMeta.aged || checkedRecently) {
                return@mapNotNull null
            }

            val chapter = getChaptersByMangaId.await(manga.id).minByOrNull {
                it.dateUpload
            }

            UpdateEntry(manga, raisedMeta, chapter)
        }.toList().sortedBy { it.meta.lastUpdateCheck }
    }

    // Mutable bookkeeping for one updater run.
    private inner class UpdateIteration(private val total: Int) {
        var failures: Int = 0
        var updated: Int = 0
        val updatedManga: MutableList<Pair<Manga, Array<Chapter>>> = mutableListOf()
        private val modified = mutableSetOf<Long>()

        suspend fun updateGallery(index: Int, entry: UpdateEntry) {
            val (manga, meta) = entry
            logger.d(
                "Updating gallery (index: %s, manga.id: %s, meta.gId: %s, meta.gToken: %s, " +
                    "failures-so-far: %s, modifiedThisIteration.size: %s)...",
                index,
                manga.id,
                meta.gId,
                meta.gToken,
                failures,
                modified.size,
            )

            if (manga.id in modified) {
                // We already processed this manga!
                logger.w("Gallery already updated this iteration, skipping...")
                updated++
                return
            }

            val (new, chapters) = fetchChapters(manga, meta) ?: return
            if (chapters.isEmpty()) {
                logger.e(
                    "No chapters found for gallery (manga.id: %s, meta.gId: %s, meta.gToken: %s, " +
                        "failures-so-far: %s)!",
                    manga.id,
                    meta.gId,
                    meta.gToken,
                    failures,
                )
                return
            }

            // Find accepted root and discard others
            val (acceptedRoot, discardedRoots, exhNew) =
                updateHelper.acceptRootAndDiscardOthers(manga.source, chapters)

            if (new.isNotEmpty() && manga.id == acceptedRoot.manga.id) {
                libraryPreferences.newUpdatesCount.getAndSet { it + new.size }
                updatedManga += acceptedRoot.manga to new.toTypedArray()
            } else if (exhNew.isNotEmpty() && updatedManga.none { it.first.id == acceptedRoot.manga.id }) {
                libraryPreferences.newUpdatesCount.getAndSet { it + exhNew.size }
                updatedManga += acceptedRoot.manga to exhNew.toTypedArray()
            }

            modified += acceptedRoot.manga.id
            modified += discardedRoots.map { it.manga.id }
            updated++
        }

        // (new, current) chapters, or null when the gallery could not be updated; network failures count.
        private suspend fun fetchChapters(
            manga: Manga,
            meta: EHentaiSearchMetadata,
        ): Pair<List<Chapter>, List<Chapter>>? =
            try {
                updateNotifier.showProgressNotification(manga, updated + failures, total)
                updateEntryAndGetChapters(manga)
            } catch (e: GalleryNotUpdatedException) {
                if (e.network) {
                    failures++

                    logger.e("> Network error while updating gallery!", e)
                    logger.e(
                        "> (manga.id: %s, meta.gId: %s, meta.gToken: %s, failures-so-far: %s)",
                        manga.id,
                        meta.gId,
                        meta.gToken,
                        failures,
                    )
                }
                null
            }
    }

    // New, current
    internal suspend fun updateEntryAndGetChapters(manga: Manga): Pair<List<Chapter>, List<Chapter>> {
        val source = ehSourceOf(manga)
        try {
            val result = updateMangaFromRemote(
                source,
                manga,
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
            ).getOrThrow()
            return result.newChapters to getChaptersByMangaId.await(manga.id)
        } catch (notFound: EHentai.GalleryNotFoundException) {
            ageDeadGallery(manga)
            throw GalleryNotUpdatedException(false, notFound)
        } catch (expected: Throwable) {
            // Wrapped whatever the cause; the worker decides whether to retry.
            throw GalleryNotUpdatedException(true, expected)
        }
    }

    private fun ehSourceOf(manga: Manga): EHentai = sourceManager.get(manga.source) as? EHentai
        ?: throw GalleryNotUpdatedException(false, IllegalStateException("Missing EH-based source (${manga.source})!"))

    // A gallery the site no longer has is marked aged so the updater stops asking for it.
    private suspend fun ageDeadGallery(manga: Manga) {
        val meta = getFlatMetadataById.await(manga.id)?.raise(EHentaiSearchMetadata::class) ?: return
        logger.d("Aged %s - notfound", manga.id)
        meta.aged = true
        insertFlatMetadata.await(meta)
    }

    fun requiresWifiConnection(exhPreferences: ExhPreferences): Boolean {
        val restrictions = exhPreferences.exhAutoUpdateRequirements.get()
        return DEVICE_ONLY_ON_WIFI in restrictions
    }

    companion object {
        private const val MAX_UPDATE_FAILURES = 5

        private val MIN_BACKGROUND_UPDATE_FREQ = 1.days.inWholeMilliseconds

        private const val TAG = "EHBackgroundUpdater"

        private val logger by lazy { XLog.tag("EHUpdaterScheduler") }

        fun launchBackgroundTest(context: Context) {
            context.workManager.enqueue(
                OneTimeWorkRequestBuilder<EHentaiUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag(TAG)
                    .build(),
            )
        }

        fun scheduleBackground(context: Context, prefInterval: Int? = null, prefRestrictions: Set<String>? = null) {
            val exhPreferences = Injekt.get<ExhPreferences>()
            val interval = prefInterval ?: exhPreferences.exhAutoUpdateFrequency.get()
            if (interval > 0) {
                val restrictions = prefRestrictions ?: exhPreferences.exhAutoUpdateRequirements.get()
                val acRestriction = DEVICE_CHARGING in restrictions

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(acRestriction)
                    .build()

                val request = PeriodicWorkRequestBuilder<EHentaiUpdateWorker>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    FLEX_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
                logger.d("Successfully scheduled background update job!")
            } else {
                cancelBackground(context)
            }
        }

        fun cancelBackground(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }
    }
}

internal data class UpdateEntry(val manga: Manga, val meta: EHentaiSearchMetadata, val rootChapter: Chapter?)

internal object EHentaiUpdateWorkerConstants {
    const val UPDATES_PER_ITERATION = 50

    val GALLERY_AGE_TIME = 365.days.inWholeMilliseconds
}
