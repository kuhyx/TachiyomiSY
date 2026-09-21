package eu.kanade.tachiyomi.data.download

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows down the app. The cache is invalidated by the time
 * defined by the renew interval as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 */
internal class DownloadCache(
    private val context: Context,
    internal val provider: DownloadProvider = Injekt.get(),
    internal val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    // The interval after which this cache should be invalidated. 1 hour shouldn't cause major
    // issues, as the cache is only used for UI feedback.
    private val renewInterval = 1.hours.inWholeMilliseconds

    // The last time the cache was refreshed.
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1.seconds) // Don't notify if it finishes quickly enough
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: File
        get() = File(context.cacheDir, "dl_index_cache_v3")

    internal val rootDownloadsDirMutex = Mutex()
    internal var rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

    init {
        // Attempt to read cache file
        scope.launch {
            rootDownloadsDirMutex.withLock {
                try {
                    if (diskCacheFile.exists()) {
                        val diskCache = diskCacheFile.inputStream().use {
                            ProtoBuf.decodeFromByteArray<RootDirectory>(it.readBytes())
                        }
                        rootDownloadsDir = diskCache
                        lastRenew = System.currentTimeMillis()
                    }
                } catch (expected: Throwable) {
                    // Logged whatever the cause; the caller carries on.
                    logcat(LogPriority.ERROR, expected) { "Failed to initialize from disk cache" }
                    diskCacheFile.delete()
                }
            }
        }

        storageManager.changes
            .onEach { invalidateCache() }
            .launchIn(scope)
    }

    private var updateDiskCacheJob: Job? = null

    // SY <--

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        diskCacheFile.delete()
        renewCache()
    }

    // Renews the downloads cache.
    internal fun renewCache() {
        // Avoid renewing cache if in the process nor too often
        if (lastRenew + renewInterval >= System.currentTimeMillis() || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launchIO {
            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }

            // Try to wait until extensions and sources have loaded
            // SY -->
            var sources = emptyList<Source>()
            withTimeoutOrNull(30.seconds) {
                extensionManager.isInitialized.first { it }
                sourceManager.isInitialized.first { it }

                sources = getSources()
            }
            // SY <--

            val sourceMap = sources.associate { provider.getSourceDirName(it).lowercase() to it.id }

            rootDownloadsDirMutex.withLock {
                val updatedRootDir = RootDirectory(storageManager.getDownloadsDirectory())

                updatedRootDir.sourceDirs = updatedRootDir.dir.namedSubDirectories()
                    .mapNotNull { dir ->
                        val sourceId = sourceMap[dir.name!!.lowercase()]
                        sourceId?.let { it to SourceDirectory(dir) }
                    }
                    .toMap()

                updatedRootDir.sourceDirs.values
                    .map { sourceDir -> async { sourceDir.scan() } }
                    .awaitAll()

                rootDownloadsDir = updatedRootDir
            }

            _isInitializing.emit(false)
        }.apply {
            invokeOnCompletion(onCancelling = true) { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "DownloadCache: failed to create cache" }
                }
                lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    // SY --> stub sources included so their downloads are still counted
    private fun getSources(): List<Source> = sourceManager.getVisibleOnlineSources() + sourceManager.getStubSources()
    // SY <--

    internal fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
        updateDiskCache()
    }

    private fun updateDiskCache() {
        updateDiskCacheJob?.cancel()
        updateDiskCacheJob = scope.launchIO {
            delay(1.seconds)
            ensureActive()
            val bytes = ProtoBuf.encodeToByteArray(rootDownloadsDir)
            ensureActive()
            try {
                diskCacheFile.writeBytes(bytes)
            } catch (expected: Throwable) {
                // Logged whatever the cause; the caller carries on.
                logcat(
                    priority = LogPriority.ERROR,
                    throwable = expected,
                    message = { "Failed to write disk cache file" },
                )
            }
        }
    }
}
