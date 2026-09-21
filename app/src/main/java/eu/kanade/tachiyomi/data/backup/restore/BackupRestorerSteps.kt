@file:OptIn(ExperimentalAtomicApi::class)

package eu.kanade.tachiyomi.data.backup.restore

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

internal suspend fun BackupRestorer.restoreCategories(backupCategories: List<BackupCategory>) {
    currentCoroutineContext().ensureActive()
    categoriesRestorer(backupCategories)

    val progress = restoreProgress.incrementAndFetch()
    notifier.showRestoreProgress(
        context.stringResource(MR.strings.categories),
        progress,
        restoreAmount,
        isSync,
    )
}

// SY -->
internal suspend fun BackupRestorer.restoreSavedSearches(backupSavedSearches: List<BackupSavedSearch>) {
    currentCoroutineContext().ensureActive()
    savedSearchRestorer.restoreSavedSearches(backupSavedSearches)

    val progress = restoreProgress.incrementAndFetch()
    notifier.showRestoreProgress(
        context.stringResource(SYMR.strings.saved_searches),
        progress,
        restoreAmount,
        isSync,
    )
}

internal suspend fun BackupRestorer.restoreManga(
    backupMangas: List<BackupManga>,
    backupCategories: List<BackupCategory>,
) {
    mangaRestorer.sortByNew(backupMangas)
        /* SY --> */.sortedBy { it.source == MERGED_SOURCE_ID } /* SY <-- */
        .chunked(RESTORE_BATCH_SIZE)
        .forEach { chunk ->
            database.transaction {
                chunk.forEach {
                    currentCoroutineContext().ensureActive()

                    try {
                        mangaRestorer.restore(it, backupCategories)
                    } catch (expected: Exception) {
                        // Any failure ends here and the fallback below applies.
                        val sourceName = sourceMapping[it.source] ?: it.source.toString()
                        errors.add(Date() to "${it.title} [$sourceName]: ${expected.message}")
                    }

                    restoreProgress.incrementAndFetch()
                }
            }
            notifier.showRestoreProgress(chunk.last().title, restoreProgress.load(), restoreAmount, isSync)
        }
}

internal suspend fun BackupRestorer.restoreAppPreferences(
    preferences: List<BackupPreference>,
    categories: List<BackupCategory>?,
) {
    currentCoroutineContext().ensureActive()
    preferenceRestorer.restoreApp(
        preferences,
        categories,
    )

    val progress = restoreProgress.incrementAndFetch()
    notifier.showRestoreProgress(
        context.stringResource(MR.strings.app_settings),
        progress,
        restoreAmount,
        isSync,
    )
}

internal suspend fun BackupRestorer.restoreSourcePreferences(preferences: List<BackupSourcePreferences>) {
    currentCoroutineContext().ensureActive()
    preferenceRestorer.restoreSource(preferences)

    val progress = restoreProgress.incrementAndFetch()
    notifier.showRestoreProgress(
        context.stringResource(MR.strings.source_settings),
        progress,
        restoreAmount,
        isSync,
    )
}

internal suspend fun BackupRestorer.restoreExtensionStores(
    backupExtensionStores: List<BackupExtensionStore>,
) {
    backupExtensionStores
        .chunked(RESTORE_BATCH_SIZE)
        .forEach { chunk ->
            database.transaction {
                chunk.forEach {
                    currentCoroutineContext().ensureActive()

                    try {
                        extensionStoreRestorer(it)
                    } catch (expected: Exception) {
                        // Any failure ends here and the fallback below applies.
                        errors.add(Date() to "Error Adding Repo: ${it.name} : ${expected.message}")
                    }

                    restoreProgress.incrementAndFetch()
                }
            }
            notifier.showRestoreProgress(
                context.stringResource(MR.strings.extensionStores),
                restoreProgress.load(),
                restoreAmount,
                isSync,
            )
        }
}
