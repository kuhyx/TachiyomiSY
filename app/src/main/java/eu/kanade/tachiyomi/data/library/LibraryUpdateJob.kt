package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaForDownloading
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// How many sources update at once, and the periodic-work flex window and retry backoff.

@OptIn(ExperimentalAtomicApi::class)
internal class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    internal val sourceManager: SourceManager = Injekt.get()
    internal val libraryPreferences: LibraryPreferences = Injekt.get()
    internal val downloadManager: DownloadManager = Injekt.get()
    internal val getLibraryManga: GetLibraryManga = Injekt.get()
    internal val getManga: GetManga = Injekt.get()
    internal val fetchInterval: FetchInterval = Injekt.get()
    internal val filterChaptersForDownload: FilterChaptersForDownload = Injekt.get()
    internal val updateManga: UpdateManga = Injekt.get()
    internal val updateMangaFromRemote: UpdateMangaFromRemote = Injekt.get()

    // SY -->
    internal val getFavorites: GetFavorites = Injekt.get()
    internal val insertFlatMetadata: InsertFlatMetadata = Injekt.get()
    internal val networkToLocalManga: NetworkToLocalManga = Injekt.get()
    internal val getMergedMangaForDownloading: GetMergedMangaForDownloading = Injekt.get()
    internal val getTracks: GetTracks = Injekt.get()
    internal val insertTrack: InsertTrack = Injekt.get()
    internal val trackerManager: TrackerManager = Injekt.get()
    internal val mdList = trackerManager.mdList
    // SY <--

    internal val notifier = LibraryUpdateNotifier(context)

    internal var mangaToUpdate: List<LibraryManga> = mutableListOf()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO) && !canRunAutoNow()) return Result.retry()

        setForegroundSafely()

        val target = inputData.getString(KEY_TARGET)?.let { Target.valueOf(it) }
            ?: Target.CHAPTERS

        // If this is a chapter update, set the last update time to now
        if (target == Target.CHAPTERS) {
            libraryPreferences.lastUpdatedTimestamp.set(Instant.now().toEpochMilli())
        }

        val categoryId = inputData.getLong(KEY_CATEGORY, -1L)
        // SY -->
        val group = inputData.getInt(KEY_GROUP, LibraryGroup.BY_DEFAULT)
        val groupExtra = inputData.getString(KEY_GROUP_EXTRA)
        // SY <--
        addMangaToQueue(categoryId, group, groupExtra)

        return withIOContext { run(target) }
    }

    // An automatic run honours the wifi-only restriction (pre-P, where WorkManager cannot) and yields to a manual one.
    private fun canRunAutoNow(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val restrictions = Injekt.get<LibraryPreferences>().autoUpdateDeviceRestrictions.get()
            if (DEVICE_ONLY_ON_WIFI in restrictions && !context.isConnectedToWifi()) return false
        }
        return !context.workManager.isRunning(WORK_NAME_MANUAL)
    }

    private suspend fun run(target: Target): Result = try {
        when (target) {
            Target.CHAPTERS -> updateChapterList()
            Target.COVERS -> updateCovers()
            // SY -->
            Target.SYNC_FOLLOWS -> syncFollows()
            Target.PUSH_FAVORITES -> pushFavorites()
            // SY <--
        }
        Result.success()
    } catch (_: CancellationException) {
        // Assume success although cancelled
        Result.success()
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected)
        Result.failure()
    } finally {
        notifier.cancelProgressNotification()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notifier = LibraryUpdateNotifier(context)
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    // Adds list of manga to be updated.
    // @param categoryId the ID of the category to update, or -1 if no category specified.
    // SY --> whether this run covers every category instead of a single group
    private suspend fun addMangaToQueue(categoryId: Long, group: Int, groupExtra: String?) {
        val listToUpdate = selectMangaToUpdate(categoryId, group, groupExtra)
        val skippedUpdates = mutableListOf<Pair<Manga, String?>>()
        mangaToUpdate = applyUpdateRestrictions(listToUpdate, skippedUpdates)
        notifier.showQueueSizeWarningIfNeeded(mangaToUpdate)

        if (skippedUpdates.isNotEmpty()) {
            // Follow-up: surface skipped reasons to user? (https://github.com/kuhyx/TachiyomiSY/issues/16)
            logcat {
                skippedUpdates
                    .groupBy { it.second }
                    .map { (reason, entries) -> "$reason: [${entries.map { it.first.title }.sorted().joinToString()}]" }
                    .joinToString()
            }
        }
    }

    // SY -->

    // SY <--

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga chapters
        COVERS, // Manga covers

        // SY -->
        SYNC_FOLLOWS, // MangaDex specific, pull mangadex manga in reading, rereading

        PUSH_FAVORITES, // MangaDex specific, push mangadex manga to mangadex
        // SY <--
    }

    companion object {
        internal const val TAG = "LibraryUpdate"
        internal const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        internal const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        const val ERROR_LOG_HELP_URL = "https://mihon.app/docs/guides/troubleshooting/"

        // Key for category to update.
        internal const val KEY_CATEGORY = "category"

        // Key that defines what should be updated.
        internal const val KEY_TARGET = "target"

        // SY -->

        /**
         * Key for group to update.
         */
        const val KEY_GROUP = "group"
        const val KEY_GROUP_EXTRA = "group_extra"
        // SY <--
    }
}
