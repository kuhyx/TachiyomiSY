package mihon.core.migration.migrations

import android.app.Application
import exh.log.xLogE
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File

private const val VERSION = 24f

internal class DeleteOldEhFavoritesDatabaseMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        withIOContext { migrate(context) }
        return true
    }

    private fun migrate(context: Application) {
        try {
            sequenceOf(
                "fav-sync",
                "fav-sync.management",
                "fav-sync.lock",
                "fav-sync.note",
            ).map {
                File(context.filesDir, it)
            }.filter(File::exists).forEach {
                if (it.isDirectory) {
                    it.deleteRecursively()
                } else {
                    it.delete()
                }
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            xLogE("Failed to delete old favorites database", expected)
        }
    }
}
