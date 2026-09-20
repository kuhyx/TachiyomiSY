package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import exh.source.LIBRARY_UPDATE_EXCLUDED_SOURCES
import exh.source.MERGED_SOURCE_ID
import exh.source.mangaDexSourceIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
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
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

// How many sources update at once, and the periodic-work flex window and retry backoff.
private const val MAX_CONCURRENT_SOURCES = 5
private const val FLEX_MINUTES = 10L
private const val BACKOFF_MINUTES = 10L

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
    private val getMergedMangaForDownloading: GetMergedMangaForDownloading = Injekt.get()
    internal val getTracks: GetTracks = Injekt.get()
    internal val insertTrack: InsertTrack = Injekt.get()
    internal val trackerManager: TrackerManager = Injekt.get()
    internal val mdList = trackerManager.mdList
    // SY <--

    internal val notifier = LibraryUpdateNotifier(context)

    private var mangaToUpdate: List<LibraryManga> = mutableListOf()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val preferences = Injekt.get<LibraryPreferences>()
                val restrictions = preferences.autoUpdateDeviceRestrictions.get()
                if (DEVICE_ONLY_ON_WIFI in restrictions && !context.isConnectedToWifi()) {
                    return Result.retry()
                }
            }

            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                return Result.retry()
            }
        }

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

        return withIOContext {
            try {
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
        }
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

    // Method that updates manga in [mangaToUpdate]. It's called in a background thread, so it's safe
    // to do heavy operations or network calls here.
    // For each manga it calls [updateManga] and updates the notification showing the current
    // progress.
    // @return an observable delivering the progress of each update.
    private suspend fun updateChapterList() {
        val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)
        val run = LibraryUpdateRun(fetchInterval.getWindow(ZonedDateTime.now()))
        // SY -->
        val mdlistLogged = mdList.isLoggedIn
        // SY <--

        coroutineScope {
            mangaToUpdate.groupBy { it.manga.source }
                // SY -->
                .filterNot { it.key in LIBRARY_UPDATE_EXCLUDED_SOURCES }
                // SY <--
                .values
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            // SY -->
                            val isMangaDex = mangaInSource.firstOrNull()?.manga?.source?.let { it in mangaDexSourceIds }
                            if (mdlistLogged && isMangaDex == true) {
                                launch { addInitialMdListTracks(mangaInSource) }
                            }
                            // SY <--
                            mangaInSource.forEach { libraryManga ->
                                ensureActive()
                                updateIfInLibrary(libraryManga.manga, run)
                            }
                        }
                    }
                }
                .awaitAll()
        }
        reportRun(run)
    }

    internal fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        // SY -->
        if (manga.source == MERGED_SOURCE_ID) {
            val downloadingManga = runBlocking { getMergedMangaForDownloading.await(manga.id) }
                .associateBy { it.id }
            chapters.groupBy { it.mangaId }
                .forEach { (mangaId, mangaChapters) ->
                    downloadingManga[mangaId]?.let { downloadManager.downloadChapters(it, mangaChapters, false) }
                }

            return
        }
        // SY <--
        downloadManager.downloadChapters(manga, chapters, false)
    }

    // Updates the chapters for the given manga and adds them to the database.
    // @param manga the manga to update.
    // @return a pair of the inserted and removed chapters.
    internal suspend fun updateManga(manga: Manga, fetchWindow: Pair<Long, Long>): List<Chapter> {
        val source = sourceManager.getOrStub(manga.source)

        val update = updateMangaFromRemote(
            source = source,
            manga = manga,
            fetchDetails = libraryPreferences.autoUpdateMetadata.get(),
            fetchChapters = true,
            fetchWindow = fetchWindow,
        )
            .getOrThrow()

        return if (update.manga.favorite) update.newChapters else emptyList()
    }

    private suspend fun updateCovers() {
        val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()

        coroutineScope {
            mangaToUpdate.groupBy { it.manga.source }
                .values
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            mangaInSource.forEach { libraryManga ->
                                val manga = libraryManga.manga
                                ensureActive()

                                withUpdateNotification(
                                    currentlyUpdatingManga,
                                    progressCount,
                                    manga,
                                ) {
                                    val source = sourceManager.get(manga.source)
                                    if (source != null) {
                                        try {
                                            updateMangaFromRemote(
                                                source,
                                                manga,
                                                fetchDetails = true,
                                                fetchChapters = false,
                                                manualFetch = true,
                                            ).getOrThrow()
                                        } catch (expected: Throwable) {
                                            // Ignore errors and continue
                                            logcat(LogPriority.ERROR, expected)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
    }

    // SY -->

    // SY <--

    internal suspend fun withUpdateNotification(
        updatingManga: CopyOnWriteArrayList<Manga>,
        completed: AtomicInt,
        manga: Manga,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingManga.add(manga)
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )

        block()

        ensureActive()

        updatingManga.remove(manga)
        completed.incrementAndFetch()
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )
    }

    // Writes basic file of update errors to cache dir.
    internal fun writeErrorFile(errors: List<Pair<Manga, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("mihon_update_errors.txt")
                file.bufferedWriter().use { out ->
                    out.write(context.stringResource(MR.strings.library_errors_help, ERROR_LOG_HELP_URL) + "\n\n")
                    // Error file format:
                    // ! Error
                    //   # Source
                    //     - Manga
                    errors.groupBy({ it.second }, { it.first }).forEach { (error, mangas) ->
                        out.write("\n! ${error}\n")
                        mangas.groupBy { it.source }.forEach { (srcId, mangas) ->
                            val source = sourceManager.getOrStub(srcId)
                            out.write("  # $source\n")
                            mangas.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {}
        return File("")
    }

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
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        private const val ERROR_LOG_HELP_URL = "https://mihon.app/docs/guides/troubleshooting/"

        // Key for category to update.
        private const val KEY_CATEGORY = "category"

        // Key that defines what should be updated.
        private const val KEY_TARGET = "target"

        // SY -->

        /**
         * Key for group to update.
         */
        const val KEY_GROUP = "group"
        const val KEY_GROUP_EXTRA = "group_extra"
        // SY <--

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.autoUpdateInterval.get()
            if (interval > 0) {
                val restrictions = preferences.autoUpdateDeviceRestrictions.get()
                val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
                val networkRequest = NetworkRequest.Builder().apply {
                    removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    if (DEVICE_ONLY_ON_WIFI in restrictions) {
                        addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    }
                    if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    }
                }
                    .build()
                val constraints = Constraints.Builder()
                    // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
                    .setRequiredNetworkRequest(networkRequest, networkType)
                    .setRequiresCharging(DEVICE_CHARGING in restrictions)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    FLEX_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_MINUTES, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_AUTO,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }

        fun startNow(
            context: Context,
            category: Category? = null,
            target: Target = Target.CHAPTERS,
            // SY -->
            group: Int = LibraryGroup.BY_DEFAULT,
            groupExtra: String? = null,
            // SY <--
        ): Boolean {
            val wm = context.workManager
            // Check if the LibraryUpdateJob is already running
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }

            val inputData = workDataOf(
                KEY_CATEGORY to category?.id,
                KEY_TARGET to target.name,
                // SY -->
                KEY_GROUP to group,
                KEY_GROUP_EXTRA to groupExtra,
                // SY <--
            )

            val syncPreferences: SyncPreferences = Injekt.get()

            // Always sync the data before library update if syncing is enabled.
            if (syncPreferences.isSyncEnabled()) {
                // Check if SyncDataJob is already running
                if (SyncDataJob.isRunning(context)) {
                    // SyncDataJob is already running
                    return false
                }

                // Define the SyncDataJob
                val syncDataJob = OneTimeWorkRequestBuilder<SyncDataJob>()
                    .addTag(SyncDataJob.TAG_MANUAL)
                    .build()

                // Chain SyncDataJob to run before LibraryUpdateJob
                val libraryUpdateJob = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                    .addTag(TAG)
                    .addTag(WORK_NAME_MANUAL)
                    .setInputData(inputData)
                    .build()

                wm.beginUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, syncDataJob)
                    .then(libraryUpdateJob)
                    .enqueue()
            } else {
                val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                    .addTag(TAG)
                    .addTag(WORK_NAME_MANUAL)
                    .setInputData(inputData)
                    .build()

                wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)
            }

            return true
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}
