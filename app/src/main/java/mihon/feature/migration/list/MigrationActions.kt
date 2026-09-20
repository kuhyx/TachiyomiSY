package mihon.feature.migration.list

import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import logcat.LogPriority
import mihon.feature.migration.list.MigrationListScreenModel.Dialog
import mihon.feature.migration.list.models.MigratingManga
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.api.get

internal fun MigrationListScreenModel.useMangaForMigration(current: Long, target: Long, onMissingChapters: () -> Unit) {
    val migratingManga = items.find { it.manga.id == current } ?: return
    migratingManga.searchResult.value = SearchResult.Searching
    screenModelScope.launchIO {
        val result = migratingManga.migrationScope.async {
            getManga.await(target)?.let { manga ->
                try {
                    val source = sourceManager.get(manga.source)!!
                    updateMangaFromRemote(
                        source = source,
                        manga = manga,
                        fetchChapters = true,
                        // SY -->
                        throttleFunc = throttleManager::throttle,
                        // SY <--
                    ).getOrThrow().manga
                } catch (_: Exception) {
                    null
                }
            }
        }
            .await()

        if (result == null) {
            migratingManga.searchResult.value = SearchResult.NotFound
            withUIContext { onMissingChapters() }
            return@launchIO
        }

        migratingManga.searchResult.value = result.toSuccessSearchResult()
        updateMigrationProgress()
    }
}

internal fun MigrationListScreenModel.migrateMangas() {
    migrateMangas(replace = true)
}

internal fun MigrationListScreenModel.copyMangas() {
    migrateMangas(replace = false)
}

internal fun MigrationListScreenModel.migrateMangas(replace: Boolean) {
    migrateJob = screenModelScope.launchIO {
        updateState { it.copy(dialog = Dialog.Progress(0f)) }
        val items = items
        try {
            items.forEachIndexed { index, manga ->
                try {
                    ensureActive()
                    val target = manga.searchResult.value.let {
                        if (it is SearchResult.Success) {
                            it.manga
                        } else {
                            null
                        }
                    }
                    if (target != null) {
                        migrateManga(
                            current = manga.manga,
                            target = target,
                            replace = replace,
                            // SY -->
                            throttleFunc = throttleManager::throttle,
                            // SY <--
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (expected: Exception) {
                    // Logged whatever the cause; the caller carries on.
                    logcat(LogPriority.WARN, throwable = expected)
                }
                updateState {
                    it.copy(dialog = Dialog.Progress((index.toFloat() / items.size).coerceAtMost(1f)))
                }
            }

            navigateBack()
        } finally {
            updateState { it.copy(dialog = null) }
            migrateJob = null
        }
    }
}

internal fun MigrationListScreenModel.cancelMigrate() {
    migrateJob?.cancel()
    migrateJob = null
}

internal suspend fun MigrationListScreenModel.navigateBack() {
    navigateBackChannel.send(Unit)
}

internal fun MigrationListScreenModel.migrateNow(mangaId: Long, replace: Boolean) {
    screenModelScope.launchIO {
        val manga = items.find { it.manga.id == mangaId }
        if (manga != null) {
            val target = (manga.searchResult.value as? SearchResult.Success)?.manga
            if (target != null) {
                migrateManga(current = manga.manga, target = target, replace = replace)

                removeManga(mangaId)
            }
        }
    }
}

internal fun MigrationListScreenModel.removeManga(mangaId: Long) {
    screenModelScope.launchIO {
        val item = items.find { it.manga.id == mangaId }
        if (item != null) {
            removeManga(item)
            item.cancelMigration()
            updateMigrationProgress()
        }
    }
}

internal fun MigrationListScreenModel.removeManga(item: MigratingManga) {
    updateState { it.copy(items = items.toMutableList().apply { remove(item) }) }
}
