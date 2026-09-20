package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 18f

private const val THREE_HOURS = 3

internal class RemoveShorterLibraryUpdatesMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        withIOContext { migrate(libraryPreferences) }
        return true
    }

    private fun migrate(libraryPreferences: LibraryPreferences) {
        val updateInterval = libraryPreferences.autoUpdateInterval.get()
        if (updateInterval == 1 || updateInterval == 2) {
            libraryPreferences.autoUpdateInterval.set(THREE_HOURS)
        }
    }
}
