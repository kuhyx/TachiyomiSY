package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 22f

// Update intervals under twelve hours were dropped; anyone on them moves to twelve.
private const val THREE_HOURS = 3
private const val FOUR_HOURS = 4
private const val SIX_HOURS = 6
private const val EIGHT_HOURS = 8
private val REMOVED_INTERVALS_HOURS = listOf(THREE_HOURS, FOUR_HOURS, SIX_HOURS, EIGHT_HOURS)
private const val REPLACEMENT_INTERVAL_HOURS = 12

internal class RemoveShortLibraryUpdatesMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val updateInterval = libraryPreferences.autoUpdateInterval.get()
        if (updateInterval in REMOVED_INTERVALS_HOURS) {
            libraryPreferences.autoUpdateInterval.set(REPLACEMENT_INTERVAL_HOURS)
        }

        return@withIOContext true
    }
}
