package mihon.core.migration.migrations

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 18f

// Theme 4 was folded into the automatic theme.
private const val REMOVED_THEME = 4
private const val AUTOMATIC_THEME = 3

internal class RemoveOldReaderThemeMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return@withIOContext false
        val readerTheme = readerPreferences.readerTheme.get()
        if (readerTheme == REMOVED_THEME) {
            readerPreferences.readerTheme.set(AUTOMATIC_THEME)
        }

        return@withIOContext true
    }
}
