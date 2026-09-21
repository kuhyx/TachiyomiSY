package mihon.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.archiveReaderMode
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 66f

internal class MoveCacheToDiskSettingMigration : Migration {
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
        val cacheImagesToDisk = prefs.getBoolean("cache_archive_manga_on_disk", false)
        if (cacheImagesToDisk) {
            readerPreferences.archiveReaderMode.set(ReaderPreferences.ArchiveReaderMode.CACHE_TO_DISK)
        }
    }
}
