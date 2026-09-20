package mihon.core.migration.migrations

import android.content.SharedPreferences
import androidx.core.content.edit
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 75f

internal class RemoveDuplicateReaderPreferenceMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val prefs = migrationContext.get<SharedPreferences>() ?: return false
        withIOContext { migrate(prefs) }
        return true
    }

    private fun migrate(prefs: SharedPreferences) {
        if (prefs.getBoolean("mark_read_dupe", false)) {
            val readPrefSet = prefs.getStringSet("mark_duplicate_read_chapter_read", emptySet())?.toMutableSet()
            readPrefSet?.add("existing")
            prefs.edit {
                putStringSet("mark_duplicate_read_chapter_read", readPrefSet)
                remove("mark_read_dupe")
            }
        }
    }
}
