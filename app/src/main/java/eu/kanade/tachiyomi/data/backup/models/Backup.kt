package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_BACKUP_MANGA = 1
private const val BACKUP_BACKUP_CATEGORIES = 2
private const val BACKUP_BACKUP_SOURCES = 101
private const val BACKUP_BACKUP_PREFERENCES = 104
private const val BACKUP_BACKUP_SOURCE_PREFERENCES = 105
private const val BACKUP_BACKUP_EXTENSION_STORES = 106
private const val BACKUP_BACKUP_SAVED_SEARCHES = 600

@Serializable
internal data class Backup(
    @ProtoNumber(BACKUP_BACKUP_MANGA) val backupManga: List<BackupManga>,
    @ProtoNumber(BACKUP_BACKUP_CATEGORIES) val backupCategories: List<BackupCategory> = emptyList(),
    // @ProtoNumber(100) var backupBrokenSources, legacy source model with non-compliant proto
    // number,
    @ProtoNumber(BACKUP_BACKUP_SOURCES) val backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(BACKUP_BACKUP_PREFERENCES) val backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(BACKUP_BACKUP_SOURCE_PREFERENCES) val backupSourcePreferences: List<BackupSourcePreferences> =
        emptyList(),
    @ProtoNumber(BACKUP_BACKUP_EXTENSION_STORES) val backupExtensionStores: List<BackupExtensionStore> = emptyList(),
    // SY specific values
    @ProtoNumber(BACKUP_BACKUP_SAVED_SEARCHES) val backupSavedSearches: List<BackupSavedSearch> = emptyList(),
)
