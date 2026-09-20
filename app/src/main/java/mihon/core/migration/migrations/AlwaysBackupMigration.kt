package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.backup.service.BackupPreferences

private const val VERSION = 40f

private const val TWELVE_HOURS = 12

internal class AlwaysBackupMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val backupPreferences = migrationContext.get<BackupPreferences>() ?: return false
        withIOContext { migrate(backupPreferences) }
        return true
    }

    private fun migrate(backupPreferences: BackupPreferences) {
        if (backupPreferences.backupInterval.get() == 0) {
            backupPreferences.backupInterval.set(TWELVE_HOURS)
        }
    }
}
