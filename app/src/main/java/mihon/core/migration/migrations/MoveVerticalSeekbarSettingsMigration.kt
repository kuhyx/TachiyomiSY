package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 77f

internal class MoveVerticalSeekbarSettingsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val readerPreferences = migrationContext.get<ReaderPreferences>()
        if (context == null || readerPreferences == null) return false
        withIOContext { migrate(context, readerPreferences) }
        return true
    }

    private fun migrate(context: Application, readerPreferences: ReaderPreferences) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            val forceHorzSeekbar = prefs.getBoolean("pref_force_horz_seekbar", false)
            if (forceHorzSeekbar) {
                putBoolean(
                    "pref_webtoon_vertical_navigator",
                    false,
                )
            }
            remove("pref_force_horz_seekbar")
            val leftVerticalSeekbar = prefs.getBoolean("pref_left_handed_vertical_seekbar", false)
            if (leftVerticalSeekbar) {
                putBoolean(
                    readerPreferences.verticalNavigatorOnLeft.key(),
                    true,
                )
            }
            remove("pref_left_handed_vertical_seekbar")
        }
    }
}
