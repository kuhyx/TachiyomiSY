package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences

private const val LIBRARY_SORTING_ASCENDING = "library_sorting_ascending"
private const val ALPHABETICAL = "ALPHABETICAL"

private const val VERSION = 20f

// The integer sorting-mode preference indexed these names; 5 was unused and falls back to alphabetical.
private val LEGACY_SORTING_MODES = listOf(
    ALPHABETICAL,
    "LAST_READ",
    "LAST_MANGA_UPDATE",
    "UNREAD_COUNT",
    "TOTAL_CHAPTERS",
    ALPHABETICAL,
    "LATEST_CHAPTER",
    "DRAG_AND_DROP",
    "DATE_ADDED",
    "TAG_LIST",
    "CHAPTER_FETCH_DATE",
)

internal class MoveLibrarySortingSettingsMigration : Migration {
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
        try {
            val oldSortingMode = prefs.getInt(libraryPreferences.sortingMode.key(), 0 /* ALPHABETICAL */)
            val oldSortingDirection = prefs.getBoolean(LIBRARY_SORTING_ASCENDING, true)

            val newSortingMode = LEGACY_SORTING_MODES.getOrElse(oldSortingMode) { ALPHABETICAL }

            val newSortingDirection = if (oldSortingDirection == true) {
                "ASCENDING"
            } else {
                "DESCENDING"
            }

            prefs.edit(commit = true) {
                remove(libraryPreferences.sortingMode.key())
                remove(LIBRARY_SORTING_ASCENDING)
            }

            prefs.edit {
                putString(libraryPreferences.sortingMode.key(), newSortingMode)
                putString(LIBRARY_SORTING_ASCENDING, newSortingDirection)
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(throwable = expected) { "Already done migration" }
        }
    }
}
