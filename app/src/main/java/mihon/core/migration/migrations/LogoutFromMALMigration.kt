package mihon.core.migration.migrations

import eu.kanade.tachiyomi.data.track.TrackerManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 12f

internal class LogoutFromMALMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        // Force MAL log out due to login flow change
        migrationContext.get<TrackerManager>()?.myAnimeList?.logout()

        true
    }
}
