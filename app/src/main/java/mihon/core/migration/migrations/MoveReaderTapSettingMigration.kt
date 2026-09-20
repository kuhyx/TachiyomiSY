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

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return@withIOContext false
        val oldReaderTap = prefs.getBoolean("reader_tap", false)
        if (!oldReaderTap) {
            readerPreferences.navigationModePager.set(NAVIGATION_DISABLED)
            readerPreferences.navigationModeWebtoon.set(NAVIGATION_DISABLED)
        }

        return@withIOContext true
    }
}
