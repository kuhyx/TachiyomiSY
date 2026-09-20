package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences

private const val VERSION = 20f

/** The integer sorting-mode preference indexed these names; 5 was unused and falls back to alphabetical. */
private val LEGACY_SORTING_MODES = listOf(
    "ALPHABETICAL",
    "LAST_READ",
    "LAST_MANGA_UPDATE",
    "UNREAD_COUNT",
    "TOTAL_CHAPTERS",
    "ALPHABETICAL",
    "LATEST_CHAPTER",
    "DRAG_AND_DROP",
    "DATE_ADDED",
    "TAG_LIST",
    "CHAPTER_FETCH_DATE",
)

internal class MoveLibrarySortingSettingsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        try {
            val oldSortingMode = prefs.getInt(libraryPreferences.sortingMode.key(), 0 /* ALPHABETICAL */)
            val oldSortingDirection = prefs.getBoolean("library_sorting_ascending", true)

            val newSortingMode = LEGACY_SORTING_MODES.getOrElse(oldSortingMode) { "ALPHABETICAL" }

            val newSortingDirection = when (oldSortingDirection) {
                true -> "ASCENDING"
                else -> "DESCENDING"
            }

            prefs.edit(commit = true) {
                remove(libraryPreferences.sortingMode.key())
                remove("library_sorting_ascending")
            }

            prefs.edit {
                putString(libraryPreferences.sortingMode.key(), newSortingMode)
                putString("library_sorting_ascending", newSortingDirection)
            }
        } catch (e: Exception) {
            logcat(throwable = e) { "Already done migration" }
        }

        return@withIOContext true
    }
}
