package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_BACKUP_MANGA = 1
private const val BACKUP_BACKUP_CATEGORIES = 2
private const val BACKUP_BACKUP_BROKEN_SOURCES = 100
private const val BACKUP_BACKUP_SOURCES = 101
private const val BACKUP_BACKUP_PREFERENCES = 104
private const val BACKUP_BACKUP_SOURCE_PREFERENCES = 105
private const val BACKUP_BACKUP_EXTENSION_STORES = 106
private const val BACKUP_BACKUP_SAVED_SEARCHES = 600

@Serializable
internal data class Backup(
    @ProtoNumber(BACKUP_BACKUP_MANGA) val backupManga: List<BackupManga>,
    @ProtoNumber(BACKUP_BACKUP_CATEGORIES) var backupCategories: List<BackupCategory> = emptyList(),
    // @ProtoNumber(BACKUP_BACKUP_BROKEN_SOURCES) var backupBrokenSources, legacy source model with non-compliant proto number,
    @ProtoNumber(BACKUP_BACKUP_SOURCES) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(BACKUP_BACKUP_PREFERENCES) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(BACKUP_BACKUP_SOURCE_PREFERENCES) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(BACKUP_BACKUP_EXTENSION_STORES) var backupExtensionStores: List<BackupExtensionStore> = emptyList(),
    // SY specific values
    @ProtoNumber(BACKUP_BACKUP_SAVED_SEARCHES) var backupSavedSearches: List<BackupSavedSearch> = emptyList(),
)
