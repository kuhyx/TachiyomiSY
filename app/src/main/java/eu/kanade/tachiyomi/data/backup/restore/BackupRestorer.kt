package eu.kanade.tachiyomi.data.backup.restore

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionStoreRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.MangaRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.SavedSearchRestorer
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal const val RESTORE_BATCH_SIZE = 100

@OptIn(ExperimentalAtomicApi::class)
internal class BackupRestorer(
    internal val context: Context,
    internal val notifier: BackupNotifier,
    internal val isSync: Boolean,

    internal val database: Database = Injekt.get(),
    internal val categoriesRestorer: CategoriesRestorer = CategoriesRestorer(),
    internal val preferenceRestorer: PreferenceRestorer = PreferenceRestorer(context),
    internal val extensionStoreRestorer: ExtensionStoreRestorer = ExtensionStoreRestorer(),
    internal val mangaRestorer: MangaRestorer = MangaRestorer(isSync),
    // SY -->
    internal val savedSearchRestorer: SavedSearchRestorer = SavedSearchRestorer(),
    // SY <--
) {

    internal var restoreAmount = 0
    internal val restoreProgress = AtomicInt(0)
    internal val errors = CopyOnWriteArrayList<Pair<Date, String>>()

    // Mapping of source ID to source name from backup data.
    internal var sourceMapping: Map<Long, String> = emptyMap()

    suspend fun restore(uri: Uri, options: RestoreOptions) {
        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        // Invalidate download cache to ensure UI reflects any restored downloads
        if (options.libraryEntries) {
            try {
                Injekt.get<DownloadCache>().invalidateCache()
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected) { "Failed to invalidate download cache after restore" }
            }
        }

        val time = System.currentTimeMillis() - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(
            time,
            errors.size,
            logFile.parent,
            logFile.name,
            isSync,
        )
    }

    private suspend fun restoreFromFile(uri: Uri, options: RestoreOptions) {
        val backup = BackupDecoder(context).decode(uri)

        // Store source mapping for error messages
        val backupMaps = backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }

        restoreAmount += options.stepCount(backup)

        // Each restorer opens its own write transaction on the same database. Upstream
        // `launch`es all six into one coroutineScope, so they contend for SQLite's
        // single writer; that surfaces as "Error code: 5, database is locked" from
        // Room's InvalidationTracker and aborts most of the library restore
        // (TachiyomiSY #1634 / #1638). Awaiting them in order costs a little wall time
        // and removes the contention entirely. It also removes a second race: with
        // appSettings and categories both enabled, restoreAppPreferences writes the
        // category tables at the same time restoreCategories and restoreManga do.
        val restoredCategories = backup.backupCategories.takeIf { options.categories }
        coroutineScope {
            if (options.categories) {
                restoreCategories(backup.backupCategories)
            }
            // SY -->
            if (options.savedSearches) {
                restoreSavedSearches(backup.backupSavedSearches)
            }
            // SY <--
            if (options.appSettings) {
                restoreAppPreferences(backup.backupPreferences, restoredCategories)
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(backup.backupSourcePreferences)
            }
            if (options.libraryEntries) {
                restoreManga(backup.backupManga, restoredCategories.orEmpty())
            }
            if (options.extensionStores) {
                restoreExtensionStores(backup.backupExtensionStores)
            }

            // Follow-up: optionally trigger online library + tracker update
            // https://github.com/kuhyx/TachiyomiSY/issues/15
        }
    }

    // SY <--

    // An unwritable cache yields an empty path, like no errors at all.
    private fun writeErrorLog(): File {
        if (errors.isEmpty()) return File("")
        return try {
            val file = context.createFileInCacheDir("mihon_restore_error.txt")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            file.writeText(errors.joinToString("") { (date, message) -> "[${sdf.format(date)}] $message\n" })
            file
        } catch (_: Exception) {
            File("")
        }
    }
}

// One progress step per manga and per extension store, one per enabled settings group.
internal fun RestoreOptions.stepCount(backup: Backup): Int = listOf(
    backup.backupManga.size.takeIf { libraryEntries },
    1.takeIf { categories },
    // SY -->
    1.takeIf { savedSearches },
    // SY <--
    1.takeIf { appSettings },
    backup.backupExtensionStores.size.takeIf { extensionStores },
    1.takeIf { sourceSettings },
).sumOf { it ?: 0 }
