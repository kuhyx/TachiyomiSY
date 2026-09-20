package eu.kanade.tachiyomi.data.backup.create

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal suspend fun BackupCreator.backupCategories(options: BackupOptions): List<BackupCategory> {
    if (!options.categories) return emptyList()

    return categoriesBackupCreator()
}

internal suspend fun BackupCreator.backupMangas(mangas: List<Manga>, options: BackupOptions): List<BackupManga> {
    if (!options.libraryEntries) return emptyList()

    return mangaBackupCreator(mangas, options)
}

internal fun BackupCreator.backupSources(mangas: List<BackupManga>): List<BackupSource> = sourcesBackupCreator(mangas)

internal fun BackupCreator.backupAppPreferences(options: BackupOptions): List<BackupPreference> {
    if (!options.appSettings) return emptyList()

    return preferenceBackupCreator.createApp(includePrivatePreferences = options.privateSettings)
}

internal fun BackupCreator.backupSourcePreferences(options: BackupOptions): List<BackupSourcePreferences> {
    if (!options.sourceSettings) return emptyList()

    return preferenceBackupCreator.createSource(includePrivatePreferences = options.privateSettings)
}

internal suspend fun BackupCreator.backupExtensionStores(options: BackupOptions): List<BackupExtensionStore> {
    if (!options.extensionStores) return emptyList()

    return extensionStoresBackupCreator()
}

// SY -->
internal suspend fun BackupCreator.backupSavedSearches(options: BackupOptions): List<BackupSavedSearch> {
    if (!options.savedSearches) return emptyList()

    return savedSearchBackupCreator()
}
