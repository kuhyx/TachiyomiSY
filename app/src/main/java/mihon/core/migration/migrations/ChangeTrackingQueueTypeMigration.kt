package mihon.core.migration.migrations

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 44f

internal class ChangeTrackingQueueTypeMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        withIOContext { migrate(context) }
        return true
    }

    private fun migrate(context: Application) {
        val trackingQueuePref = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)
        trackingQueuePref.all.forEach {
            val (_, lastChapterRead) = it.value.toString().split(":")
            trackingQueuePref.edit {
                remove(it.key)
                putFloat(it.key, lastChapterRead.toFloat())
            }
        }
    }
}
