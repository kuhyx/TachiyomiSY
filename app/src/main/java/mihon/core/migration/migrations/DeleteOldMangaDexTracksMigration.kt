package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.data.Database

private const val VERSION = 17f

// The id the pre-MDList MangaDex tracker used.
private const val OLD_MANGADEX_TRACKER_ID = 6L

internal class DeleteOldMangaDexTracksMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val database = migrationContext.get<Database>() ?: return@withIOContext false
        // Delete old mangadex trackers
        database.ehQueries.deleteBySyncId(OLD_MANGADEX_TRACKER_ID)
        return@withIOContext true
    }
}
