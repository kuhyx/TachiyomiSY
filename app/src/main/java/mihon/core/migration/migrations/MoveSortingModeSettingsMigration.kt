package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.category.CategoryMapper
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 38f

// Bits 2-5 of a category's flags held its sort mode; 0b1000 in that field meant "date added".
private const val SORT_MODE_MASK = 0b00111100L
private const val SORT_BY_DATE_ADDED_FLAG = 0b00100000L

internal class MoveSortingModeSettingsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val libraryPreferences = migrationContext.get<LibraryPreferences>()
        val database = migrationContext.get<Database>()
        if (context == null || libraryPreferences == null || database == null) return false
        withIOContext { migrate(context, libraryPreferences, database) }
        return true
    }

    private suspend fun migrate(context: Application, libraryPreferences: LibraryPreferences, database: Database) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Handle renamed enum values
        val newSortingMode = when (
            val oldSortingMode = prefs.getString(libraryPreferences.sortingMode.key(), "ALPHABETICAL")
        ) {
            "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
            "UNREAD" -> "UNREAD_COUNT"
            "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
            "DRAG_AND_DROP" -> "ALPHABETICAL"
            else -> oldSortingMode
        }
        prefs.edit {
            putString(libraryPreferences.sortingMode.key(), newSortingMode)
        }
        database.transaction {
            database.categoriesQueries.getCategories().awaitList(CategoryMapper::mapCategory)
                .filter { it.flags and SORT_MODE_MASK == SORT_BY_DATE_ADDED_FLAG }
                .forEach {
                    database.categoriesQueries.update(
                        categoryId = it.id,
                        flags = it.flags and 0b00111100L.inv(),
                        name = null,
                        version = it.version,
                        uid = it.uid,
                        last_modified_at = null,
                        isSyncing = null,
                        order = null,
                    )
                }
        }
    }
}
