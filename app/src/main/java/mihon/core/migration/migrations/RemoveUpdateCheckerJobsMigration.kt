package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.workManager
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 52f

internal class RemoveUpdateCheckerJobsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val preferenceStore = migrationContext.get<PreferenceStore>()
        val trackerManager = migrationContext.get<TrackerManager>()
        if (context == null || preferenceStore == null || trackerManager == null) return false
        withIOContext { migrate(context, preferenceStore, trackerManager) }
        return true
    }

    private fun migrate(context: Application, preferenceStore: PreferenceStore, trackerManager: TrackerManager) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Removed background jobs
        context.workManager.cancelAllWorkByTag("UpdateChecker")
        context.workManager.cancelAllWorkByTag("ExtensionUpdate")
        prefs.edit {
            remove("automatic_ext_updates")
        }
        val prefKeys = listOf(
            "pref_filter_library_downloaded",
            "pref_filter_library_unread",
            "pref_filter_library_started",
            "pref_filter_library_bookmarked",
            "pref_filter_library_completed",
            "pref_filter_library_lewd",
        ) + trackerManager.trackers.map { "pref_filter_library_tracked_${it.id}" }

        prefKeys.forEach { key ->
            val pref = prefs.getInt(key, 0)
            prefs.edit {
                remove(key)

                val newValue = when (pref) {
                    1 -> TriState.ENABLED_IS
                    2 -> TriState.ENABLED_NOT
                    else -> TriState.DISABLED
                }

                preferenceStore.getEnum("${key}_v2", TriState.DISABLED).set(newValue)
            }
        }
    }
}
