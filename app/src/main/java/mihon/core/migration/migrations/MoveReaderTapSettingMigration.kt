package mihon.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 32f

// The navigation-mode preference value that turns tap zones off.
private const val NAVIGATION_DISABLED = 5

internal class MoveReaderTapSettingMigration : Migration {
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
        val oldReaderTap = prefs.getBoolean("reader_tap", false)
        if (!oldReaderTap) {
            readerPreferences.navigationModePager.set(NAVIGATION_DISABLED)
            readerPreferences.navigationModeWebtoon.set(NAVIGATION_DISABLED)
        }
    }
}
