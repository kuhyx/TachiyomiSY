package mihon.core.migration.migrations

import android.app.Application
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

internal class SetupEHentaiUpdateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        EHentaiUpdateWorker.scheduleBackground(context)
        return true
    }
}
