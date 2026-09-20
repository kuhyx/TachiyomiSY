package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 41f

internal class ResetFilterAndSortSettingsMigration : Migration {
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
        val preferences = listOf(
            libraryPreferences.filterChapterByRead,
            libraryPreferences.filterChapterByDownloaded,
            libraryPreferences.filterChapterByBookmarked,
            libraryPreferences.sortChapterBySourceOrNumber,
            libraryPreferences.displayChapterByNameOrNumber,
            libraryPreferences.sortChapterByAscendingOrDescending,
        )

        prefs.edit {
            preferences.forEach { preference ->
                val key = preference.key()
                val value = prefs.getInt(key, Int.MIN_VALUE)
                if (value != Int.MIN_VALUE) {
                    remove(key)
                    putLong(key, value.toLong())
                }
            }
        }
    }
}
