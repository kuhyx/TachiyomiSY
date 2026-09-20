package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 39f

internal class MoveSortingModeSettingMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val libraryPreferences = migrationContext.get<LibraryPreferences>()
        if (context == null || libraryPreferences == null) return false
        withIOContext { migrate(context, libraryPreferences) }
        return true
    }

    private fun migrate(context: Application, libraryPreferences: LibraryPreferences) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sort = prefs.getString(libraryPreferences.sortingMode.key(), null) ?: return
        val direction = prefs.getString("library_sorting_ascending", "ASCENDING")!!
        prefs.edit {
            putString(libraryPreferences.sortingMode.key(), "$sort,$direction")
            remove("library_sorting_ascending")
        }
    }
}
